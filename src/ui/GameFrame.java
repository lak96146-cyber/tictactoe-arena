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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Screen D: Main Game Screen.
 * Contains responsive 3x3 BoardCanvas, Player 1 & 2 cards with active turn glow,
 * series score indicators, live match timer, in-game chat, and exit actions.
 */
public class GameFrame extends JFrame implements ClientListener {
    private final GameClient client;
    private final ViewManager viewManager;

    private BoardCanvas boardCanvas;
    private JLabel matchTimerLabel;
    private JLabel turnStatusLabel;
    private JLabel roundInfoLabel;
    private JLabel roomCodeBadge;

    // Player Cards
    private ModernComponents.ModernPanel playerXCard;
    private ModernComponents.ModernPanel playerOCard;
    private JLabel pXNameLabel;
    private JLabel pONameLabel;
    private JLabel pXScoreLabel;
    private JLabel pOScoreLabel;

    // In-game Chat
    private JTextArea chatArea;
    private ModernComponents.ModernTextField chatInput;

    // Timers
    private Timer matchTimer;
    private int matchSeconds = 0;

    public GameFrame(GameClient client, ViewManager viewManager) {
        this.client = client;
        this.viewManager = viewManager;
        this.client.addListener(this);

        initUI();
        initTimers();
    }

