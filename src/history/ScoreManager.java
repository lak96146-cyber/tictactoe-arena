package history;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manages competitive player statistics and persistent leaderboard calculations.
 */
public class ScoreManager {
    private static final String CSV_HEADER = "Username,TotalMatches,Wins,Losses,Draws";

    private final File statsFile;
    private final Map<String, PlayerStats> statsMap = new HashMap<>();

    public ScoreManager() {
        this(resolveDefaultPath("player_statistics.csv"));
    }

    public ScoreManager(String filePath) {
        this.statsFile = new File(filePath);
        initFile();
        loadAll();
    }

    public static String resolveDefaultPath(String fileName) {
        String baseDir = System.getProperty("arena.data.dir");
        if (baseDir == null || baseDir.trim().isEmpty()) {
            baseDir = System.getenv("ARENA_DATA_DIR");
        }
        if (baseDir == null || baseDir.trim().isEmpty()) {
            baseDir = "data";
        }
        return new File(baseDir, fileName).getPath();
    }

    public File getFile() {
        return statsFile;
    }

    private synchronized void initFile() {
        try {
            File parent = statsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!statsFile.exists()) {
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(statsFile), StandardCharsets.UTF_8))) {
                    writer.println(CSV_HEADER);
                }
            }
        } catch (IOException e) {
            System.err.println("[ScoreManager] Error initializing stats file: " + e.getMessage());
        }
    }

    /**
     * Loads all player statistics into memory from the CSV file.
     */
    public synchronized void loadAll() {
        statsMap.clear();
        if (!statsFile.exists()) return;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(statsFile), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                PlayerStats stats = PlayerStats.fromCsvLine(line);
                if (stats != null) {
                    statsMap.put(stats.getUsername().toLowerCase(), stats);
                }
            }
        } catch (IOException e) {
            System.err.println("[ScoreManager] Error reading player stats: " + e.getMessage());
        }
    }

    /**
     * Flushes current memory state to the CSV file.
     */
    public synchronized void saveAll() {
        initFile();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(statsFile, false), StandardCharsets.UTF_8))) {
            writer.println(CSV_HEADER);
            for (PlayerStats stats : statsMap.values()) {
                writer.println(stats.toCsvLine());
            }
        } catch (IOException e) {
            System.err.println("[ScoreManager] Error saving player stats: " + e.getMessage());
        }
    }

    /**
     * Retrieves or creates a player's statistics profile.
     */
    public synchronized PlayerStats getOrCreatePlayer(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new PlayerStats("Unknown");
        }
        String key = username.trim().toLowerCase();
        PlayerStats stats = statsMap.get(key);
        if (stats == null) {
            stats = new PlayerStats(username.trim());
            statsMap.put(key, stats);
        }
        return stats;
    }

    /**
     * Updates statistics for both players upon match completion.
     */
    public synchronized void recordMatchResult(String player1, String player2, String winner) {
        if (player1 == null || player2 == null) return;
        PlayerStats p1 = getOrCreatePlayer(player1);
        PlayerStats p2 = getOrCreatePlayer(player2);

        if (winner == null || winner.equalsIgnoreCase("DRAW")) {
            p1.recordDraw();
            p2.recordDraw();
        } else if (winner.equalsIgnoreCase(player1)) {
            p1.recordWin();
            p2.recordLoss();
        } else if (winner.equalsIgnoreCase(player2)) {
            p2.recordWin();
            p1.recordLoss();
        }

        saveAll();
    }

    /**
     * Returns a sorted list of players for the leaderboard (highest wins first).
     */
    public synchronized List<PlayerStats> getLeaderboard() {
        List<PlayerStats> list = new ArrayList<>(statsMap.values());
        Collections.sort(list);
        return list;
    }
}
