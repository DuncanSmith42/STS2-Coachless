package coachless.ui;
 
import coachless.data.SaveFileManager;
import coachless.data.SaveQueries;
 
import java.util.ArrayList;
import java.util.List;
 
public class CardPanel extends TablePanel {
    public CardPanel(SaveFileManager saves) {
        super(saves,
            "Cards",
            "Pick rate = picked / times offered. Win rate = share of runs won with the card in your deck.",
            false,
            1,
            new String[]{"Card", "Picked", "Skipped", "Pick rate %", "Runs won", "Runs lost", "Win rate %"},
            new Class<?>[]{String.class, Long.class, Long.class, Double.class, Long.class, Long.class, Double.class},
            (all, character) -> {
                List<Object[]> out = new ArrayList<>();
                for (SaveQueries.CardRow r : SaveQueries.cards(all)) {
                    out.add(new Object[]{r.name(), r.picked(), r.skipped(), r.PickRate(),r.won(), r.lost(), r.WinRate()});
                }
                return out;
            });
    }
}