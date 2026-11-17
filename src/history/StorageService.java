package history;

import java.util.List;

/**
 * Storage abstraction interface for persisting match records and player ratings.
 * Allows seamless switching between CSV file persistence, in-memory storage,
 * or future remote database adapters (PostgreSQL, Redis, DynamoDB).
 */
public interface StorageService {
    void saveMatch(MatchRecord record);
    List<MatchRecord> getAllMatches();
    List<MatchRecord> getMatchesForPlayer(String username);

    void recordMatchResult(String player1, String player2, String winner);
    List<PlayerStats> getLeaderboard();
    PlayerStats getOrCreatePlayer(String username);

    String getStorageType();
}
