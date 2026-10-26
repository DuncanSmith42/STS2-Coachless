package coachless.data;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;

public final class ProgressSave {

    public static final List<String> CharacterIDs = List.of(
        "CHARACTER.IRONCLAD",
        "CHARACTER.SILENT",
        "CHARACTER.REGENT",
        "CHARACTER.NECROBINDER",
        "CHARACTER.DEFECT"
    );

    public final long wins;
    public final long losses;
    public final long playtimeSeconds;
    public final long bestWinStreak;

    public final Map<String, Map<String, long[]>> encounters;

    public final Map<String, long[]> cards;

    private ProgressSave(long wins, long losses, long playtimeSeconds, long bestWinStreak, 
        Map<String, Map<String, long[]>> encounters, Map<String, long[]> cards) {
        this.wins = wins;
        this.losses = losses;
        this.playtimeSeconds = playtimeSeconds;
        this.bestWinStreak = bestWinStreak;
        this.encounters = encounters;
        this.cards = cards;
    }

    public long runs() { return wins + losses; }

    public static ProgressSave parse(String json) {
        Map<String, Object> root = Json.map(Json.parse(json));
        if (!root.containsKey("character_stats")) {
            throw new IllegalArgumentException("Not a progress.save file (no character_stats)");
        }

        long wins = 0, losses = 0, bestStreak = 0;
        for (Object o : Json.list(root.get("character_stats"))) {
            Map<String, Object> c = Json.map(o);
            if (!isPlayableCharacter(Json.str(c, "id", ""))) continue;
            wins += (long) Json.num(c, "total_wins", 0);
            losses += (long) Json.num(c, "total_losses", 0);
            bestStreak = Math.max(bestStreak, (long) Json.num(c, "best_win_streak", 0));
        }

        Map<String, Map<String, long[]>> encounters = new LinkedHashMap<>();
        for (Object o : Json.list(root.get("encounter_stats"))) {
            Map<String, Object> e = Json.map(o);
            String encounterId = Json.str(e, "encounter_id", "");
            for (Object fo : Json.list(e.get("fight_stats"))) {
                Map<String, Object> f = Json.map(fo);
                String character = Json.str(f, "character", "");
                if (!isPlayableCharacter(character)) continue;
                encounters.computeIfAbsent(character, k -> new LinkedHashMap<>()).put(encounterId,
                    new long[]{(long) Json.num(f, "wins", 0), (long) Json.num(f, "losses", 0)});
            }
        }

        Map<String, long[]> cards = new LinkedHashMap<>();
        for (Object o : Json.list(root.get("card_stats"))) {
            Map<String, Object> c = Json.map(o);
            cards.put(Json.str(c, "id", ""), new long[]{
                    (long) Json.num(c, "times_picked", 0),
                    (long) Json.num(c, "times_skipped", 0),
                    (long) Json.num(c, "times_won", 0),
                    (long) Json.num(c, "times_lost", 0)});
        }

        return new ProgressSave(wins, losses, (long) Json.num(root, "total_playtime", 0), bestStreak, encounters, cards);
    }

    // The five real characters
    private static boolean isPlayableCharacter(String id) {
        return id.startsWith("CHARACTER.")
                && !id.contains("-")
                && !id.equals("CHARACTER.RANDOM_CHARACTER");
    }
}
