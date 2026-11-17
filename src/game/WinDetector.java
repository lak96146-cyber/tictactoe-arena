package game;

/**
 * Authoritative detector for win and draw conditions in Tic-Tac-Toe.
 * Evaluates all 8 possible winning combinations:
 * - 3 Horizontal rows
 * - 3 Vertical columns
 * - 2 Diagonals
 */
public class WinDetector {

    public static class WinResult {
        public final boolean hasWinner;
        public final PlayerSymbol winner;
        public final boolean isDraw;
        public final WinningLine winningLine;

        public WinResult(boolean hasWinner, PlayerSymbol winner, boolean isDraw, WinningLine winningLine) {
            this.hasWinner = hasWinner;
            this.winner = winner;
            this.isDraw = isDraw;
            this.winningLine = winningLine;
        }

        public static WinResult none() {
            return new WinResult(false, PlayerSymbol.NONE, false, null);
        }

        public static WinResult win(PlayerSymbol winner, WinningLine line) {
            return new WinResult(true, winner, false, line);
        }

        public static WinResult draw() {
            return new WinResult(false, PlayerSymbol.NONE, true, null);
        }
    }

    /**
     * Checks the board for win or draw.
     */
    public static WinResult evaluate(GameBoard board) {
        if (board == null) return WinResult.none();

        // 1. Check Horizontal Rows
        for (int r = 0; r < 3; r++) {
            PlayerSymbol first = board.getCell(r, 0);
            if (first != PlayerSymbol.NONE &&
                first == board.getCell(r, 1) &&
                first == board.getCell(r, 2)) {
                return WinResult.win(first, new WinningLine(r, 0, r, 1, r, 2));
            }
        }

        // 2. Check Vertical Columns
        for (int c = 0; c < 3; c++) {
            PlayerSymbol first = board.getCell(0, c);
            if (first != PlayerSymbol.NONE &&
                first == board.getCell(1, c) &&
                first == board.getCell(2, c)) {
                return WinResult.win(first, new WinningLine(0, c, 1, c, 2, c));
            }
        }

        // 3. Check Main Diagonal (top-left to bottom-right)
        PlayerSymbol diag1 = board.getCell(0, 0);
        if (diag1 != PlayerSymbol.NONE &&
            diag1 == board.getCell(1, 1) &&
            diag1 == board.getCell(2, 2)) {
            return WinResult.win(diag1, new WinningLine(0, 0, 1, 1, 2, 2));
        }

        // 4. Check Anti-Diagonal (top-right to bottom-left)
        PlayerSymbol diag2 = board.getCell(0, 2);
        if (diag2 != PlayerSymbol.NONE &&
            diag2 == board.getCell(1, 1) &&
            diag2 == board.getCell(2, 0)) {
            return WinResult.win(diag2, new WinningLine(0, 2, 1, 1, 2, 0));
        }

        // 5. Check Draw (all cells full, no winner)
        if (board.isFull()) {
            return WinResult.draw();
        }

        // Game is still ongoing
        return WinResult.none();
    }
}
