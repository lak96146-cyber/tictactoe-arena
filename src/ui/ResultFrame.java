package ui;

import audio.SoundManager;
import client.ClientListener;
import client.GameClient;
import game.PlayerSymbol;
import game.WinningLine;
import history.MatchRecord;
import history.PlayerStats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Screen E: Result Screen.
 * Displays winner/draw announcement, match duration, final score,
 * and allows mutual rematch negotiation or return to dashboard.
 */
public class ResultFrame extends JDialog implements ClientListener {
    private final GameClient client;
    private final ViewManager viewManager;
    private final String winnerName;
    private final int sHost;
    private final int sGuest;
    private final int durationSec;

    private ModernComponents.ModernButton rematchBtn;
    private JLabel rematchStatusLabel;

    public ResultFrame(JFrame parent, GameClient client, ViewManager viewManager,
                       String winnerName, int sHost, int sGuest, int durationSec) {
        super(parent, "Match Concluded", true);
        this.client = client;
        this.viewManager = viewManager;
        this.winnerName = winnerName;
        this.sHost = sHost;
        this.sGuest = sGuest;
        this.durationSec = durationSec;

        this.client.addListener(this);

        initUI();
    }

    private void initUI() {
        setSize(460, 520);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(Theme.BG_PANEL);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(30, 36, 30, 36));

        boolean isDraw = (winnerName == null || winnerName.equalsIgnoreCase("DRAW"));
        boolean iWon = (!isDraw && winnerName.equalsIgnoreCase(client.getUsername()));

        // Outcome Header
        JLabel iconLabel = new JLabel(isDraw ? "🤝" : (iWon ? "🏆" : "💀"), SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String headline = isDraw ? "STALEMATE" : (iWon ? "VICTORY ACHIEVED" : "DEFEAT");
        Color headColor = isDraw ? Theme.ACCENT_AMBER : (iWon ? Theme.ACCENT_GREEN : Theme.ACCENT_NEON_PINK);

        JLabel outcomeLabel = new JLabel(headline, SwingConstants.CENTER);
        outcomeLabel.setFont(Theme.FONT_DISPLAY);
        outcomeLabel.setForeground(headColor);
        outcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel champLabel = new JLabel(isDraw ? "HONORABLE DRAW" : "CHAMPION: " + winnerName, SwingConstants.CENTER);
        champLabel.setFont(Theme.FONT_SUBTITLE);
        champLabel.setForeground(Theme.TEXT_PRIMARY);
        champLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Stats Card
        ModernComponents.ModernPanel statsCard = new ModernComponents.ModernPanel(14, Theme.BG_CARD, Theme.BORDER_COLOR);
        statsCard.setLayout(new GridLayout(2, 2, 10, 10));
        statsCard.setBorder(new EmptyBorder(16, 16, 16, 16));
        statsCard.setMaximumSize(new Dimension(380, 100));

        JPanel p1 = createStatItem("FINAL SCORE", sHost + " - " + sGuest);
        JPanel p2 = createStatItem("DURATION", formatDuration(durationSec));
        JPanel p3 = createStatItem("SERIES FORMAT", client.getMatchMode());
        JPanel p4 = createStatItem("ARENA ROOM", client.getCurrentRoomCode());

        statsCard.add(p1);
        statsCard.add(p2);
        statsCard.add(p3);
        statsCard.add(p4);

        // Rematch Status Label
        rematchStatusLabel = new JLabel("Both combatants must agree to rematch.", SwingConstants.CENTER);
        rematchStatusLabel.setFont(Theme.FONT_SMALL);
        rematchStatusLabel.setForeground(Theme.TEXT_SECONDARY);
        rematchStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Action Buttons
        rematchBtn = new ModernComponents.ModernButton("REQUEST REMATCH", Theme.ACCENT_PURPLE, Theme.ACCENT_BLUE);
        rematchBtn.setMaximumSize(new Dimension(380, 44));
        rematchBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        rematchBtn.setGlowingBorder(true);
        rematchBtn.addActionListener(e -> {
            rematchBtn.setEnabled(false);
            rematchBtn.setText("WAITING FOR OPPONENT...");
            rematchStatusLabel.setText("Rematch requested! Awaiting opponent's response...");
            rematchStatusLabel.setForeground(Theme.ACCENT_CYAN);
            client.requestRematch();
        });

        ModernComponents.ModernButton dashBtn = new ModernComponents.ModernButton("RETURN TO DASHBOARD", Theme.BG_PANEL, Theme.BORDER_COLOR);
        dashBtn.setMaximumSize(new Dimension(380, 42));
        dashBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        dashBtn.addActionListener(e -> {
            client.leaveRoom();
            dispose();
            viewManager.showHome();
        });

        mainPanel.add(iconLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(outcomeLabel);
        mainPanel.add(Box.createVerticalStrut(4));
        mainPanel.add(champLabel);
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(statsCard);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(rematchStatusLabel);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(rematchBtn);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(dashBtn);

        setContentPane(mainPanel);
    }

    private JPanel createStatItem(String label, String value) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_SMALL_BOLD);
        l.setForeground(Theme.TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(Theme.FONT_BODY_BOLD);
        v.setForeground(Theme.TEXT_PRIMARY);
        p.add(l);
        p.add(v);
        return p;
    }

