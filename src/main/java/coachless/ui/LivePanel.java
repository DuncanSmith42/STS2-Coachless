package coachless.ui;

import coachless.GameSessionFramework.session.GameSession;
import coachless.data.RunFileWatcher;
import coachless.multiplayer.MultiplayerController;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class LivePanel extends JPanel implements MultiplayerController.Listener {

    private final JLabel status = new JLabel("No run file selected");
    private final JTextArea output = new JTextArea();
    private RunFileWatcher watcher;

    //  multiplayer 
    private static final String CARD_OFF = "off";
    private static final String CARD_SETUP = "setup";
    private static final String CARD_SESSION = "session";

    private final MultiplayerController multiplayer = new MultiplayerController(this);

    private final JRadioButton singleBtn = new JRadioButton("Single player", true);
    private final JRadioButton multiBtn = new JRadioButton("Multiplayer");
    private final CardLayout multiCards = new CardLayout();
    private final JPanel multiArea = new JPanel(multiCards);

    private final JTextField nameField = new JTextField("Player", 12);
    private final JTextField hostPortField = new JTextField("5555", 5);
    private final JButton hostBtn = new JButton("Host game");
    private final JTextField joinAddressField = new JTextField(12);
    private final JTextField joinPortField = new JTextField("5555", 5);
    private final JTextField joinCodeField = new JTextField(8);
    private final JButton joinBtn = new JButton("Join game");

    private final JLabel codeLabel = new JLabel("-");
    private final JLabel addressLabel = new JLabel("-");
    private final JLabel playersCountLabel = new JLabel("Players");
    private final JLabel sessionStatus = new JLabel(" ");
    private final DefaultListModel<String> playersModel = new DefaultListModel<>();
    private final JList<String> playersList = new JList<>(playersModel);
    private String inviteText = "";

    public LivePanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton choose = new JButton("Choose run file...");
        choose.addActionListener(e -> chooseFile());

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.add(choose, BorderLayout.WEST);
        top.add(status, BorderLayout.CENTER);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.add(top, BorderLayout.NORTH);
        north.add(buildMultiplayerPanel(), BorderLayout.CENTER);

        add(north, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
    }


    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        if (watcher != null) watcher.stop();
        status.setText("Watching: " + p);
        watcher = new RunFileWatcher(p, text -> SwingUtilities.invokeLater(() -> onRunUpdate(text)));
        watcher.start();
    }

    // Called on the Swing thread each time the run file changes.
    private void onRunUpdate(String json) {
        output.setText(json);
        output.setCaretPosition(0);
    }

    // Multiplayer UI

    private JPanel buildMultiplayerPanel() {
        ButtonGroup group = new ButtonGroup();
        group.add(singleBtn);
        group.add(multiBtn);
        singleBtn.addActionListener(e -> onModeChanged());
        multiBtn.addActionListener(e -> onModeChanged());

        JPanel modeRow = row(new JLabel("Mode:"), singleBtn, multiBtn);

        multiArea.add(new JPanel(), CARD_OFF);
        multiArea.add(buildSetupCard(), CARD_SETUP);
        multiArea.add(buildSessionCard(), CARD_SESSION);
        multiCards.show(multiArea, CARD_OFF);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(modeRow, BorderLayout.NORTH);
        panel.add(multiArea, BorderLayout.CENTER);
        return panel;
    }

    // host or join in multiplayer section
    private JPanel buildSetupCard() {
        hostBtn.addActionListener(e -> onHostClicked());
        joinBtn.addActionListener(e -> onJoinClicked());

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createTitledBorder("Multiplayer"));
        card.add(row(new JLabel("Your name:"), nameField));
        card.add(row(new JLabel("Host a game - port:"), hostPortField, hostBtn));
        card.add(row(new JLabel("Join a game - address:"), joinAddressField,
                new JLabel("port:"), joinPortField,
                new JLabel("code:"), joinCodeField, joinBtn));
        return card;
    }

    // code, address, player list, leave
    private JPanel buildSessionCard() {
        codeLabel.setFont(codeLabel.getFont().deriveFont(Font.BOLD, 18f));

        JButton copyBtn = new JButton("Copy invite");
        copyBtn.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(inviteText), null));
        JButton leaveBtn = new JButton("Leave session");
        leaveBtn.addActionListener(e -> onLeaveClicked());

        JPanel info = new JPanel(new GridLayout(2, 2, 8, 2));
        info.add(new JLabel("Session code:"));
        info.add(codeLabel);
        info.add(new JLabel("Address:"));
        info.add(addressLabel);

        JPanel players = new JPanel(new BorderLayout(0, 2));
        players.add(playersCountLabel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(playersList);
        scroll.setPreferredSize(new Dimension(200, 80));
        players.add(scroll, BorderLayout.CENTER);

        JPanel bottom = row(copyBtn, leaveBtn, sessionStatus);

        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createTitledBorder("Multiplayer session"));
        card.add(info, BorderLayout.NORTH);
        card.add(players, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private static JPanel row(Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        for (Component c : parts) {
            p.add(c);
        }
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private void onModeChanged() {
        if (multiBtn.isSelected()) {
            multiCards.show(multiArea, multiplayer.isInSession() ? CARD_SESSION : CARD_SETUP);
        } else {
            if (multiplayer.isInSession()) {
                multiplayer.leaveSession();
            }
            multiCards.show(multiArea, CARD_OFF);
        }
    }

    private void onHostClicked() {
        String name = readName();
        Integer port = readPort(hostPortField);
        if (name == null || port == null) return;
        try {
            multiplayer.hostGame(name, port); //  onHosting() switches the card
        } catch (IOException ex) {
            showError("Could not start hosting: " + ex.getMessage());
        }
    }

    private void onJoinClicked() {
        String name = readName();
        Integer port = readPort(joinPortField);
        String address = joinAddressField.getText().trim();
        String code = joinCodeField.getText().trim().toUpperCase();
        if (name == null || port == null) return;
        if (address.isEmpty() || code.isEmpty()) {
            showError("Enter the host's address and the session code.");
            return;
        }

        joinBtn.setEnabled(false);
        //not on swing thread because itoo slow
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                multiplayer.joinGame(address, port, code, name);
                return null;
            }

            @Override
            protected void done() {
                joinBtn.setEnabled(true);
                try {
                    get();
                    showSession(code, address + ":" + port,
                            "Join my STS Coachless game - address " + address
                                    + ", port " + port + ", code " + code);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showError("Could not join: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void onLeaveClicked() {
        multiplayer.leaveSession();
        playersModel.clear();
        multiCards.show(multiArea, CARD_SETUP);
    }

    private void showSession(String code, String address, String invite) {
        codeLabel.setText(code);
        addressLabel.setText(address);
        inviteText = invite;
        multiBtn.setSelected(true);
        multiCards.show(multiArea, CARD_SESSION);
    }

    private String readName() {
        String name = nameField.getText().trim().replace(",", "");
        if (name.isEmpty()) {
            showError("Enter your name first.");
            return null;
        }
        return name;
    }

    private Integer readPort(JTextField field) {
        try {
            int port = Integer.parseInt(field.getText().trim());
            if (port >= 1 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
        }
        showError("Enter a port between 1 and 65535.");
        return null;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Multiplayer", JOptionPane.ERROR_MESSAGE);
    }


    @Override
    public void onStatusChanged(String s) {
        sessionStatus.setText(s);
    }

    @Override
    public void onPlayersChanged(List<String> names) {
        playersModel.clear();
        for (int i = 0; i < names.size(); i++) {
            playersModel.addElement(i == 0 ? names.get(i) + " (host)" : names.get(i));
        }
        playersCountLabel.setText("Players (" + names.size() + "/" + GameSession.MAX_PLAYERS + ")");
    }

    @Override
    public void onHosting(String code, String address, int port) {
        showSession(code, address + ":" + port,
                "Join my STS Coachless game - address " + address
                        + ", port " + port + ", code " + code);
    }

    @Override
    public void onDisconnected(String reason) {
        playersModel.clear();
        multiCards.show(multiArea, CARD_SETUP);
        JOptionPane.showMessageDialog(this, reason, "Disconnected", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void onMessageReceived(String message) {
        
    }
}
