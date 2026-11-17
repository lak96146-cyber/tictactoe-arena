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
 * Screen G: Match History Screen.
 * Displays completed match logs from persistent storage.
 */
public class MatchHistoryFrame extends JFrame implements ClientListener {
    private final GameClient client;
    private final ViewManager viewManager;

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel countLabel;

    public MatchHistoryFrame(GameClient client, ViewManager viewManager) {
        this.client = client;
        this.viewManager = viewManager;
        this.client.addListener(this);

        initUI();
    }

    private void initUI() {
        setTitle("Tic-Tac-Toe Arena — Match History");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(840, 600);
        setMinimumSize(new Dimension(680, 480));
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
        JLabel titleLabel = new JLabel("BATTLE ARCHIVES");
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.TEXT_PRIMARY);

        countLabel = new JLabel("Historical record of all authenticated arena matches");
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
        String[] columnNames = {"MATCH ID", "FORMAT", "HOST (X)", "GUEST (O)", "WINNER", "SCORE", "DATE & TIME", "DURATION"};
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
                setHorizontalAlignment(SwingConstants.CENTER);

                if (!isSelected) {
                    c.setBackground((row % 2 == 0) ? Theme.BG_PANEL : new Color(15, 20, 32));
                }

                if (col == 0) { // Match ID
                    setFont(new Font("Consolas", Font.BOLD, 12));
                    setForeground(Theme.ACCENT_PURPLE);
                } else if (col == 4) { // Winner
                    setFont(Theme.FONT_BODY_BOLD);
                    String win = String.valueOf(val);
                    if ("DRAW".equalsIgnoreCase(win)) {
                        setForeground(Theme.ACCENT_AMBER);
                    } else {
                        setForeground(Theme.ACCENT_GREEN);
                    }
                } else if (col == 5) { // Score
                    setFont(Theme.FONT_BODY_BOLD);
                    setForeground(Theme.TEXT_PRIMARY);
                } else {
                    setFont(Theme.FONT_BODY);
                    setForeground(isSelected ? Theme.ACCENT_CYAN : Theme.TEXT_SECONDARY);
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
        client.requestHistory();
    }

    // --- ClientListener Callbacks ---

    @Override
    public void onHistoryReceived(List<MatchRecord> history) {
        tableModel.setRowCount(0);
        for (MatchRecord m : history) {
            tableModel.addRow(new Object[]{
                    m.getMatchId(),
                    m.getMode(),
                    m.getPlayer1(),
                    m.getPlayer2(),
                    m.getWinner(),
                    m.getResult(),
                    m.getDateTime(),
                    m.getFormattedDuration()
            });
        }
        countLabel.setText("Total Battles Recorded: " + history.size());
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
    @Override public void onLeaderboardReceived(List<PlayerStats> l) {}
}
