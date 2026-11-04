package coachless.GameSessionFramework.session;

import coachless.GameSessionFramework.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// lobby logic, 4 player cap
public class GameSession {
    public static final int MAX_PLAYERS = 4;

    private final String code;
    private final Player host;
    // CopyOnwrite so the code can read the list while someone joins on another thread
    private final List<Player> players = new CopyOnWriteArrayList<>();

    public GameSession(String code, Player host) {
        this.code = code;
        this.host = host;
        this.players.add(host);
    }

    // checks the code first
    public void join(String attemptedCode, Player player) {
        if (attemptedCode == null || !code.equalsIgnoreCase(attemptedCode.trim())) {
            throw new InvalidCodeException();
        }
        addPlayer(player);
    }

    //synched for threading of players joining
    private synchronized void addPlayer(Player player) {
        if (players.size() >= MAX_PLAYERS) {
            throw new SessionException("Session is full");
        }
        if (players.contains(player)) {
            throw new SessionException("Player already in session");
        }
        players.add(player);
    }

    public String getCode() { return code; }
    public Player getHost() { return host; }
    public List<Player> getPlayers() { return java.util.Collections.unmodifiableList(players); }
}

