package GameSessionFramework;

import GameSessionFramework.session.GameSession;
import GameSessionFramework.Network.HostServer;
import GameSessionFramework.Network.JoinClient;
import GameSessionFramework.session.GameCodeGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SessionDemo {
    public static void main(String[] args) throws Exception {
        String code = new GameCodeGenerator().generateCode();
        GameSession session = new GameSession(code, new Player("host-1", "Dunc"));
        HostServer server = new HostServer(session, 0, msg -> System.out.println("[host]  " + msg));
        server.start();

        System.out.println("Hosting on localhost:" + server.getPort() + "   session code: " + code + "\n");

        List<JoinClient> clients = new ArrayList<>();
        for (String name : new String[]{"Alvin", "Diya", "Min"}) {
            System.out.println(name + " joins with the right code");
            clients.add(JoinClient.connect("localhost", server.getPort(), code, name,
                    msg -> System.out.println("[" + name + "] received: " + msg)));
            pause();
        }

        System.out.println("\n5th player tries to join ");
        tryJoin(server.getPort(), code, "Louis");

        System.out.println("\nSomeone tries the wrong code ");
        tryJoin(server.getPort(), "ZZZZZZ", "Sam");

        pause();
        clients.forEach(JoinClient::close);
        server.stop();
        System.out.println("\nDone.");
    }

    private static void tryJoin(int port, String code, String name) {
        try {
            JoinClient.connect("localhost", port, code, name, msg -> { });

        } catch (IOException e) {
            System.out.println("[" + name + "] rejected -> " + e.getMessage());
        }
        pause();
    }

    private static void pause() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }
    }
}