    private String formatDuration(int sec) {
        int m = sec / 60;
        int s = sec % 60;
        return (m > 0) ? String.format("%dm %02ds", m, s) : String.format("%ds", s);
    }

    // --- ClientListener Callbacks ---

    @Override
    public void onRematchRequested(String fromPlayer) {
        SoundManager.getInstance().playNotification();
        int opt = JOptionPane.showConfirmDialog(
                this,
                fromPlayer + " has requested a rematch! Do you accept?",
                "Rematch Challenge",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        boolean accept = (opt == JOptionPane.YES_OPTION);
        client.respondRematch(accept);
        if (!accept) {
            rematchStatusLabel.setText("You declined the rematch offer.");
            rematchStatusLabel.setForeground(Theme.ACCENT_NEON_PINK);
        }
    }

    @Override
    public void onRematchStatus(String status, String details) {
        if ("ACCEPTED".equalsIgnoreCase(status)) {
            dispose();
            // Match restarts
        } else {
            rematchStatusLabel.setText("Rematch declined by opponent.");
            rematchStatusLabel.setForeground(Theme.ACCENT_NEON_PINK);
            rematchBtn.setEnabled(true);
            rematchBtn.setText("REQUEST REMATCH");
        }
    }

    @Override
    public void onGameStart(String c, String x, String o, PlayerSymbol s, PlayerSymbol t, String m, int r, int tw, int sh, int sg) {
        dispose();
    }

    @Override
    public void onOpponentLeft(String reason) {
        rematchStatusLabel.setText("Opponent left the arena.");
        rematchBtn.setEnabled(false);
    }

    @Override public void onConnected() {}
    @Override public void onConnectionFailed(String r) {}
    @Override public void onDisconnected(String r) { dispose(); }
    @Override public void onLoginSuccess(String u) {}
    @Override public void onRoomCreated(String c, String m, String h) {}
    @Override public void onRoomJoined(String c, String h, String g, String m) {}
    @Override public void onJoinFailed(String r) {}
    @Override public void onPlayerJoined(String c, String g) {}
    @Override public void onReadyStatus(String c, boolean hr, boolean gr) {}
    @Override public void onMoveMade(String c, int r, int col, PlayerSymbol s, PlayerSymbol nt) {}
    @Override public void onMoveRejected(String r) {}
    @Override public void onRoundResult(String c, String w, WinningLine l, int sh, int sg, boolean so) {}
    @Override public void onMatchOver(String c, String w, int sh, int sg, int d) {}
    @Override public void onChatReceived(String c, String s, String m) {}
    @Override public void onLeaderboardReceived(List<PlayerStats> l) {}
    @Override public void onHistoryReceived(List<MatchRecord> h) {}
}
