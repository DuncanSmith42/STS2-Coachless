package coachless.ui;

import coachless.data.SaveFileManager;
import coachless.data.SaveQueries;

import java.util.ArrayList;
import java.util.List;

/** Which fights end your runs. Filter by character with the dropdown. */
public class MonsterPanel extends TablePanel {
    public MonsterPanel(SaveFileManager saves) {
        super(saves,
            "Monster deaths",
            "How many runs ended in each fight. Death rate = deaths / times you fought it.",
            true,
            1,
            new String[]{"Encounter", "Deaths", "Fights", "Death rate %"},
            new Class<?>[]{String.class, Long.class, Long.class, Double.class},
            (all, character) -> {
                List<Object[]> out = new ArrayList<>();
                for (SaveQueries.EncounterRow r : SaveQueries.encounters(all, character)) {
                    out.add(new Object[]{r.name(), r.deaths(), r.fights(), r.DeathRate()});
                }
                return out;
            });
    }
}