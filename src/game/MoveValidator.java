package game;

/**
 * Validates moves server-side before updating game state.
 * Rejects illegal moves with descriptive reason messages.
 */
public class MoveValidator {

    public static class ValidationResult {
        public final boolean isValid;
        public final String errorMessage;

        private ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }
    }

    /**
     * Validates an intended move.
     *
     * @param board The active game board
     * @param status The current game room status
     * @param currentTurn The symbol whose turn it currently is
     * @param playerSymbol The symbol of the player attempting the move
     * @param row Target row (0..2)
     * @param col Target col (0..2)
     * @return ValidationResult indicating success or reason for rejection
     */
    public static ValidationResult validate(GameBoard board,
                                           GameStatus status,
                                           PlayerSymbol currentTurn,
                                           PlayerSymbol playerSymbol,
                                           int row,
                                           int col) {
        if (board == null) {
            return ValidationResult.invalid("Game board is not initialized.");
        }

        if (status != GameStatus.IN_PROGRESS) {
            if (status == GameStatus.LOBBY_WAITING || status == GameStatus.LOBBY_READY) {
                return ValidationResult.invalid("The match has not started yet.");
            } else if (status == GameStatus.ROUND_ENDED) {
                return ValidationResult.invalid("Round has concluded. Waiting for next round.");
            } else if (status == GameStatus.MATCH_FINISHED) {
                return ValidationResult.invalid("Match has already finished.");
            } else {
                return ValidationResult.invalid("Game is not currently active.");
            }
        }

        if (playerSymbol == null || playerSymbol == PlayerSymbol.NONE) {
            return ValidationResult.invalid("Unassigned player symbol.");
        }

        if (playerSymbol != currentTurn) {
            return ValidationResult.invalid("It is not your turn! Waiting for opponent.");
        }

        if (!board.isValidCoord(row, col)) {
            return ValidationResult.invalid("Cell coordinates (" + row + ", " + col + ") are out of bounds.");
        }

        if (!board.isCellEmpty(row, col)) {
            return ValidationResult.invalid("Cell (" + row + ", " + col + ") is already occupied by " + board.getCell(row, col) + ".");
        }

        return ValidationResult.valid();
    }
}
