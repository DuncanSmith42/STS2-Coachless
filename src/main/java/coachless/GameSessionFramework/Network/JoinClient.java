package coachless.GameSessionFramework.Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

// Connects to a host's IP and port and sends the session code.
// Sends PING every 5s; if the host goes silent for 15s the connection counts as lost.
public class JoinClient implements AutoCloseable {
    private static final int TIMEOUT_MS = 5000;        // connecting + waiting for the join reply
    private static final int HEARTBEAT_MS = 5000;
    private static final int IDLE_TIMEOUT_MS = 15000;

    private final Socket socket;
    private volatile boolean closedByUs;

    private JoinClient(Socket socket) {
        this.socket = socket;
    }

    // Simpler version for callers that don't care about disconnects (HostMain / JoinMain / SessionDemo)
    public static JoinClient connect(String host, int port, String code, String name,
                                     Consumer<String> onMessage) throws IOException {
        return connect(host, port, code, name, onMessage, reason -> { });
    }

    // Throws IOException with a readable message if the host is unreachable or rejects the join.
    // onDisconnect is called (once, with a reason) only if the connection is lost
    // without us calling close() ourselves.
    public static JoinClient connect(String host, int port, String code, String name,
                                     Consumer<String> onMessage,
                                     Consumer<String> onDisconnect) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            out.println("JOIN " + code + " " + name);
            String reply = in.readLine();
            if (reply == null) {
                throw new IOException("Host closed the connection");
            }
            if (!reply.startsWith("OK")) {
                throw new IOException(reply.replaceFirst("^ERROR ", ""));
            }

            socket.setSoTimeout(IDLE_TIMEOUT_MS);
            JoinClient client = new JoinClient(socket);

            Thread listener = new Thread(() -> client.listen(in, onMessage, onDisconnect));
            listener.setDaemon(true);
            listener.start();

            Thread pinger = new Thread(() -> client.pingLoop(out));
            pinger.setDaemon(true);
            pinger.start();

            return client;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    private void listen(BufferedReader in, Consumer<String> onMessage, Consumer<String> onDisconnect) {
        String reason = "Connection to the host was lost";
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("PING")) {
                    continue;
                }
                if (line.equals("CLOSED")) {
                    reason = "The host ended the session";
                    break;
                }
                onMessage.accept(line);
            }
        } catch (SocketTimeoutException e) {
            reason = "The host stopped responding";
        } catch (IOException e) {
            // keep the default reason
        }
        if (closedByUs) {
            return; // we left on purpose, nothing to report
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        onDisconnect.accept(reason);
    }

    private void pingLoop(PrintWriter out) {
        while (!closedByUs && !socket.isClosed()) {
            try {
                Thread.sleep(HEARTBEAT_MS);
            } catch (InterruptedException e) {
                return;
            }
            out.println("PING");
        }
    }

    @Override
    public void close() {
        closedByUs = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
