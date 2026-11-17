package client;

import game.PlayerSymbol;
import game.WinningLine;
import history.MatchRecord;
import history.PlayerStats;

import java.util.List;

/**
 * Listener interface for asynchronous game and network events dispatched from the server.
 */
public interface ClientListener {
    void onConnected();
    void onConnectionFailed(String reason);
    void onDisconnected(String reason);

    void onLoginSuccess(String username);
    void onRoomCreated(String roomCode, String mode, String hostName);
    void onRoomJoined(String roomCode, String hostName, String guestName, String mode);
    void onJoinFailed(String reason);
    void onPlayerJoined(String roomCode, String guestName);
    void onReadyStatus(String roomCode, boolean hostReady, boolean guestReady);

    void onGameStart(String roomCode, String playerX, String playerO,
                     PlayerSymbol mySymbol, PlayerSymbol currentTurn,
                     String mode, int currentRound, int targetWins,
                     int scoreHost, int scoreGuest);

    void onMoveMade(String roomCode, int row, int col, PlayerSymbol symbol, PlayerSymbol nextTurn);
    void onMoveRejected(String reason);

    void onRoundResult(String roomCode, String winnerSymbol, WinningLine winningLine,
                       int scoreHost, int scoreGuest, boolean seriesOver);

    void onMatchOver(String roomCode, String winnerName, int scoreHost, int scoreGuest, int durationSec);

    void onChatReceived(String roomCode, String sender, String message);
    void onRematchRequested(String fromPlayer);
    void onRematchStatus(String status, String details);
    void onOpponentLeft(String reason);

    void onLeaderboardReceived(List<PlayerStats> leaderboard);
    void onHistoryReceived(List<MatchRecord> history);
}
