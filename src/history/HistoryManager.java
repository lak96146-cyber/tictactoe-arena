package history;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages persistent storage and retrieval of match history in CSV format.
 */
public class HistoryManager {
    private static final String CSV_HEADER = "MatchId,Player1,Player2,Winner,Result,DateTime,DurationSeconds,Mode";

    private final File historyFile;

    public HistoryManager() {
        this(resolveDefaultPath("match_history.csv"));
    }

    public HistoryManager(String filePath) {
        this.historyFile = new File(filePath);
        initFile();
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
        return historyFile;
    }

    private synchronized void initFile() {
        try {
            File parent = historyFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!historyFile.exists()) {
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                        new FileOutputStream(historyFile), StandardCharsets.UTF_8))) {
                    writer.println(CSV_HEADER);
                }
            }
        } catch (IOException e) {
            System.err.println("[HistoryManager] Error initializing history file: " + e.getMessage());
        }
    }

    /**
     * Appends a match record to the CSV file.
     */
    public synchronized void saveMatch(MatchRecord record) {
        if (record == null) return;
        initFile();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(historyFile, true), StandardCharsets.UTF_8))) {
            writer.println(record.toCsvLine());
        } catch (IOException e) {
            System.err.println("[HistoryManager] Error saving match record: " + e.getMessage());
        }
    }

    /**
     * Loads all match records from the CSV file in reverse chronological order (newest first).
     */
    public synchronized List<MatchRecord> getAllMatches() {
        List<MatchRecord> list = new ArrayList<>();
        if (!historyFile.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(historyFile), StandardCharsets.UTF_8))) {
            String line = reader.readLine(); // Header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                MatchRecord record = MatchRecord.fromCsvLine(line);
                if (record != null) {
                    list.add(record);
                }
            }
        } catch (IOException e) {
            System.err.println("[HistoryManager] Error reading match history: " + e.getMessage());
        }

        // Newest matches first
        Collections.reverse(list);
        return list;
    }

    /**
     * Filters matches for a specific player username.
     */
    public synchronized List<MatchRecord> getMatchesForPlayer(String username) {
        List<MatchRecord> all = getAllMatches();
        if (username == null || username.trim().isEmpty()) return all;
        List<MatchRecord> filtered = new ArrayList<>();
        for (MatchRecord r : all) {
            if (username.equalsIgnoreCase(r.getPlayer1()) || username.equalsIgnoreCase(r.getPlayer2())) {
                filtered.add(r);
            }
        }
        return filtered;
    }
}
