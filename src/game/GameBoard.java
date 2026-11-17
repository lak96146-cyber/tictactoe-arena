package game;

import java.util.Arrays;

/**
 * Encapsulates the 3x3 Tic-Tac-Toe grid state and basic mutations.
 */
public class GameBoard {
    public static final int SIZE = 3;
    private final PlayerSymbol[][] grid;
    private int moveCount;

    public GameBoard() {
        grid = new PlayerSymbol[SIZE][SIZE];
        reset();
    }

    /**
     * Resets all cells to NONE and move counter to zero.
     */
    public synchronized void reset() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = PlayerSymbol.NONE;
            }
        }
        moveCount = 0;
    }

    /**
     * Checks if coordinates are valid within the 3x3 board.
     */
    public boolean isValidCoord(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    /**
     * Checks if a cell is currently empty.
     */
    public synchronized boolean isCellEmpty(int r, int c) {
        if (!isValidCoord(r, c)) return false;
        return grid[r][c] == PlayerSymbol.NONE;
    }

    /**
     * Gets the symbol at cell (r, c).
     */
    public synchronized PlayerSymbol getCell(int r, int c) {
        if (!isValidCoord(r, c)) return PlayerSymbol.NONE;
        return grid[r][c];
    }

    /**
     * Places a move on the board if valid and empty.
     * Returns true if successfully placed.
     */
    public synchronized boolean placeMove(int r, int c, PlayerSymbol symbol) {
        if (!isValidCoord(r, c) || symbol == null || symbol == PlayerSymbol.NONE) {
            return false;
        }
        if (grid[r][c] != PlayerSymbol.NONE) {
            return false;
        }
        grid[r][c] = symbol;
        moveCount++;
        return true;
    }

    /**
     * Returns total number of moves made in this round.
     */
    public synchronized int getMoveCount() {
        return moveCount;
    }

    /**
     * Returns true if all 9 cells have been filled.
     */
    public synchronized boolean isFull() {
        return moveCount >= SIZE * SIZE;
    }

    /**
     * Serializes current board state to a compact 9-character string, e.g. "XOX--O---".
     */
    public synchronized String serialize() {
        StringBuilder sb = new StringBuilder(9);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                PlayerSymbol s = grid[r][c];
                if (s == PlayerSymbol.X) sb.append('X');
                else if (s == PlayerSymbol.O) sb.append('O');
                else sb.append('-');
            }
        }
        return sb.toString();
    }

    /**
     * Populates board state from serialized 9-character string.
     */
    public synchronized void deserialize(String str) {
        reset();
        if (str == null || str.length() != 9) return;
        for (int i = 0; i < 9; i++) {
            char ch = str.charAt(i);
            int r = i / SIZE;
            int c = i % SIZE;
            if (ch == 'X') {
                grid[r][c] = PlayerSymbol.X;
                moveCount++;
            } else if (ch == 'O') {
                grid[r][c] = PlayerSymbol.O;
                moveCount++;
            }
        }
    }

    /**
     * Deep copy of the board.
     */
    public synchronized GameBoard copy() {
        GameBoard copy = new GameBoard();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                copy.grid[r][c] = this.grid[r][c];
            }
        }
        copy.moveCount = this.moveCount;
        return copy;
    }
}
