package coachless;

import coachless.data.GlobalStats;
import coachless.data.PersonalStats;
import coachless.data.SaveFileManager;
import coachless.ui.ComparePanel;
import coachless.ui.LivePanel;
import coachless.ui.SourcesBar;
import coachless.ui.CardPanel;
import coachless.ui.MonsterPanel;
import coachless.data.ProgressFileType;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class MainFrame extends JFrame {

    public MainFrame() {
        super("STS Coachless");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 600));

        SaveFileManager saves = new SaveFileManager(new ProgressFileType());
        PersonalStats personal = new PersonalStats(saves);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Compare", new ComparePanel(personal, new GlobalStats()));
        tabs.addTab("Monsters", new MonsterPanel(saves));
        tabs.addTab("Cards", new CardPanel(saves));
        tabs.addTab("Live Run", new LivePanel());

        add(new SourcesBar(saves), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        SourcesBar.installFileDrop((JComponent) getContentPane(), saves);
        
        pack();
        setLocationRelativeTo(null);
    }
}
