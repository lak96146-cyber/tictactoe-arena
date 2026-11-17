package ui;

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
 * Screen B: Main Dashboard.
 * Hub for matchmaking, creating private rooms, joining with room code,
 * browsing leaderboard, match history, rules, and settings.
 */
public class HomeFrame extends JFrame implements ClientListener {
    private final GameClient client;
    private final ViewManager viewManager;

    private JLabel userWelcomeLabel;
    private JLabel statusBadge;

    public HomeFrame(GameClient client, ViewManager viewManager) {
        this.client = client;
        this.viewManager = viewManager;
        this.client.addListener(this);

        initUI();
    }

    private void initUI() {
        setTitle("Tic-Tac-Toe Arena — Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(560, 680);
        setMinimumSize(new Dimension(500, 620));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Top Navigation Bar
        JPanel navBar = new JPanel(new BorderLayout());
        navBar.setOpaque(false);

        JPanel userSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        userSection.setOpaque(false);

        JLabel avatarLabel = new JLabel("🛡️");
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel nameBox = new JPanel(new GridLayout(2, 1, 0, 2));
        nameBox.setOpaque(false);
        userWelcomeLabel = new JLabel("OPERATOR: " + client.getUsername());
        userWelcomeLabel.setFont(Theme.FONT_SUBTITLE);
        userWelcomeLabel.setForeground(Theme.TEXT_PRIMARY);

        JLabel rankTag = new JLabel("COMPETITOR RANKED");
        rankTag.setFont(Theme.FONT_SMALL_BOLD);
        rankTag.setForeground(Theme.ACCENT_CYAN);

        nameBox.add(userWelcomeLabel);
        nameBox.add(rankTag);
        userSection.add(avatarLabel);
        userSection.add(nameBox);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setOpaque(false);

        statusBadge = new ModernComponents.BadgeLabel("ONLINE", new Color(0, 255, 135, 40), Theme.ACCENT_GREEN);

        JButton settingsBtn = new JButton("⚙");
        settingsBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        settingsBtn.setForeground(Theme.TEXT_SECONDARY);
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setBorderPainted(false);
        settingsBtn.setFocusPainted(false);
        settingsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        settingsBtn.addActionListener(e -> viewManager.showSettings(this));

        JButton rulesBtn = new JButton("❓");
        rulesBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        rulesBtn.setForeground(Theme.TEXT_SECONDARY);
        rulesBtn.setContentAreaFilled(false);
        rulesBtn.setBorderPainted(false);
        rulesBtn.setFocusPainted(false);
        rulesBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rulesBtn.addActionListener(e -> viewManager.showRules(this));

        rightControls.add(statusBadge);
        rightControls.add(rulesBtn);
        rightControls.add(settingsBtn);

        navBar.add(userSection, BorderLayout.WEST);
        navBar.add(rightControls, BorderLayout.EAST);

        // Center Action Cards
        JPanel centerGrid = new JPanel(new GridLayout(3, 1, 0, 14));
        centerGrid.setOpaque(false);

        // Card 1: Quick Play (Matchmaking)
        ModernComponents.ModernPanel playCard = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        playCard.setLayout(new BorderLayout(15, 0));
        playCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel playText = new JPanel(new GridLayout(2, 1, 0, 4));
        playText.setOpaque(false);
        JLabel playTitle = new JLabel("QUICK MATCHMAKING");
        playTitle.setFont(Theme.FONT_SUBTITLE);
        playTitle.setForeground(Theme.TEXT_PRIMARY);
        JLabel playDesc = new JLabel("Instantly join an open arena lobby or auto-create one.");
        playDesc.setFont(Theme.FONT_SMALL);
        playDesc.setForeground(Theme.TEXT_SECONDARY);
        playText.add(playTitle);
        playText.add(playDesc);

        ModernComponents.ModernButton quickPlayBtn = new ModernComponents.ModernButton("PLAY NOW", Theme.ACCENT_PURPLE, Theme.ACCENT_BLUE);
        quickPlayBtn.setPreferredSize(new Dimension(130, 42));
        quickPlayBtn.setGlowingBorder(true);
        quickPlayBtn.addActionListener(e -> client.quickMatch());

        playCard.add(playText, BorderLayout.CENTER);
        playCard.add(quickPlayBtn, BorderLayout.EAST);

        // Card 2: Create Custom Room
        ModernComponents.ModernPanel createCard = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        createCard.setLayout(new BorderLayout(15, 0));
        createCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel createText = new JPanel(new GridLayout(2, 1, 0, 4));
        createText.setOpaque(false);
        JLabel createTitle = new JLabel("CREATE PRIVATE ROOM");
        createTitle.setFont(Theme.FONT_SUBTITLE);
        createTitle.setForeground(Theme.TEXT_PRIMARY);
        JLabel createDesc = new JLabel("Host a match with code: Best of 1, Best of 3, or Best of 5.");
        createDesc.setFont(Theme.FONT_SMALL);
        createDesc.setForeground(Theme.TEXT_SECONDARY);
        createText.add(createTitle);
        createText.add(createDesc);

        ModernComponents.ModernButton createBtn = new ModernComponents.ModernButton("HOST ROOM", Theme.BG_CARD, Theme.BORDER_ACTIVE);
        createBtn.setPreferredSize(new Dimension(130, 42));
        createBtn.addActionListener(e -> showCreateRoomDialog());

        createCard.add(createText, BorderLayout.CENTER);
        createCard.add(createBtn, BorderLayout.EAST);

        // Card 3: Join Private Room
        ModernComponents.ModernPanel joinCard = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        joinCard.setLayout(new BorderLayout(15, 0));
        joinCard.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel joinText = new JPanel(new GridLayout(2, 1, 0, 4));
        joinText.setOpaque(false);
        JLabel joinTitle = new JLabel("JOIN VIA ROOM CODE");
        joinTitle.setFont(Theme.FONT_SUBTITLE);
        joinTitle.setForeground(Theme.TEXT_PRIMARY);
        JLabel joinDesc = new JLabel("Enter a 6-character room code to challenge a friend.");
        joinDesc.setFont(Theme.FONT_SMALL);
        joinDesc.setForeground(Theme.TEXT_SECONDARY);
        joinText.add(joinTitle);
        joinText.add(joinDesc);

        ModernComponents.ModernButton joinBtn = new ModernComponents.ModernButton("JOIN ROOM", Theme.BG_CARD, Theme.BORDER_ACTIVE);
        joinBtn.setPreferredSize(new Dimension(130, 42));
        joinBtn.addActionListener(e -> showJoinRoomDialog());

        joinCard.add(joinText, BorderLayout.CENTER);
        joinCard.add(joinBtn, BorderLayout.EAST);

        centerGrid.add(playCard);
        centerGrid.add(createCard);
        centerGrid.add(joinCard);

        // Bottom Dashboard Buttons (Leaderboard, History, Exit)
        JPanel bottomRow = new JPanel(new GridLayout(1, 3, 12, 0));
        bottomRow.setOpaque(false);

        ModernComponents.ModernButton lbBtn = new ModernComponents.ModernButton("🏆 LEADERBOARD", Theme.BG_CARD, Theme.BORDER_COLOR);
        lbBtn.setFont(Theme.FONT_SMALL_BOLD);
        lbBtn.addActionListener(e -> viewManager.showLeaderboard());

        ModernComponents.ModernButton histBtn = new ModernComponents.ModernButton("📜 MATCH HISTORY", Theme.BG_CARD, Theme.BORDER_COLOR);
        histBtn.setFont(Theme.FONT_SMALL_BOLD);
        histBtn.addActionListener(e -> viewManager.showMatchHistory());

        ModernComponents.ModernButton exitBtn = new ModernComponents.ModernButton("🚪 LOGOUT", Theme.BG_CARD, Theme.ACCENT_NEON_PINK.darker());
        exitBtn.setFont(Theme.FONT_SMALL_BOLD);
        exitBtn.addActionListener(e -> {
            client.disconnect();
            viewManager.showLogin();
        });

        bottomRow.add(lbBtn);
        bottomRow.add(histBtn);
        bottomRow.add(exitBtn);

        mainPanel.add(navBar, BorderLayout.NORTH);
        mainPanel.add(centerGrid, BorderLayout.CENTER);
        mainPanel.add(bottomRow, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    public void refresh() {
        userWelcomeLabel.setText("OPERATOR: " + client.getUsername());
    }

    private void showCreateRoomDialog() {
        JDialog dialog = new JDialog(this, "Host Custom Match", true);
        dialog.setSize(380, 260);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Theme.BG_PANEL);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("SELECT MATCH FORMAT");
        title.setFont(Theme.FONT_SUBTITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JComboBox<String> modeBox = new JComboBox<>(new String[]{
                "Best of 3 (First to 2 Wins)",
                "Best of 1 (Sudden Death)",
                "Best of 5 (Championship Series)"
        });
        modeBox.setFont(Theme.FONT_BODY);
        modeBox.setBackground(Theme.BG_INPUT);
        modeBox.setForeground(Theme.TEXT_PRIMARY);
        modeBox.setMaximumSize(new Dimension(320, 36));

        ModernComponents.ModernButton confirmBtn = new ModernComponents.ModernButton("CREATE LOBBY", Theme.ACCENT_PURPLE, Theme.ACCENT_BLUE);
        confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        confirmBtn.setMaximumSize(new Dimension(320, 42));
        confirmBtn.addActionListener(e -> {
            int idx = modeBox.getSelectedIndex();
            String mode = (idx == 0) ? "BO3" : (idx == 1) ? "BO1" : "BO5";
            dialog.dispose();
            client.createRoom(mode);
        });

        p.add(title);
        p.add(Box.createVerticalStrut(18));
        p.add(modeBox);
        p.add(Box.createVerticalStrut(24));
        p.add(confirmBtn);

        dialog.setContentPane(p);
        dialog.setVisible(true);
    }

    private void showJoinRoomDialog() {
        JDialog dialog = new JDialog(this, "Join Private Room", true);
        dialog.setSize(380, 240);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Theme.BG_PANEL);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("ENTER 6-DIGIT ROOM CODE");
        title.setFont(Theme.FONT_SUBTITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        ModernComponents.ModernTextField codeField = new ModernComponents.ModernTextField("e.g. 7X8K2M");
        codeField.setMaximumSize(new Dimension(320, 40));
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.setFont(new Font("Consolas", Font.BOLD, 18));

        ModernComponents.ModernButton joinBtn = new ModernComponents.ModernButton("ENTER ROOM", Theme.ACCENT_BLUE, Theme.ACCENT_PURPLE);
        joinBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        joinBtn.setMaximumSize(new Dimension(320, 42));
        joinBtn.addActionListener(e -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a room code.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            dialog.dispose();
            client.joinRoom(code);
        });

        p.add(title);
        p.add(Box.createVerticalStrut(16));
        p.add(codeField);
        p.add(Box.createVerticalStrut(20));
        p.add(joinBtn);

        dialog.setContentPane(p);
        dialog.setVisible(true);
    }

    // --- ClientListener Callbacks ---

    @Override
    public void onRoomCreated(String roomCode, String mode, String hostName) {
        viewManager.showLobby();
    }

    @Override
    public void onRoomJoined(String roomCode, String hostName, String guestName, String mode) {
        viewManager.showLobby();
    }

    @Override
    public void onJoinFailed(String reason) {
        JOptionPane.showMessageDialog(this, reason, "Join Room Failed", JOptionPane.ERROR_MESSAGE);
    }

    @Override public void onConnected() {}
    @Override public void onConnectionFailed(String r) {}
    @Override public void onDisconnected(String r) {
        JOptionPane.showMessageDialog(this, "Disconnected from server: " + r, "Disconnected", JOptionPane.WARNING_MESSAGE);
        viewManager.showLogin();
    }
    @Override public void onLoginSuccess(String u) {}
    @Override public void onPlayerJoined(String c, String g) {}
    @Override public void onReadyStatus(String c, boolean hr, boolean gr) {}
    @Override public void onGameStart(String c, String x, String o, PlayerSymbol s, PlayerSymbol t, String m, int r, int tw, int sh, int sg) {}
    @Override public void onMoveMade(String c, int r, int col, PlayerSymbol s, PlayerSymbol nt) {}
    @Override public void onMoveRejected(String r) {}
    @Override public void onRoundResult(String c, String w, WinningLine l, int sh, int sg, boolean so) {}
    @Override public void onMatchOver(String c, String w, int sh, int sg, int d) {}
    @Override public void onChatReceived(String c, String s, String m) {}
    @Override public void onRematchRequested(String f) {}
    @Override public void onRematchStatus(String s, String d) {}
    @Override public void onOpponentLeft(String r) {}
    @Override public void onLeaderboardReceived(List<PlayerStats> l) {}
    @Override public void onHistoryReceived(List<MatchRecord> h) {}
}
