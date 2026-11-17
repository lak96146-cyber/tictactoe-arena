package history;

import java.io.Serializable;

/**
 * Represents a completed match record stored in history.
 */
public class MatchRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String matchId;
    private final String player1;
    private final String player2;
    private final String winner;
    private final String result;
    private final String dateTime;
    private final int durationSeconds;
    private final String mode;

    public MatchRecord(String matchId, String player1, String player2, String winner,
                       String result, String dateTime, int durationSeconds, String mode) {
        this.matchId = matchId;
        this.player1 = player1;
        this.player2 = player2;
        this.winner = winner;
        this.result = result;
        this.dateTime = dateTime;
        this.durationSeconds = durationSeconds;
        this.mode = mode;
    }

    public String getMatchId() { return matchId; }
    public String getPlayer1() { return player1; }
    public String getPlayer2() { return player2; }
    public String getWinner() { return winner; }
    public String getResult() { return result; }
    public String getDateTime() { return dateTime; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getMode() { return mode; }

    public String getFormattedDuration() {
        int m = durationSeconds / 60;
        int s = durationSeconds % 60;
        if (m > 0) {
            return String.format("%dm %02ds", m, s);
        } else {
            return String.format("%ds", s);
        }
    }

    public String toCsvLine() {
        return escape(matchId) + "," +
               escape(player1) + "," +
               escape(player2) + "," +
               escape(winner) + "," +
               escape(result) + "," +
               escape(dateTime) + "," +
               durationSeconds + "," +
               escape(mode);
    }

    public static MatchRecord fromCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;
        String[] parts = line.split(",", -1);
        if (parts.length < 8) return null;
        try {
            return new MatchRecord(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                parts[4].trim(),
                parts[5].trim(),
                Integer.parseInt(parts[6].trim()),
                parts[7].trim()
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace(",", ";").replace("\n", " ").trim();
    }
}
