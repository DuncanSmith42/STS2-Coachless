package coachless.GameSessionFramework.session;

import coachless.GameSessionFramework.Player;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// lobby logic, 4 player cap
public class GameSession {
    public static final int MAX_PLAYERS = 4;

    private final String code;
    private final Player host;
    // CopyOnWrite so other code can read the list while someone joins/leaves on another thread
    private final List<Player> players = new CopyOnWriteArrayList<>();

    public GameSession(String code, Player host) {
        this.code = code;
        this.host = host;
        this.players.add(host);
    }

    // checks the gamecode first
    public void join(String attemptedCode, Player player) {
        if (attemptedCode == null || !code.equalsIgnoreCase(attemptedCode.trim())) {
            throw new InvalidCodeException();
        }
        addPlayer(player);
    }

    // synchronized so it checks if full, then adds happens as one step
    private synchronized void addPlayer(Player player) {
        if (players.size() >= MAX_PLAYERS) {
            throw new SessionException("Session is full");
        }
        if (players.contains(player)) {
            throw new SessionException("Player already in session");
        }
        players.add(player);
    }

    // Removes a player who left or dropped. The host can't be removed this way
    // if the host leaves, the whole session ends instead.
    public synchronized boolean removePlayer(Player player) {
        if (player.equals(host)) {
            return false;
        }
        return players.remove(player);
    }

    public String getCode() { return code; }
    public Player getHost() { return host; }
    public List<Player> getPlayers() { return Collections.unmodifiableList(players); }
}
