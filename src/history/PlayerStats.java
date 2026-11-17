package history;

import java.io.Serializable;

/**
 * Tracks competitive statistics for a player.
 */
public class PlayerStats implements Serializable, Comparable<PlayerStats> {
    private static final long serialVersionUID = 1L;

    private final String username;
    private int totalMatches;
    private int wins;
    private int losses;
    private int draws;

    public PlayerStats(String username) {
        this(username, 0, 0, 0, 0);
    }

    public PlayerStats(String username, int totalMatches, int wins, int losses, int draws) {
        this.username = username;
        this.totalMatches = totalMatches;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
    }

    public String getUsername() { return username; }
    public int getTotalMatches() { return totalMatches; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getDraws() { return draws; }

    public double getWinRate() {
        if (totalMatches <= 0) return 0.0;
        return (wins * 100.0) / totalMatches;
    }

    public String getFormattedWinRate() {
        return String.format("%.1f%%", getWinRate());
    }

    public synchronized void recordWin() {
        totalMatches++;
        wins++;
    }

    public synchronized void recordLoss() {
        totalMatches++;
        losses++;
    }

    public synchronized void recordDraw() {
        totalMatches++;
        draws++;
    }

    public String toCsvLine() {
        return escape(username) + "," + totalMatches + "," + wins + "," + losses + "," + draws;
    }

    public static PlayerStats fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = line.split(",", -1);
        if (parts.length < 5) return null;
        try {
            return new PlayerStats(
                parts[0].trim(),
                Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim()),
                Integer.parseInt(parts[3].trim()),
                Integer.parseInt(parts[4].trim())
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace(",", ";").replace("\n", " ").trim();
    }

    @Override
    public int compareTo(PlayerStats other) {
        // Sort descending by wins
        if (this.wins != other.wins) {
            return Integer.compare(other.wins, this.wins);
        }
        // Then by win rate
        int winRateComp = Double.compare(other.getWinRate(), this.getWinRate());
        if (winRateComp != 0) return winRateComp;
        // Then by total matches
        return Integer.compare(other.totalMatches, this.totalMatches);
    }
}
