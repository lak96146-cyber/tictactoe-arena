package ui;

import client.ClientListener;
import client.GameClient;
import game.PlayerSymbol;
import game.WinningLine;
import history.MatchRecord;
import history.PlayerStats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

/**
 * Screen F: Leaderboard Screen.
 * Shows player rankings, win/loss records, and win rates in a sleek dark table.
 */
public class LeaderboardFrame extends JFrame implements ClientListener {
    private final GameClient client;
    private final ViewManager viewManager;

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel countLabel;

    public LeaderboardFrame(GameClient client, ViewManager viewManager) {
        this.client = client;
        this.viewManager = viewManager;
        this.client.addListener(this);

        initUI();
    }

    private void initUI() {
        setTitle("Tic-Tac-Toe Arena — Global Leaderboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 600);
        setMinimumSize(new Dimension(600, 480));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        JLabel titleLabel = new JLabel("GLOBAL HALL OF FAME");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);

        countLabel = new JLabel("Ranking elite combatants by verified wins & win rate");
        countLabel.setFont(Theme.FONT_SMALL);
        countLabel.setForeground(Theme.TEXT_SECONDARY);
        titleBox.add(titleLabel);
        titleBox.add(countLabel);

        ModernComponents.ModernButton refreshBtn = new ModernComponents.ModernButton("🔄 REFRESH", Theme.BG_CARD, Theme.BORDER_ACTIVE);
        refreshBtn.setFont(Theme.FONT_SMALL_BOLD);
        refreshBtn.setPreferredSize(new Dimension(110, 36));
        refreshBtn.addActionListener(e -> requestData());

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);

        // Table
        String[] columnNames = {"RANK", "OPERATOR", "MATCHES", "WINS", "LOSSES", "DRAWS", "WIN RATE"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(36);
        table.setFont(Theme.FONT_BODY);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setBackground(Theme.BG_PANEL);
        table.setSelectionBackground(Theme.BG_CARD);
        table.setSelectionForeground(Theme.ACCENT_CYAN);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.FONT_SMALL_BOLD);
        header.setBackground(Theme.BG_CARD);
        header.setForeground(Theme.TEXT_SECONDARY);
        header.setPreferredSize(new Dimension(0, 38));
        header.setBorder(null);

        // Custom Cell Renderer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSelected, hasFocus, row, col);
                setHorizontalAlignment(col == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);

                if (!isSelected) {
                    c.setBackground((row % 2 == 0) ? Theme.BG_PANEL : new Color(15, 20, 32));
                }

                if (col == 0) { // Rank
                    setFont(Theme.FONT_BODY_BOLD);
                    if (row == 0) setForeground(new Color(255, 215, 0));      // Gold #1
                    else if (row == 1) setForeground(new Color(192, 192, 192)); // Silver #2
                    else if (row == 2) setForeground(new Color(205, 127, 50));  // Bronze #3
                    else setForeground(Theme.TEXT_SECONDARY);
                } else if (col == 3) { // Wins
                    setForeground(Theme.ACCENT_GREEN);
                    setFont(Theme.FONT_BODY_BOLD);
                } else if (col == 4) { // Losses
                    setForeground(Theme.ACCENT_NEON_PINK);
                    setFont(Theme.FONT_BODY);
                } else if (col == 6) { // Win Rate
                    setForeground(Theme.ACCENT_CYAN);
                    setFont(Theme.FONT_BODY_BOLD);
                } else {
                    setForeground(isSelected ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);
                    setFont(Theme.FONT_BODY);
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(Theme.BG_PANEL);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER_COLOR, 1));
        scrollPane.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        // Bottom Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomBar.setOpaque(false);

        ModernComponents.ModernButton backBtn = new ModernComponents.ModernButton("← BACK TO DASHBOARD", Theme.BG_CARD, Theme.BORDER_ACTIVE);
        backBtn.setFont(Theme.FONT_BODY_BOLD);
        backBtn.setPreferredSize(new Dimension(180, 42));
        backBtn.addActionListener(e -> viewManager.showHome());
        bottomBar.add(backBtn);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    public void requestData() {
        tableModel.setRowCount(0);
        client.requestLeaderboard();
    }

    // --- ClientListener Callbacks ---

    @Override
    public void onLeaderboardReceived(List<PlayerStats> leaderboard) {
        tableModel.setRowCount(0);
        int rank = 1;
        for (PlayerStats p : leaderboard) {
            String rankStr = (rank == 1) ? "🥇 #1" : (rank == 2) ? "🥈 #2" : (rank == 3) ? "🥉 #3" : "#" + rank;
            tableModel.addRow(new Object[]{
                    rankStr,
                    p.getUsername(),
                    p.getTotalMatches(),
                    p.getWins(),
                    p.getLosses(),
                    p.getDraws(),
                    p.getFormattedWinRate()
            });
            rank++;
        }
        countLabel.setText("Total Registered Competitors: " + leaderboard.size());
    }

    @Override public void onConnected() {}
    @Override public void onConnectionFailed(String r) {}
    @Override public void onDisconnected(String r) { viewManager.showLogin(); }
    @Override public void onLoginSuccess(String u) {}
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
    @Override public void onHistoryReceived(List<MatchRecord> h) {}
}
