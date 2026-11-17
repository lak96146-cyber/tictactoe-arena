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
import java.awt.datatransfer.StringSelection;
import java.util.List;

/**
 * Screen C: Multiplayer Lobby Screen.
 * Displays room code, player slots, X/O assignment, ready indicators,
 * and handles countdown to match start.
 */
public class LobbyFrame extends JFrame implements ClientListener {
    private final GameClient client;
    private final ViewManager viewManager;

    private JLabel roomCodeLabel;
    private JLabel modeBadge;
    private JLabel hostNameLabel;
    private JLabel hostReadyLabel;
    private JLabel guestNameLabel;
    private JLabel guestReadyLabel;
    private ModernComponents.ModernButton readyBtn;
    private ModernComponents.ModernButton startBtn;
    private JLabel statusNoticeLabel;

    private boolean isReady = false;

    public LobbyFrame(GameClient client, ViewManager viewManager) {
        this.client = client;
        this.viewManager = viewManager;
        this.client.addListener(this);

        initUI();
    }

    private void initUI() {
        setTitle("Tic-Tac-Toe Arena — Match Lobby");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                leaveLobby();
            }
        });

        setSize(560, 680);
        setMinimumSize(new Dimension(500, 620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header Panel: Room Code & Copy Button
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel lobbyTitle = new JLabel("MULTIPLAYER ARENA LOBBY", SwingConstants.CENTER);
        lobbyTitle.setFont(Theme.FONT_TITLE);
        lobbyTitle.setForeground(Theme.TEXT_PRIMARY);
        lobbyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel codeCard = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        codeCard.setOpaque(false);

        roomCodeLabel = new JLabel("CODE: ------");
        roomCodeLabel.setFont(new Font("Consolas", Font.BOLD, 22));
        roomCodeLabel.setForeground(Theme.ACCENT_CYAN);

        ModernComponents.ModernButton copyBtn = new ModernComponents.ModernButton("📋 COPY", Theme.BG_CARD, Theme.BORDER_ACTIVE);
        copyBtn.setFont(Theme.FONT_SMALL_BOLD);
        copyBtn.setPreferredSize(new Dimension(90, 32));
        copyBtn.addActionListener(e -> copyRoomCode());

        modeBadge = new ModernComponents.BadgeLabel("BEST OF 3", Theme.ACCENT_PURPLE, Color.WHITE);

        codeCard.add(roomCodeLabel);
        codeCard.add(copyBtn);
        codeCard.add(modeBadge);

        headerPanel.add(lobbyTitle);
        headerPanel.add(Box.createVerticalStrut(12));
        headerPanel.add(codeCard);

        // Center Panel: Player Slots (Host & Guest)
        JPanel slotsPanel = new JPanel(new GridLayout(2, 1, 0, 16));
        slotsPanel.setOpaque(false);

        // Slot 1: Host Card (Symbol X)
        ModernComponents.ModernPanel hostCard = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        hostCard.setLayout(new BorderLayout(15, 0));
        hostCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel xSymbolIcon = new JLabel("X");
        xSymbolIcon.setFont(Theme.FONT_DISPLAY);
        xSymbolIcon.setForeground(Theme.ACCENT_NEON_PINK);

        JPanel hostDetails = new JPanel(new GridLayout(2, 1, 0, 4));
        hostDetails.setOpaque(false);
        hostNameLabel = new JLabel(client.isHost() ? client.getUsername() : client.getOpponentName());
        hostNameLabel.setFont(Theme.FONT_SUBTITLE);
        hostNameLabel.setForeground(Theme.TEXT_PRIMARY);

        JLabel hostRole = new JLabel("HOST • SYMBOL X");
        hostRole.setFont(Theme.FONT_SMALL_BOLD);
        hostRole.setForeground(Theme.TEXT_SECONDARY);
        hostDetails.add(hostNameLabel);
        hostDetails.add(hostRole);

        hostReadyLabel = new JLabel("NOT READY", SwingConstants.RIGHT);
        hostReadyLabel.setFont(Theme.FONT_SMALL_BOLD);
        hostReadyLabel.setForeground(Theme.TEXT_MUTED);

        hostCard.add(xSymbolIcon, BorderLayout.WEST);
        hostCard.add(hostDetails, BorderLayout.CENTER);
        hostCard.add(hostReadyLabel, BorderLayout.EAST);

        // Slot 2: Guest Card (Symbol O)
        ModernComponents.ModernPanel guestCard = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        guestCard.setLayout(new BorderLayout(15, 0));
        guestCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel oSymbolIcon = new JLabel("O");
        oSymbolIcon.setFont(Theme.FONT_DISPLAY);
        oSymbolIcon.setForeground(Theme.ACCENT_CYAN);

        JPanel guestDetails = new JPanel(new GridLayout(2, 1, 0, 4));
        guestDetails.setOpaque(false);
        guestNameLabel = new JLabel(client.isHost() ? "WAITING FOR OPPONENT..." : client.getUsername());
        guestNameLabel.setFont(Theme.FONT_SUBTITLE);
        guestNameLabel.setForeground(Theme.TEXT_PRIMARY);

        JLabel guestRole = new JLabel("GUEST • SYMBOL O");
        guestRole.setFont(Theme.FONT_SMALL_BOLD);
        guestRole.setForeground(Theme.TEXT_SECONDARY);
        guestDetails.add(guestNameLabel);
        guestDetails.add(guestRole);

        guestReadyLabel = new JLabel("NOT READY", SwingConstants.RIGHT);
        guestReadyLabel.setFont(Theme.FONT_SMALL_BOLD);
        guestReadyLabel.setForeground(Theme.TEXT_MUTED);

        guestCard.add(oSymbolIcon, BorderLayout.WEST);
        guestCard.add(guestDetails, BorderLayout.CENTER);
        guestCard.add(guestReadyLabel, BorderLayout.EAST);

        slotsPanel.add(hostCard);
        slotsPanel.add(guestCard);

        // Bottom Controls
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        statusNoticeLabel = new JLabel("Waiting for both players to declare READY...", SwingConstants.CENTER);
        statusNoticeLabel.setFont(Theme.FONT_SMALL);
        statusNoticeLabel.setForeground(Theme.TEXT_SECONDARY);
        statusNoticeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new GridLayout(1, 3, 12, 0));
        btnRow.setOpaque(false);

        readyBtn = new ModernComponents.ModernButton("READY", Theme.BG_CARD, Theme.ACCENT_GREEN.darker());
        readyBtn.setFont(Theme.FONT_BODY_BOLD);
        readyBtn.addActionListener(e -> {
            isReady = !isReady;
            readyBtn.setText(isReady ? "CANCEL READY" : "READY");
            readyBtn.setBaseColor(isReady ? Theme.ACCENT_GREEN.darker() : Theme.BG_CARD);
            client.toggleReady();
        });

        startBtn = new ModernComponents.ModernButton("START MATCH", Theme.ACCENT_PURPLE, Theme.ACCENT_BLUE);
        startBtn.setFont(Theme.FONT_BODY_BOLD);
        startBtn.setGlowingBorder(true);
        startBtn.setEnabled(client.isHost());
        startBtn.addActionListener(e -> client.startMatch());

        ModernComponents.ModernButton leaveBtn = new ModernComponents.ModernButton("LEAVE LOBBY", Theme.BG_CARD, Theme.ACCENT_NEON_PINK.darker());
        leaveBtn.setFont(Theme.FONT_BODY_BOLD);
        leaveBtn.addActionListener(e -> leaveLobby());

        btnRow.add(readyBtn);
        if (client.isHost()) {
            btnRow.add(startBtn);
        }
        btnRow.add(leaveBtn);

        bottomPanel.add(statusNoticeLabel);
        bottomPanel.add(Box.createVerticalStrut(14));
        bottomPanel.add(btnRow);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(slotsPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    public void refresh() {
        roomCodeLabel.setText("CODE: " + (client.getCurrentRoomCode() != null ? client.getCurrentRoomCode() : "------"));
        modeBadge.setText("MODE: " + client.getMatchMode());

        if (client.isHost()) {
            hostNameLabel.setText(client.getUsername());
            guestNameLabel.setText(client.getOpponentName().isEmpty() ? "WAITING FOR OPPONENT..." : client.getOpponentName());
        } else {
            hostNameLabel.setText(client.getOpponentName());
            guestNameLabel.setText(client.getUsername());
        }
        isReady = false;
        readyBtn.setText("READY");
        readyBtn.setBaseColor(Theme.BG_CARD);
        startBtn.setEnabled(client.isHost());
    }

    private void copyRoomCode() {
        String code = client.getCurrentRoomCode();
        if (code != null && !code.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
            JOptionPane.showMessageDialog(this, "Room Code '" + code + "' copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void leaveLobby() {
        client.leaveRoom();
        viewManager.showHome();
    }

    // --- ClientListener Callbacks ---

    @Override
    public void onPlayerJoined(String roomCode, String guestName) {
        guestNameLabel.setText(guestName);
        statusNoticeLabel.setText(guestName + " has entered the arena lobby!");
        SoundManager.getInstance().playNotification();
    }

    @Override
    public void onReadyStatus(String roomCode, boolean hostReady, boolean guestReady) {
        hostReadyLabel.setText(hostReady ? "READY ✓" : "NOT READY");
        hostReadyLabel.setForeground(hostReady ? Theme.ACCENT_GREEN : Theme.TEXT_MUTED);

        guestReadyLabel.setText(guestReady ? "READY ✓" : "NOT READY");
        guestReadyLabel.setForeground(guestReady ? Theme.ACCENT_GREEN : Theme.TEXT_MUTED);

        if (hostReady && guestReady) {
            statusNoticeLabel.setText("Both combatants ready! Launching match...");
            statusNoticeLabel.setForeground(Theme.ACCENT_GREEN);
        } else {
            statusNoticeLabel.setText("Waiting for both players to declare READY...");
            statusNoticeLabel.setForeground(Theme.TEXT_SECONDARY);
        }
    }

    @Override
    public void onGameStart(String roomCode, String pX, String pO, PlayerSymbol mySym,
                            PlayerSymbol curTurn, String mode, int curRound, int targetWins,
                            int sHost, int sGuest) {
        viewManager.showGame();
    }

    @Override
    public void onOpponentLeft(String reason) {
        JOptionPane.showMessageDialog(this, reason, "Opponent Left", JOptionPane.WARNING_MESSAGE);
        viewManager.showHome();
    }

    @Override public void onConnected() {}
    @Override public void onConnectionFailed(String r) {}
    @Override public void onDisconnected(String r) {
        viewManager.showLogin();
    }
    @Override public void onLoginSuccess(String u) {}
    @Override public void onRoomCreated(String c, String m, String h) { refresh(); }
    @Override public void onRoomJoined(String c, String h, String g, String m) { refresh(); }
    @Override public void onJoinFailed(String r) {}
    @Override public void onMoveMade(String c, int r, int col, PlayerSymbol s, PlayerSymbol nt) {}
    @Override public void onMoveRejected(String r) {}
    @Override public void onRoundResult(String c, String w, WinningLine l, int sh, int sg, boolean so) {}
    @Override public void onMatchOver(String c, String w, int sh, int sg, int d) {}
    @Override public void onChatReceived(String c, String s, String m) {}
    @Override public void onRematchRequested(String f) {}
    @Override public void onRematchStatus(String s, String d) {}
    @Override public void onLeaderboardReceived(List<PlayerStats> l) {}
    @Override public void onHistoryReceived(List<MatchRecord> h) {}
}
