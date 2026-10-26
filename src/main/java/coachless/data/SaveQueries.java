package coachless.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SaveQueries {
    //Prevent instantiation
    private SaveQueries() {}

    public record EncounterRow(String name, long deaths, long fights) {
        public double DeathRate() {
            if (fights == 0) {return Double.NaN;}
            return 100.0 * deaths / fights;
        }
    }

    public record CardRow(String name, long picked, long skipped, long won, long lost) {
        public double PickRate() {
            long offered = picked + skipped;
            if (offered == 0) {return Double.NaN;}
            return 100.0 * picked / offered;
        }

        public double WinRate() {
            long picked = won + lost;
            if (picked == 0) {return Double.NaN;}
            return 100.0 * won / picked;
        }
    }

    public static List<EncounterRow> encounters(List<ProgressSave> saves, String characterID) {
        Map<String, long[]> total = new LinkedHashMap<>();
        for (ProgressSave save : saves) {
            
            for (Map.Entry<String, Map<String, long[]>> ch : save.encounters.entrySet()) {
                if (characterID != null && !characterID.equals(ch.getKey())) {continue;}
                
                for (Map.Entry<String, long[]> e : ch.getValue().entrySet()) {
                    long[] t = total.computeIfAbsent(e.getKey(), k -> new long[2]);
                    t[0] += e.getValue()[0];
                    t[1] += e.getValue()[1];
                }
            }
        }
        List<EncounterRow> rows = new ArrayList<>();
        for (Map.Entry<String, long[]> e : total.entrySet()) {
            long fights = e.getValue()[0] + e.getValue()[1];
            if (fights > 0) {
                rows.add(new EncounterRow(Names.PrettyPrint(e.getKey()), e.getValue()[1], fights));
            }
        }
        return rows;
    }

    public static List<CardRow> cards(List<ProgressSave> saves) {
        Map<String, long[]> total = new LinkedHashMap<>();
        for (ProgressSave save : saves) {
            for (Map.Entry<String, long[]> e : save.cards.entrySet()) {
                long[] t = total.computeIfAbsent(e.getKey(), k -> new long[4]);
                for (int i = 0; i < 4; i++) {
                    t[i] += e.getValue()[i];
                }
            }
        }

        List<CardRow> rows = new ArrayList<>();
        for (Map.Entry<String, long[]> e : total.entrySet()) {
            long [] v = e.getValue();
            if (v[0] + v[1] + v[2] + v[3] > 0) {
                rows.add(new CardRow(Names.PrettyPrint(e.getKey()), v[0], v[1], v[2], v[3]));
            }
        }
        return rows;
    }
}