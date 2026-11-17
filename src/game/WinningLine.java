package game;

import java.io.Serializable;

/**
 * Stores the coordinates of the three cells that formed a winning line.
 */
public class WinningLine implements Serializable {
    private static final long serialVersionUID = 1L;

    public final int r1, c1;
    public final int r2, c2;
    public final int r3, c3;

    public WinningLine(int r1, int c1, int r2, int c2, int r3, int c3) {
        this.r1 = r1;
        this.c1 = c1;
        this.r2 = r2;
        this.c2 = c2;
        this.r3 = r3;
        this.c3 = c3;
    }

    /**
     * Checks if cell (r, c) is part of this winning line.
     */
    public boolean contains(int r, int c) {
        return (r == r1 && c == c1) ||
               (r == r2 && c == c2) ||
               (r == r3 && c == c3);
    }

    /**
     * Serializes to a compact string format: "r1,c1,r2,c2,r3,c3".
     */
    public String toCoordinateString() {
        return r1 + "," + c1 + "," + r2 + "," + c2 + "," + r3 + "," + c3;
    }

    /**
     * Parses from a compact string format.
     */
    public static WinningLine fromCoordinateString(String str) {
        if (str == null || str.trim().isEmpty() || str.equalsIgnoreCase("NONE")) {
            return null;
        }
        try {
            String[] parts = str.split(",");
            if (parts.length == 6) {
                return new WinningLine(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim()),
                    Integer.parseInt(parts[4].trim()),
                    Integer.parseInt(parts[5].trim())
                );
            }
        } catch (NumberFormatException e) {
            // Invalid formatting
        }
        return null;
    }

    @Override
    public String toString() {
        return "[" + r1 + "," + c1 + " -> " + r2 + "," + c2 + " -> " + r3 + "," + c3 + "]";
    }
}
