package history;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory storage implementation of StorageService.
 * Ideal for ephemeral cloud hosting (e.g. Render free tier restarts)
 * and environments where local disk writes are restricted.
 */
public class InMemoryStorageService implements StorageService {
    private final List<MatchRecord> matches = new CopyOnWriteArrayList<>();
    private final Map<String, PlayerStats> statsMap = new ConcurrentHashMap<>();

    @Override
    public void saveMatch(MatchRecord record) {
        if (record != null) {
            matches.add(record);
        }
    }

    @Override
    public List<MatchRecord> getAllMatches() {
        List<MatchRecord> copy = new ArrayList<>(matches);
        Collections.reverse(copy);
        return copy;
    }

    @Override
    public List<MatchRecord> getMatchesForPlayer(String username) {
        List<MatchRecord> result = new ArrayList<>();
        if (username == null) return result;
        for (MatchRecord m : getAllMatches()) {
            if (username.equalsIgnoreCase(m.getPlayer1()) || username.equalsIgnoreCase(m.getPlayer2())) {
                result.add(m);
            }
        }
        return result;
    }

    @Override
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
    }

    @Override
    public synchronized List<PlayerStats> getLeaderboard() {
        List<PlayerStats> list = new ArrayList<>(statsMap.values());
        Collections.sort(list);
        return list;
    }

    @Override
    public synchronized PlayerStats getOrCreatePlayer(String username) {
        if (username == null || username.trim().isEmpty()) {
            return new PlayerStats("Unknown");
        }
        String key = username.trim().toLowerCase();
        return statsMap.computeIfAbsent(key, k -> new PlayerStats(username.trim()));
    }

    @Override
    public String getStorageType() {
        return "In-Memory (Ephemeral)";
    }
}
