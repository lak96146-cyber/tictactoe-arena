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
 * Screen A: Welcome & Login Screen.
 * Allows player to set username, server address, and establish connection.
 */
public class LoginFrame extends JFrame implements ClientListener {
    private final GameClient client;
    private final ViewManager viewManager;

    private ModernComponents.ModernTextField usernameField;
    private ModernComponents.ModernTextField hostField;
    private ModernComponents.ModernTextField portField;
    private ModernComponents.ModernButton connectBtn;
    private JLabel statusLabel;
    private JPanel statusDot;

    public LoginFrame(GameClient client, ViewManager viewManager) {
        this.client = client;
        this.viewManager = viewManager;
        this.client.addListener(this);

        initUI();
    }

    private void initUI() {
        setTitle("Tic-Tac-Toe Arena — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 640);
        setMinimumSize(new Dimension(420, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Header Section (Logo, Title, Tagline)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel logoBadge = new JLabel("⚡", SwingConstants.CENTER);
        logoBadge.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        logoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("TIC-TAC-TOE ARENA", SwingConstants.CENTER);
        titleLabel.setFont(Theme.FONT_DISPLAY);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("COMPETITIVE DESKTOP MULTIPLAYER", SwingConstants.CENTER);
        subtitleLabel.setFont(Theme.FONT_SMALL_BOLD);
        subtitleLabel.setForeground(Theme.ACCENT_CYAN);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(logoBadge);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);

        // Center Form Panel
        ModernComponents.ModernPanel formCard = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel userLabel = new JLabel("CALLSIGN / USERNAME");
        userLabel.setFont(Theme.FONT_SMALL_BOLD);
        userLabel.setForeground(Theme.TEXT_SECONDARY);

        usernameField = new ModernComponents.ModernTextField("e.g. CyberViper");
        usernameField.setText("Player" + (int)(Math.random() * 900 + 100));

        JLabel hostLabel = new JLabel("SERVER HOST / RENDER URL");
        hostLabel.setFont(Theme.FONT_SMALL_BOLD);
        hostLabel.setForeground(Theme.TEXT_SECONDARY);

        hostField = new ModernComponents.ModernTextField("localhost or your-app.onrender.com");
        hostField.setText("localhost");

        JLabel portLabel = new JLabel("SERVER PORT (8888 for local, 443 for Render/WSS)");
        portLabel.setFont(Theme.FONT_SMALL_BOLD);
        portLabel.setForeground(Theme.TEXT_SECONDARY);

        portField = new ModernComponents.ModernTextField("8888");
        portField.setText("8888");

        formCard.add(userLabel);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(usernameField);
        formCard.add(Box.createVerticalStrut(14));
        formCard.add(hostLabel);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(hostField);
        formCard.add(Box.createVerticalStrut(14));
        formCard.add(portLabel);
        formCard.add(Box.createVerticalStrut(6));
        formCard.add(portField);

        // Status Panel (Dot + text)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        statusPanel.setOpaque(false);
        statusDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyQualityRendering(g2);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        statusDot.setPreferredSize(new Dimension(10, 10));
        statusDot.setBackground(Theme.TEXT_MUTED);

        statusLabel = new JLabel("Ready to connect");
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_SECONDARY);

        statusPanel.add(statusDot);
        statusPanel.add(statusLabel);

        // Bottom Actions
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        connectBtn = new ModernComponents.ModernButton("ENTER ARENA", Theme.ACCENT_PURPLE, Theme.ACCENT_BLUE);
        connectBtn.setFont(Theme.FONT_SUBTITLE);
        connectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectBtn.setMaximumSize(new Dimension(360, 48));
        connectBtn.setPreferredSize(new Dimension(360, 48));
        connectBtn.setGlowingBorder(true);
        connectBtn.addActionListener(e -> attemptConnect());

        bottomPanel.add(statusPanel);
        bottomPanel.add(Box.createVerticalStrut(14));
        bottomPanel.add(connectBtn);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formCard, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void attemptConnect() {
        String username = usernameField.getText().trim();
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();

        if (username.isEmpty()) {
            setStatus("Please enter a username.", Theme.ACCENT_NEON_PINK);
            return;
        }
        if (host.isEmpty()) {
            host = "localhost";
        }

        int port = 8888;
        if (!portStr.isEmpty()) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                setStatus("Invalid port number.", Theme.ACCENT_NEON_PINK);
                return;
            }
        } else if (host.toLowerCase().contains("onrender.com") || host.startsWith("https://") || host.startsWith("wss://")) {
            port = 443;
        }

        boolean isSecure = host.toLowerCase().contains("onrender.com") || port == 443 || host.startsWith("wss://");
        String proto = isSecure ? "WSS (Secure WebSocket)" : "WS (WebSocket)";
        setStatus("Connecting via " + proto + " to " + host + ":" + port + "...", Theme.ACCENT_AMBER);
        connectBtn.setEnabled(false);

        client.connect(host, port, username);
    }

    private void setStatus(String text, Color dotColor) {
        statusLabel.setText(text);
        statusDot.setBackground(dotColor);
        statusDot.repaint();
    }

    // --- ClientListener Callbacks ---

    @Override
    public void onConnected() {
        setStatus("Connected to server! Logging in...", Theme.ACCENT_GREEN);
    }

    @Override
    public void onLoginSuccess(String user) {
        setStatus("Authenticated as " + user, Theme.ACCENT_GREEN);
        connectBtn.setEnabled(true);
        // Navigate to Home Dashboard
        viewManager.showHome();
    }

    @Override
    public void onConnectionFailed(String reason) {
        setStatus("Connection failed: " + reason, Theme.ACCENT_NEON_PINK);
        connectBtn.setEnabled(true);
    }

    @Override
    public void onDisconnected(String reason) {
        setStatus("Disconnected: " + reason, Theme.TEXT_MUTED);
        connectBtn.setEnabled(true);
    }

    @Override public void onRoomCreated(String c, String m, String h) {}
    @Override public void onRoomJoined(String c, String h, String g, String m) {}
    @Override public void onJoinFailed(String r) {}
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
