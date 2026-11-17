package game;

/**
 * Enumeration representing the possible states of a game room/session.
 */
public enum GameStatus {
    LOBBY_WAITING,   // Waiting for opponent to join
    LOBBY_READY,     // Both players present, waiting for readiness or start
    IN_PROGRESS,     // Game active, accepting moves
    ROUND_ENDED,     // Current round finished (win or draw), preparing next round or match end
    MATCH_FINISHED,  // Overall series won (BO1/BO3/BO5 completed)
    ABORTED          // Player disconnected or surrendered
}
