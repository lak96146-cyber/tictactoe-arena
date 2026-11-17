package history;

import java.util.List;

/**
 * File-backed CSV implementation of StorageService.
 * Suitable for local development, persistent volumes, or self-hosted servers.
 */
public class CsvStorageService implements StorageService {
    private final HistoryManager historyManager;
    private final ScoreManager scoreManager;

    public CsvStorageService() {
        this.historyManager = new HistoryManager();
        this.scoreManager = new ScoreManager();
    }

    public CsvStorageService(String historyPath, String statsPath) {
        this.historyManager = new HistoryManager(historyPath);
        this.scoreManager = new ScoreManager(statsPath);
    }

    @Override
    public void saveMatch(MatchRecord record) {
        historyManager.saveMatch(record);
    }

    @Override
    public List<MatchRecord> getAllMatches() {
        return historyManager.getAllMatches();
    }

    @Override
    public List<MatchRecord> getMatchesForPlayer(String username) {
        return historyManager.getMatchesForPlayer(username);
    }

    @Override
    public void recordMatchResult(String player1, String player2, String winner) {
        scoreManager.recordMatchResult(player1, player2, winner);
    }

    @Override
    public List<PlayerStats> getLeaderboard() {
        return scoreManager.getLeaderboard();
    }

    @Override
    public PlayerStats getOrCreatePlayer(String username) {
        return scoreManager.getOrCreatePlayer(username);
    }

    @Override
    public String getStorageType() {
        return "CSV-File (" + historyManager.getFile().getPath() + ")";
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }
}
