package GameSessionFramework.Network;


import GameSessionFramework.session.InvalidCodeException;
import GameSessionFramework.session.SessionException;
import GameSessionFramework.Player;
import GameSessionFramework.session.GameSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


 //client -> host:  JOIN <code> <name>
 //host -> client:  OK <playerId> or ERROR <reason>
 //host -> everyone: LOBBY name1,name2,...   (whenever someone joins)

public class HostServer {
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final GameSession session;
    private final int requestedPort;
    private final Consumer<String> log;

    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private volatile boolean running;

    //Pass port 0 to let the OS pick a free port
    public HostServer(GameSession session, int port, Consumer<String> log) {
        this.session = session;
        this.requestedPort = port;
        this.log = log;
    }

    // start the server
    public void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        running = true;
        pool.submit(this::acceptLoop);
    }

    // get the port #
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
        for (Socket s : sockets) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
        pool.shutdownNow();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                sockets.add(socket);
                pool.submit(() -> handle(socket));
            } catch (IOException e) {
                if (running) {
                    log.accept("Accept error: " + e.getMessage());
                }
            }
        }
    }

    private void handle(Socket socket) {
        String ip = socket.getInetAddress().getHostAddress();
        PrintWriter out = null;
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            AtomicInteger failures = failedAttempts.computeIfAbsent(ip, k -> new AtomicInteger());
            if (failures.get() >= MAX_FAILED_ATTEMPTS) {
                out.println("ERROR Too many failed attempts");
                return;
            }

            String line = in.readLine();
            String[] parts = line == null ? new String[0] : line.split(" ", 3);
            if (parts.length < 3 || !parts[0].equals("JOIN") || parts[2].isBlank()) {
                out.println("ERROR Bad request");
                return;
            }

            Player player = new Player(UUID.randomUUID().toString(), parts[2].trim());
            try {
                session.join(parts[1], player);
            } catch (InvalidCodeException e) {
                failures.incrementAndGet();
                log.accept("Rejected join from " + ip + ": " + e.getMessage());
                out.println("ERROR " + e.getMessage());
                return;
            } catch (SessionException e) {
                log.accept("Rejected join from " + ip + ": " + e.getMessage());
                out.println("ERROR " + e.getMessage());
                return;
            }

            out.println("OK " + player.id());
            clients.add(out);
            log.accept(player.name() + " joined (" + session.getPlayers().size()
                    + "/" + GameSession.MAX_PLAYERS + ")");
            broadcastLobby();

            // Stay connected. Game-state messages from this player get handled here later.
            while (in.readLine() != null) {
                // nothing yet
            }
        } catch (IOException e) {
            // client dropped; nothing to do yet (FR-4.3 handles real disconnect logic)
        } finally {
            if (out != null) {
                clients.remove(out);
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void broadcastLobby() {
        String names = session.getPlayers().stream()
                .map(Player::name)
                .collect(Collectors.joining(","));
        for (PrintWriter w : clients) {
            w.println("LOBBY " + names);
        }
    }
}
