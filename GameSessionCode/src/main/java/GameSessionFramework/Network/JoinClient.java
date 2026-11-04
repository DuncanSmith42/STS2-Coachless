package GameSessionFramework.Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

// connects to a host's IP and port and sends the session code
public class JoinClient implements AutoCloseable {
    private static final int TIMEOUT_MS = 5000;

    private final Socket socket;

    private JoinClient(Socket socket) {
        this.socket = socket;
    }

    // throws IOException with a readable message if the host is unreachable or rejects the join
    public static JoinClient connect(String host, int port, String code, String name,
                                     Consumer<String> onMessage) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS); // don't hang forever waiting for the host's reply

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

            socket.setSoTimeout(0); // now wait for messages as long as it takes
            Thread listener = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        onMessage.accept(line);
                    }
                } catch (IOException ignored) {
                    // connection closed
                }
            });
            listener.setDaemon(true);
            listener.start();
            return new JoinClient(socket);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
