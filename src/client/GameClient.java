package client;

import game.GameBoard;
import game.PlayerSymbol;
import game.WinningLine;
import history.MatchRecord;
import history.PlayerStats;
import protocol.MessageProtocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side game controller and state manager.
 * Exposes high-level actions for UI controllers and routes network events.
 */
public class GameClient implements ClientListener {
    private final NetworkHandler networkHandler = new NetworkHandler();
    private final List<ClientListener> listeners = new ArrayList<>();

    // Player state
    private String username = "Player";
    private String serverHost = "localhost";
    private int serverPort = MessageProtocol.DEFAULT_PORT;

    // Room / Match state
    private String currentRoomCode = null;
    private boolean isHost = false;
    private String opponentName = "Opponent";
    private PlayerSymbol mySymbol = PlayerSymbol.NONE;
    private PlayerSymbol currentTurn = PlayerSymbol.NONE;
    private final GameBoard localBoard = new GameBoard();
    private WinningLine winningLine = null;

    private String matchMode = "BO3";
    private int targetWins = 2;
    private int currentRound = 1;
    private int scoreHost = 0;
    private int scoreGuest = 0;

    public GameClient() {
        networkHandler.setListener(this);
    }

    public synchronized void addListener(ClientListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(ClientListener listener) {
        listeners.remove(listener);
    }

    // Getters
    public String getUsername() { return username; }
    public String getServerHost() { return serverHost; }
    public int getServerPort() { return serverPort; }
    public String getCurrentRoomCode() { return currentRoomCode; }
    public boolean isHost() { return isHost; }
    public String getOpponentName() { return opponentName; }
    public PlayerSymbol getMySymbol() { return mySymbol; }
    public PlayerSymbol getCurrentTurn() { return currentTurn; }
    public GameBoard getLocalBoard() { return localBoard; }
    public WinningLine getWinningLine() { return winningLine; }
    public String getMatchMode() { return matchMode; }
    public int getTargetWins() { return targetWins; }
    public int getCurrentRound() { return currentRound; }
    public int getScoreHost() { return scoreHost; }
    public int getScoreGuest() { return scoreGuest; }
    public boolean isConnected() { return networkHandler.isConnected(); }
    public boolean isMyTurn() { return mySymbol != PlayerSymbol.NONE && mySymbol == currentTurn; }

    // --- Outbound Actions ---

    public void connect(String host, int port, String user) {
        this.serverHost = host;
        this.serverPort = port;
        this.username = (user != null && !user.trim().isEmpty()) ? user.trim() : "Player";
        networkHandler.connect(host, port);
    }

    public void disconnect() {
        networkHandler.disconnect("User disconnected");
    }

    public void login() {
        networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_LOGIN, username));
    }

    public void createRoom(String mode) {
        this.matchMode = mode;
        this.isHost = true;
        networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_CREATE_ROOM, mode));
    }

    public void joinRoom(String code) {
        this.isHost = false;
        networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_JOIN_ROOM, code));
    }

    public void quickMatch() {
        networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_QUICK_MATCH));
    }

    public void toggleReady() {
        if (currentRoomCode != null) {
            networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_TOGGLE_READY, currentRoomCode));
        }
    }

    public void startMatch() {
        if (currentRoomCode != null) {
            networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_START_MATCH, currentRoomCode));
        }
    }

    public void sendMove(int row, int col) {
        if (currentRoomCode != null) {
            networkHandler.send(MessageProtocol.buildMessage(
                    MessageProtocol.CMD_MAKE_MOVE,
                    currentRoomCode,
                    String.valueOf(row),
                    String.valueOf(col)
            ));
        }
    }

    public void sendChat(String message) {
        if (currentRoomCode != null && message != null && !message.trim().isEmpty()) {
            networkHandler.send(MessageProtocol.buildMessage(
                    MessageProtocol.CMD_CHAT,
                    currentRoomCode,
                    message.trim()
            ));
        }
    }

    public void requestRematch() {
        if (currentRoomCode != null) {
            networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_REMATCH_REQUEST, currentRoomCode));
        }
    }

    public void respondRematch(boolean accept) {
        if (currentRoomCode != null) {
            networkHandler.send(MessageProtocol.buildMessage(
                    MessageProtocol.CMD_REMATCH_RESPONSE,
                    currentRoomCode,
                    accept ? "ACCEPT" : "DECLINE"
            ));
        }
    }

    public void leaveRoom() {
        if (currentRoomCode != null) {
            networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_LEAVE_ROOM, currentRoomCode));
            currentRoomCode = null;
        }
    }

    public void requestLeaderboard() {
        networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_GET_LEADERBOARD));
    }

    public void requestHistory() {
        networkHandler.send(MessageProtocol.buildMessage(MessageProtocol.CMD_GET_HISTORY));
    }

    // --- ClientListener Inbound Callback Multiplexing ---

    @Override
    public void onConnected() {
        login();
        for (ClientListener l : new ArrayList<>(listeners)) l.onConnected();
    }

    @Override
    public void onConnectionFailed(String reason) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onConnectionFailed(reason);
    }

    @Override
    public void onDisconnected(String reason) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onDisconnected(reason);
    }

    @Override
    public void onLoginSuccess(String user) {
        this.username = user;
        for (ClientListener l : new ArrayList<>(listeners)) l.onLoginSuccess(user);
    }

    @Override
    public void onRoomCreated(String roomCode, String mode, String hostName) {
        this.currentRoomCode = roomCode;
        this.matchMode = mode;
        this.isHost = true;
        this.mySymbol = PlayerSymbol.X;
        for (ClientListener l : new ArrayList<>(listeners)) l.onRoomCreated(roomCode, mode, hostName);
    }

    @Override
    public void onRoomJoined(String roomCode, String hostName, String guestName, String mode) {
        this.currentRoomCode = roomCode;
        this.matchMode = mode;
        this.isHost = false;
        this.opponentName = hostName;
        this.mySymbol = PlayerSymbol.O;
        for (ClientListener l : new ArrayList<>(listeners)) l.onRoomJoined(roomCode, hostName, guestName, mode);
    }

    @Override
    public void onJoinFailed(String reason) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onJoinFailed(reason);
    }

    @Override
    public void onPlayerJoined(String roomCode, String guestName) {
        this.opponentName = guestName;
        for (ClientListener l : new ArrayList<>(listeners)) l.onPlayerJoined(roomCode, guestName);
    }

    @Override
    public void onReadyStatus(String roomCode, boolean hostReady, boolean guestReady) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onReadyStatus(roomCode, hostReady, guestReady);
    }

    @Override
    public void onGameStart(String roomCode, String pX, String pO, PlayerSymbol mySym,
                            PlayerSymbol curTurn, String mode, int curRound, int targetWins,
                            int sHost, int sGuest) {
        this.currentRoomCode = roomCode;
        this.mySymbol = mySym;
        this.currentTurn = curTurn;
        this.matchMode = mode;
        this.currentRound = curRound;
        this.targetWins = targetWins;
        this.scoreHost = sHost;
        this.scoreGuest = sGuest;
        this.winningLine = null;
        this.localBoard.reset();
        this.opponentName = (mySym == PlayerSymbol.X) ? pO : pX;

        for (ClientListener l : new ArrayList<>(listeners)) {
            l.onGameStart(roomCode, pX, pO, mySym, curTurn, mode, curRound, targetWins, sHost, sGuest);
        }
    }

    @Override
    public void onMoveMade(String roomCode, int row, int col, PlayerSymbol symbol, PlayerSymbol nextTurn) {
        this.localBoard.placeMove(row, col, symbol);
        this.currentTurn = nextTurn;
        for (ClientListener l : new ArrayList<>(listeners)) {
            l.onMoveMade(roomCode, row, col, symbol, nextTurn);
        }
    }

    @Override
    public void onMoveRejected(String reason) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onMoveRejected(reason);
    }

    @Override
    public void onRoundResult(String roomCode, String winnerSymbol, WinningLine line, int sHost, int sGuest, boolean seriesOver) {
        this.winningLine = line;
        this.scoreHost = sHost;
        this.scoreGuest = sGuest;
        for (ClientListener l : new ArrayList<>(listeners)) {
            l.onRoundResult(roomCode, winnerSymbol, line, sHost, sGuest, seriesOver);
        }
    }

    @Override
    public void onMatchOver(String roomCode, String winnerName, int sHost, int sGuest, int durationSec) {
        this.scoreHost = sHost;
        this.scoreGuest = sGuest;
        for (ClientListener l : new ArrayList<>(listeners)) {
            l.onMatchOver(roomCode, winnerName, sHost, sGuest, durationSec);
        }
    }

    @Override
    public void onChatReceived(String roomCode, String sender, String message) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onChatReceived(roomCode, sender, message);
    }

    @Override
    public void onRematchRequested(String fromPlayer) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onRematchRequested(fromPlayer);
    }

    @Override
    public void onRematchStatus(String status, String details) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onRematchStatus(status, details);
    }

    @Override
    public void onOpponentLeft(String reason) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onOpponentLeft(reason);
    }

    @Override
    public void onLeaderboardReceived(List<PlayerStats> leaderboard) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onLeaderboardReceived(leaderboard);
    }

    @Override
    public void onHistoryReceived(List<MatchRecord> history) {
        for (ClientListener l : new ArrayList<>(listeners)) l.onHistoryReceived(history);
    }

    /**
     * Standalone entry point for launching the Game Client directly.
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient();
            ui.ViewManager viewManager = new ui.ViewManager(client);
            viewManager.showLogin();
        });
    }
}
