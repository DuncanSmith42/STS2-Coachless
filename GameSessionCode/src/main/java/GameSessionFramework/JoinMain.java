package GameSessionFramework;

import GameSessionFramework.Network.JoinClient;

import java.io.IOException;

// required args: <hostIp> <port> <code> <yourName>
public class JoinMain {
    public static void main(String[] args) throws IOException {
        if (args.length < 4) {
            System.out.println("Usage: JoinMain <hostIp> <port> <code> <yourName>");
            return;
        }
        try (JoinClient client = JoinClient.connect(args[0], Integer.parseInt(args[1]), args[2], args[3],
                msg -> System.out.println("received: " + msg))) {
            System.out.println("Joined! Press Enter to leave.");
            System.in.read();
        } catch (IOException e) {
            System.out.println("Could not join: " + e.getMessage());
        }
    }
}
