package server;

import history.*;
import protocol.MessageProtocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry managing rooms, active connections, quick matchmaking,
 * and data persistence on the server.
 */
public class ServerManager {
    private static final ServerManager INSTANCE = new ServerManager();

    private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final Set<ClientHandler> activeClients = Collections.synchronizedSet(new HashSet<>());
    private final StorageService storageService = StorageManager.getService();
    private final Random random = new Random();

    private ServerManager() {}

    public static ServerManager getInstance() {
        return INSTANCE;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public HistoryManager getHistoryManager() {
        if (storageService instanceof CsvStorageService) {
            return ((CsvStorageService) storageService).getHistoryManager();
        }
        return new HistoryManager();
    }

    public ScoreManager getScoreManager() {
        if (storageService instanceof CsvStorageService) {
            return ((CsvStorageService) storageService).getScoreManager();
        }
        return new ScoreManager();
    }

    public void registerClient(ClientHandler client) {
        activeClients.add(client);
        ServerLog.info("AUTH", "Player registered: '" + client.getUsername() +
                               "' (" + client.getRemoteAddress() + ") | Active online: " + activeClients.size());
    }

    public void unregisterClient(ClientHandler client) {
        activeClients.remove(client);
        ServerLog.info("DISCONNECT", "Player unregistered: '" + client.getUsername() +
                                     "' (" + client.getRemoteAddress() + ") | Remaining online: " + activeClients.size());
    }

    public int getOnlineCount() {
        return activeClients.size();
    }

    /**
     * Generates a unique 6-digit alphanumeric room code.
     */
    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Creates a new private or public game room.
     */
    public synchronized GameRoom createRoom(ClientHandler host, String mode) {
        String code;
        int attempts = 0;
        do {
            code = generateRoomCode();
            attempts++;
        } while (rooms.containsKey(code) && attempts < 100);

        GameRoom room = new GameRoom(code, host, mode);
        rooms.put(code, room);
        ServerLog.info("ROOM", "Room created: '" + code + "' (Mode: " + mode + ") by host '" +
                              host.getUsername() + "' (" + host.getRemoteAddress() + ")");
        return room;
    }

    public GameRoom getRoom(String code) {
        if (code == null) return null;
        return rooms.get(code.trim().toUpperCase());
    }

    public synchronized void removeRoom(String code) {
        if (code == null) return;
        GameRoom removed = rooms.remove(code.trim().toUpperCase());
        if (removed != null) {
            ServerLog.info("ROOM", "Room closed & removed: '" + code + "'");
        }
    }

    /**
     * Finds an open room for quick match, or creates one if none available.
     */
    public synchronized GameRoom findQuickMatch(ClientHandler player) {
        for (GameRoom r : rooms.values()) {
            if (!r.isFull() && r.getStatus() == game.GameStatus.LOBBY_WAITING && r.getHost() != player) {
                if (r.addGuest(player)) {
                    ServerLog.info("MATCHMAKING", "QuickMatch paired: '" + player.getUsername() + "' joined room '" + r.getRoomCode() + "'");
                    return r;
                }
            }
        }
        // No open room found, create a standard BO3 room for player
        return createRoom(player, "BO3");
    }

    /**
     * Encodes leaderboard records for network transmission.
     */
    public String encodeLeaderboard() {
        List<PlayerStats> list = storageService.getLeaderboard();
        StringBuilder sb = new StringBuilder();
        sb.append(list.size());
        for (PlayerStats p : list) {
            sb.append("~")
              .append(p.getUsername()).append(";")
              .append(p.getTotalMatches()).append(";")
              .append(p.getWins()).append(";")
              .append(p.getLosses()).append(";")
              .append(p.getDraws()).append(";")
              .append(p.getFormattedWinRate());
        }
        return sb.toString();
    }

    /**
     * Encodes match history records for network transmission.
     */
    public String encodeHistory() {
        List<MatchRecord> list = storageService.getAllMatches();
        StringBuilder sb = new StringBuilder();
        sb.append(list.size());
        for (MatchRecord m : list) {
            sb.append("~")
              .append(m.getMatchId()).append(";")
              .append(m.getPlayer1()).append(";")
              .append(m.getPlayer2()).append(";")
              .append(m.getWinner()).append(";")
              .append(m.getResult()).append(";")
              .append(m.getDateTime()).append(";")
              .append(m.getFormattedDuration()).append(";")
              .append(m.getMode());
        }
        return sb.toString();
    }
}