    private void initUI() {
        setTitle("Tic-Tac-Toe Arena — Live Match");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmExit();
            }
        });

        setSize(960, 700);
        setMinimumSize(new Dimension(840, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);

        JPanel mainLayout = new JPanel(new BorderLayout(16, 0));
        mainLayout.setOpaque(false);
        mainLayout.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Header: Match Info, Timer, Round, Room Code, Surrender
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftHeader.setOpaque(false);
        roomCodeBadge = new ModernComponents.BadgeLabel("ROOM: ------", Theme.BG_PANEL, Theme.ACCENT_CYAN);
        roundInfoLabel = new ModernComponents.BadgeLabel("ROUND 1", Theme.BG_PANEL, Theme.ACCENT_PURPLE);
        leftHeader.add(roomCodeBadge);
        leftHeader.add(roundInfoLabel);

        JPanel centerHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        centerHeader.setOpaque(false);
        matchTimerLabel = new JLabel("⏱ 00:00");
        matchTimerLabel.setFont(Theme.FONT_SUBTITLE);
        matchTimerLabel.setForeground(Theme.TEXT_PRIMARY);
        centerHeader.add(matchTimerLabel);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);
        ModernComponents.ModernButton exitBtn = new ModernComponents.ModernButton("SURRENDER", Theme.BG_CARD, Theme.ACCENT_NEON_PINK.darker());
        exitBtn.setFont(Theme.FONT_SMALL_BOLD);
        exitBtn.setPreferredSize(new Dimension(110, 32));
        exitBtn.addActionListener(e -> confirmExit());
        rightHeader.add(exitBtn);

        topHeader.add(leftHeader, BorderLayout.WEST);
        topHeader.add(centerHeader, BorderLayout.CENTER);
        topHeader.add(rightHeader, BorderLayout.EAST);

        // Center Game Area (Board Canvas + Turn Banner)
        JPanel boardArea = new JPanel(new BorderLayout(0, 10));
        boardArea.setOpaque(false);

        // Turn Status Banner
        turnStatusLabel = new JLabel("MATCH IN PROGRESS", SwingConstants.CENTER);
        turnStatusLabel.setFont(Theme.FONT_TITLE);
        turnStatusLabel.setForeground(Theme.ACCENT_CYAN);
        turnStatusLabel.setBorder(new EmptyBorder(6, 0, 6, 0));

        boardCanvas = new BoardCanvas();
        boardCanvas.setCellClickListener((row, col) -> {
            if (client.isMyTurn()) {
                client.sendMove(row, col);
            }
        });

        boardArea.add(turnStatusLabel, BorderLayout.NORTH);
        boardArea.add(boardCanvas, BorderLayout.CENTER);

        // Left Panel: Player Cards & Series Score
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(230, 0));

        // Player X Card
        playerXCard = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        playerXCard.setLayout(new BorderLayout(8, 6));
        playerXCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel iconX = new JLabel("X");
        iconX.setFont(Theme.FONT_DISPLAY);
        iconX.setForeground(Theme.ACCENT_NEON_PINK);

        JPanel infoX = new JPanel(new GridLayout(2, 1, 0, 2));
        infoX.setOpaque(false);
        pXNameLabel = new JLabel("Player 1");
        pXNameLabel.setFont(Theme.FONT_BODY_BOLD);
        pXNameLabel.setForeground(Theme.TEXT_PRIMARY);

        pXScoreLabel = new JLabel("Score: 0 wins");
        pXScoreLabel.setFont(Theme.FONT_SMALL);
        pXScoreLabel.setForeground(Theme.TEXT_SECONDARY);
        infoX.add(pXNameLabel);
        infoX.add(pXScoreLabel);

        playerXCard.add(iconX, BorderLayout.WEST);
        playerXCard.add(infoX, BorderLayout.CENTER);

        // Player O Card
        playerOCard = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        playerOCard.setLayout(new BorderLayout(8, 6));
        playerOCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel iconO = new JLabel("O");
        iconO.setFont(Theme.FONT_DISPLAY);
        iconO.setForeground(Theme.ACCENT_CYAN);

        JPanel infoO = new JPanel(new GridLayout(2, 1, 0, 2));
        infoO.setOpaque(false);
        pONameLabel = new JLabel("Player 2");
        pONameLabel.setFont(Theme.FONT_BODY_BOLD);
        pONameLabel.setForeground(Theme.TEXT_PRIMARY);

        pOScoreLabel = new JLabel("Score: 0 wins");
        pOScoreLabel.setFont(Theme.FONT_SMALL);
        pOScoreLabel.setForeground(Theme.TEXT_SECONDARY);
        infoO.add(pONameLabel);
        infoO.add(pOScoreLabel);

        playerOCard.add(iconO, BorderLayout.WEST);
        playerOCard.add(infoO, BorderLayout.CENTER);

        // Series mode reminder card
        ModernComponents.ModernPanel formatCard = new ModernComponents.ModernPanel(14, Theme.BG_PANEL, Theme.BORDER_COLOR);
        formatCard.setLayout(new BoxLayout(formatCard, BoxLayout.Y_AXIS));
        formatCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel formatTitle = new JLabel("SERIES RULES");
        formatTitle.setFont(Theme.FONT_SMALL_BOLD);
        formatTitle.setForeground(Theme.ACCENT_PURPLE);

        JLabel formatDesc = new JLabel("<html>First combatant to win " + client.getTargetWins() +
                " rounds claims arena victory. Move validation is authoritative.</html>");
        formatDesc.setFont(Theme.FONT_SMALL);
        formatDesc.setForeground(Theme.TEXT_SECONDARY);

        formatCard.add(formatTitle);
        formatCard.add(Box.createVerticalStrut(4));
        formatCard.add(formatDesc);

        leftPanel.add(playerXCard);
        leftPanel.add(Box.createVerticalStrut(14));
        leftPanel.add(playerOCard);
        leftPanel.add(Box.createVerticalStrut(14));
        leftPanel.add(formatCard);
        leftPanel.add(Box.createVerticalGlue());

        // Right Panel: In-Game Chat
        ModernComponents.ModernPanel chatPanel = new ModernComponents.ModernPanel(16, Theme.BG_PANEL, Theme.BORDER_COLOR);
        chatPanel.setLayout(new BorderLayout(0, 10));
        chatPanel.setBorder(new EmptyBorder(14, 14, 14, 14));
        chatPanel.setPreferredSize(new Dimension(240, 0));

        JLabel chatTitle = new JLabel("ARENA COMMS");
        chatTitle.setFont(Theme.FONT_SMALL_BOLD);
        chatTitle.setForeground(Theme.ACCENT_CYAN);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(Theme.BG_INPUT);
        chatArea.setForeground(Theme.TEXT_PRIMARY);
        chatArea.setFont(Theme.FONT_SMALL);
        chatArea.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);
        chatScroll.setBorder(null);
        chatScroll.getVerticalScrollBar().setUI(new ModernComponents.ModernScrollBarUI());

        JPanel chatInputRow = new JPanel(new BorderLayout(6, 0));
        chatInputRow.setOpaque(false);

        chatInput = new ModernComponents.ModernTextField("Type message...");
        chatInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendChatMessage();
                }
            }
        });

        ModernComponents.ModernButton sendBtn = new ModernComponents.ModernButton("SEND", Theme.BG_CARD, Theme.BORDER_ACTIVE);
        sendBtn.setFont(Theme.FONT_SMALL_BOLD);
        sendBtn.setPreferredSize(new Dimension(60, 36));
        sendBtn.addActionListener(e -> sendChatMessage());

        chatInputRow.add(chatInput, BorderLayout.CENTER);
        chatInputRow.add(sendBtn, BorderLayout.EAST);

        chatPanel.add(chatTitle, BorderLayout.NORTH);
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(chatInputRow, BorderLayout.SOUTH);

        mainLayout.add(topHeader, BorderLayout.NORTH);
        mainLayout.add(leftPanel, BorderLayout.WEST);
        mainLayout.add(boardArea, BorderLayout.CENTER);
        mainLayout.add(chatPanel, BorderLayout.EAST);

        setContentPane(mainLayout);
    }

    private void initTimers() {
        matchTimer = new Timer(1000, e -> {
            matchSeconds++;
            int m = matchSeconds / 60;
            int s = matchSeconds % 60;
            matchTimerLabel.setText(String.format("⏱ %02d:%02d", m, s));
        });
    }

    public void refresh() {
        matchSeconds = 0;
        matchTimer.restart();

        roomCodeBadge.setText("ROOM: " + client.getCurrentRoomCode());
        roundInfoLabel.setText("ROUND " + client.getCurrentRound() + " (" + client.getMatchMode() + ")");

        if (client.isHost()) {
            pXNameLabel.setText(client.getUsername() + " (YOU)");
            pONameLabel.setText(client.getOpponentName());
        } else {
            pXNameLabel.setText(client.getOpponentName());
            pONameLabel.setText(client.getUsername() + " (YOU)");
        }

        updateScoreLabels();
        updateTurnDisplay();
        boardCanvas.updateState(client.getLocalBoard(), client.getMySymbol(), client.isMyTurn(), client.getWinningLine());
    }

    private void updateScoreLabels() {
        pXScoreLabel.setText("Score: " + client.getScoreHost() + " / " + client.getTargetWins() + " wins");
        pOScoreLabel.setText("Score: " + client.getScoreGuest() + " / " + client.getTargetWins() + " wins");
    }

    private void updateTurnDisplay() {
        boolean isMyTurn = client.isMyTurn();
        PlayerSymbol cur = client.getCurrentTurn();

        if (isMyTurn) {
            turnStatusLabel.setText("⚡ YOUR TURN (" + client.getMySymbol() + ")");
            turnStatusLabel.setForeground(Theme.ACCENT_GREEN);
        } else {
            turnStatusLabel.setText("⏳ OPPONENT'S TURN (" + cur + ")");
            turnStatusLabel.setForeground(Theme.TEXT_SECONDARY);
        }

        // Highlight active player card border
        if (cur == PlayerSymbol.X) {
            playerXCard.setBorderColor(Theme.ACCENT_NEON_PINK);
            playerOCard.setBorderColor(Theme.BORDER_COLOR);
        } else if (cur == PlayerSymbol.O) {
            playerOCard.setBorderColor(Theme.ACCENT_CYAN);
            playerXCard.setBorderColor(Theme.BORDER_COLOR);
        } else {
            playerXCard.setBorderColor(Theme.BORDER_COLOR);
            playerOCard.setBorderColor(Theme.BORDER_COLOR);
        }

        boardCanvas.updateState(client.getLocalBoard(), client.getMySymbol(), isMyTurn, client.getWinningLine());
    }

    private void sendChatMessage() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            client.sendChat(msg);
            chatInput.setText("");
        }
    }

    private void confirmExit() {
        int res = JOptionPane.showConfirmDialog(
                this,
                "Surrendering will forfeit the match and count as a loss. Are you sure?",
                "Surrender Match",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (res == JOptionPane.YES_OPTION) {
            matchTimer.stop();
            client.leaveRoom();
            viewManager.showHome();
        }
    }

    // --- ClientListener Callbacks ---

    @Override
    public void onMoveMade(String roomCode, int row, int col, PlayerSymbol symbol, PlayerSymbol nextTurn) {
        updateTurnDisplay();
    }

    @Override
    public void onMoveRejected(String reason) {
        SoundManager.getInstance().playError();
        turnStatusLabel.setText("⚠ " + reason);
        turnStatusLabel.setForeground(Theme.ACCENT_NEON_PINK);
    }

    @Override
    public void onRoundResult(String roomCode, String winnerSymbol, WinningLine line, int sHost, int sGuest, boolean seriesOver) {
        boardCanvas.setWinningLine(line);
        updateScoreLabels();

        if ("DRAW".equalsIgnoreCase(winnerSymbol)) {
            SoundManager.getInstance().playDraw();
            turnStatusLabel.setText("STALEMATE — ROUND DRAW!");
            turnStatusLabel.setForeground(Theme.ACCENT_AMBER);
        } else {
            boolean iWonRound = winnerSymbol.equalsIgnoreCase(client.getMySymbol().name());
            if (iWonRound) {
                SoundManager.getInstance().playWin();
                turnStatusLabel.setText("VICTORY — ROUND SECURED!");
                turnStatusLabel.setForeground(Theme.ACCENT_GREEN);
            } else {
                SoundManager.getInstance().playError();
                turnStatusLabel.setText("ROUND CONCEDED TO " + winnerSymbol);
                turnStatusLabel.setForeground(Theme.ACCENT_NEON_PINK);
            }
        }
    }

    @Override
    public void onGameStart(String roomCode, String pX, String pO, PlayerSymbol mySym,
                            PlayerSymbol curTurn, String mode, int curRound, int targetWins,
                            int sHost, int sGuest) {
        roundInfoLabel.setText("ROUND " + curRound + " (" + mode + ")");
        boardCanvas.resetBoard();
        updateScoreLabels();
        updateTurnDisplay();
    }

    @Override
    public void onMatchOver(String roomCode, String winnerName, int sHost, int sGuest, int durationSec) {
        matchTimer.stop();
        boolean iWon = winnerName.equalsIgnoreCase(client.getUsername());
        if (iWon) {
            SoundManager.getInstance().playWin();
        } else {
            SoundManager.getInstance().playError();
        }
        viewManager.showResult(winnerName, sHost, sGuest, durationSec);
    }

    @Override
    public void onChatReceived(String roomCode, String sender, String message) {
        String tag = sender.equalsIgnoreCase(client.getUsername()) ? "YOU" : sender;
        chatArea.append("[" + tag + "]: " + message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    @Override
    public void onOpponentLeft(String reason) {
        matchTimer.stop();
        JOptionPane.showMessageDialog(this, reason, "Opponent Disconnected", JOptionPane.INFORMATION_MESSAGE);
        viewManager.showHome();
    }

    @Override public void onConnected() {}
    @Override public void onConnectionFailed(String r) {}
    @Override public void onDisconnected(String r) {
        matchTimer.stop();
        viewManager.showLogin();
    }
    @Override public void onLoginSuccess(String u) {}
    @Override public void onRoomCreated(String c, String m, String h) {}
    @Override public void onRoomJoined(String c, String h, String g, String m) {}
    @Override public void onJoinFailed(String r) {}
    @Override public void onPlayerJoined(String c, String g) {}
    @Override public void onReadyStatus(String c, boolean hr, boolean gr) {}
    @Override public void onRematchRequested(String f) {}
    @Override public void onRematchStatus(String s, String d) {}
    @Override public void onLeaderboardReceived(List<PlayerStats> l) {}
    @Override public void onHistoryReceived(List<MatchRecord> h) {}
}
