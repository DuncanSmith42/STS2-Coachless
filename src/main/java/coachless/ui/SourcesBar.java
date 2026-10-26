package coachless.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.UIManager;

import coachless.data.SaveFileManager;

public class SourcesBar extends JPanel {
    private static final Color BAD = new Color(0xF87171);

    private final SaveFileManager saves;
    private final JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

    public SourcesBar(SaveFileManager saves) {
        super(new BorderLayout(12, 0));
        this.saves = saves;
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JButton addBtn = new JButton("Add folder...");
        addBtn.addActionListener(e -> chooseFolders());
        chips.setOpaque(false);

        add(addBtn, BorderLayout.WEST);
        add(chips, BorderLayout.CENTER);

        saves.addListener(this::rebuild);
        rebuild();
    }

    private void rebuild() {
        chips.removeAll();
        if (saves.folders().isEmpty()) {
            JLabel hint = new JLabel("Add the folder that holds your save files (we find the files inside). You can also drag a folder onto this window.");
            hint.setForeground(UIManager.getColor("Label.disabledForeground"));
            chips.add(hint);
        }
        for (SaveFileManager.Folder f : saves.folders()) chips.add(chip(f));
        chips.revalidate();
        chips.repaint();
    }

    private JComponent chip(SaveFileManager.Folder f) {
        int n = f.files.size();
        String problem = f.error != null ? f.error : (n == 0 ? "No game files found in this folder" : null);
        boolean fileProblem = false;
        for (SaveFileManager.Entry e : f.files) if (e.error != null) fileProblem = true;
        boolean bad = problem != null || fileProblem;
 
        JPanel p = new JPanel(new BorderLayout(4, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(UIManager.getColor("Panel.background").brighter());
                g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 4));
 
        StringBuilder tip = new StringBuilder("<html>").append(esc(f.path.toString()));
        if (problem != null) tip.append("<br>").append(esc(problem));
        for (SaveFileManager.Entry e : f.files) {
            tip.append("<br>").append(esc(f.path.relativize(e.path).toString().replace('\\', '/')
                    + (e.error == null ? "  -  OK" : "  -  " + e.error)));
        }
        p.setToolTipText(tip.toString());
 
        JLabel name = new JLabel((bad ? "(!) " : "") + shortName(f.path)
                + "  -  " + n + (n == 1 ? " file" : " files"));
        if (bad) name.setForeground(BAD);
 
        JButton x = new JButton("\u00D7");
        x.setToolTipText("Remove");
        x.setBorderPainted(false);
        x.setContentAreaFilled(false);
        x.setFocusable(false);
        x.setMargin(new Insets(0, 4, 0, 4));
        x.addActionListener(ev -> saves.remove(f));
 
        p.add(name, BorderLayout.CENTER);
        p.add(x, BorderLayout.EAST);
        return p;
    }

    private static String shortName(Path p) {
        int n = p.getNameCount();
        if (n == 0) return p.toString();
        return p.subpath(Math.max(0, n - 2), n).toString().replace('\\', '/');
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void chooseFolders() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Choose the folder that contains your save files");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            addAll(saves, fc.getSelectedFiles());
        }
    }

    //shared by the button and drag and drop

    //A folder is added as is. If a file is give the folder is adde instead.
    static void addAll(SaveFileManager saves, File... files) {
        for (File f : files) {
            Path dir = f.isDirectory() ? f.toPath() : f.toPath().getParent();
            if (dir != null) saves.addFolder(dir);
        }
    }

    //accept dropped files and folders.
    public static void installFileDrop(JComponent target, SaveFileManager saves) {
        target.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport s) {
                return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport s) {
                if (!canImport(s)) return false;
                try {
                    List<File> files = (List<File>) s.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    addAll(saves, files.toArray(new File[0]));
                    return true;
                } catch (Exception ex) {
                    return false;
                }
            }
        });
    }
}
