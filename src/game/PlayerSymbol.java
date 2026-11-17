package game;

/**
 * Represents the symbols used on the Tic-Tac-Toe board.
 */
public enum PlayerSymbol {
    X,
    O,
    NONE;

    /**
     * Returns the opposite symbol (X -> O, O -> X).
     */
    public PlayerSymbol opposite() {
        if (this == X) return O;
        if (this == O) return X;
        return NONE;
    }

    /**
     * Converts string representation to PlayerSymbol safely.
     */
    public static PlayerSymbol fromString(String str) {
        if (str == null) return NONE;
        try {
            return PlayerSymbol.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }

    @Override
    public String toString() {
        return this == NONE ? " " : name();
    }
}
