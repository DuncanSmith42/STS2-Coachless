package coachless.multiplayer;

import coachless.GameSessionFramework.Network.HostServer;
import coachless.GameSessionFramework.Network.JoinClient;
import coachless.GameSessionFramework.Player;
import coachless.GameSessionFramework.session.GameCodeGenerator;
import coachless.GameSessionFramework.session.GameSession;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MultiplayerController {

    public interface Listener {
        void onStatusChanged(String status);

        // Names 
        void onPlayersChanged(List<String> playerNames);

        // what you need to join
        void onHosting(String code, String address, int port);

        // the connection to the host was lost
        void onDisconnected(String reason);

        // Anything else
        void onMessageReceived(String message);
    }

    private final Listener listener;
    private volatile HostServer hostServer;
    private volatile JoinClient joinClient;

    public MultiplayerController(Listener listener) {
        this.listener = listener;
    }

    // Starts hosting
    public void hostGame(String playerName, int port) throws IOException {
        if (isInSession()) {
            throw new IllegalStateException("Already in a multiplayer session.");
        }

        String code = new GameCodeGenerator().generateCode();
        Player host = new Player(UUID.randomUUID().toString(), clean(playerName));
        GameSession session = new GameSession(code, host);

        HostServer server = new HostServer(session, port, new HostServer.Listener() {
            @Override
            public void onPlayersChanged(List<Player> players) {
                notifyPlayers(names(players));
            }

            @Override
            public void onEvent(String message) {
                notifyStatus(message);
            }
        });
        server.start(); // throws if the port is taken

        hostServer = server;
        notifyPlayers(names(session.getPlayers()));
        notifyHosting(code, findLocalAddress(), server.getPort());
        notifyStatus("Hosting - waiting for players");
    }


    public void joinGame(String hostAddress, int port, String code, String playerName) throws IOException {
        if (isInSession()) {
            throw new IllegalStateException("Already in a multiplayer session.");
        }
        notifyStatus("Connecting to host...");
        joinClient = JoinClient.connect(hostAddress, port, code.trim(), clean(playerName),
                this::handleClientMessage, this::handleClientDisconnected);
        notifyStatus("Connected to session");
    }

    public void leaveSession() {
        HostServer server = hostServer;
        JoinClient client = joinClient;
        hostServer = null;
        joinClient = null;
        if (server != null) {
            server.stop();
        }
        if (client != null) {
            client.close();
        }
        notifyPlayers(List.of());
        notifyStatus("Not connected");
    }

    public boolean isHosting() {
        return hostServer != null;
    }

    public boolean isConnected() {
        return joinClient != null;
    }

    public boolean isInSession() {
        return hostServer != null || joinClient != null;
    }

    // on JoinClient's listener thread
    private void handleClientMessage(String message) {
        if (message.startsWith("LOBBY ")) {
            notifyPlayers(Arrays.asList(message.substring(6).split(",")));
        } else {
            notifyMessage(message);
        }
    }

    // when the connection dropped 
    private void handleClientDisconnected(String reason) {
        joinClient = null;
        notifyPlayers(List.of());
        notifyStatus("Disconnected: " + reason);
        SwingUtilities.invokeLater(() -> listener.onDisconnected(reason));
    }

    private static List<String> names(List<Player> players) {
        return players.stream().map(Player::name).toList();
    }

    // keep commas out of names
    private static String clean(String name) {
        return name.trim().replace(",", "");
    }

    private static String findLocalAddress() {
        try {
            for (NetworkInterface nic : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!nic.isUp() || nic.isLoopback() || nic.isVirtual()) {
                    continue;
                }
                for (InetAddress addr : Collections.list(nic.getInetAddresses())) {
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return "127.0.0.1";
    }

    private void notifyStatus(String status) {
        SwingUtilities.invokeLater(() -> listener.onStatusChanged(status));
    }

    private void notifyPlayers(List<String> names) {
        List<String> snapshot = List.copyOf(names);
        SwingUtilities.invokeLater(() -> listener.onPlayersChanged(snapshot));
    }

    private void notifyHosting(String code, String address, int port) {
        SwingUtilities.invokeLater(() -> listener.onHosting(code, address, port));
    }

    private void notifyMessage(String message) {
        SwingUtilities.invokeLater(() -> listener.onMessageReceived(message));
    }
}
