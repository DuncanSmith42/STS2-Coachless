package coachless.GameSessionFramework;

import coachless.GameSessionFramework.session.GameSession;
import coachless.GameSessionFramework.Network.HostServer;
import coachless.GameSessionFramework.session.GameCodeGenerator;
import java.net.InetAddress;
import java.util.UUID;

// optional args: [port] [yourName]
public class HostMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5555;
        String name = args.length > 1 ? args[1] : "Host";

        String code = new GameCodeGenerator().generateCode();
        GameSession session = new GameSession(code, new Player(UUID.randomUUID().toString(), name));
        HostServer server = new HostServer(session, port, System.out::println);
        server.start();

        System.out.println("Hosting on port " + server.getPort());
        System.out.println("Your address:  " + InetAddress.getLocalHost().getHostAddress());
        System.out.println("Session code:  " + code);
        System.out.println("Friends run JoinMain with: <address> " + server.getPort() + " " + code + " <name>");
        System.out.println("Press Enter to stop hosting.");
        System.in.read();
        server.stop();
    }
}
