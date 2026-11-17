package protocol;

/**
 * Defines command tokens, delimiters, and message helpers for client-server communication.
 */
public class MessageProtocol {
    public static final String DELIMITER = "::";
    public static final int DEFAULT_PORT = 8888;

    // --- Client -> Server Command Tokens ---
    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_CREATE_ROOM = "CREATE_ROOM";
    public static final String CMD_JOIN_ROOM = "JOIN_ROOM";
    public static final String CMD_QUICK_MATCH = "QUICK_MATCH";
    public static final String CMD_TOGGLE_READY = "TOGGLE_READY";
    public static final String CMD_START_MATCH = "START_MATCH";
    public static final String CMD_MAKE_MOVE = "MAKE_MOVE";
    public static final String CMD_CHAT = "CHAT";
    public static final String CMD_REMATCH_REQUEST = "REMATCH_REQUEST";
    public static final String CMD_REMATCH_RESPONSE = "REMATCH_RESPONSE";
    public static final String CMD_LEAVE_ROOM = "LEAVE_ROOM";
    public static final String CMD_GET_LEADERBOARD = "GET_LEADERBOARD";
    public static final String CMD_GET_HISTORY = "GET_HISTORY";
    public static final String CMD_PING = "PING";

    // --- Server -> Client Command Tokens ---
    public static final String RES_LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String RES_LOGIN_FAIL = "LOGIN_FAIL";
    public static final String RES_ROOM_CREATED = "ROOM_CREATED";
    public static final String RES_ROOM_JOINED = "ROOM_JOINED";
    public static final String RES_JOIN_FAILED = "JOIN_FAILED";
    public static final String RES_PLAYER_JOINED = "PLAYER_JOINED";
    public static final String RES_PLAYER_READY_STATUS = "PLAYER_READY_STATUS";
    public static final String RES_GAME_START = "GAME_START";
    public static final String RES_MOVE_MADE = "MOVE_MADE";
    public static final String RES_MOVE_REJECTED = "MOVE_REJECTED";
    public static final String RES_ROUND_RESULT = "ROUND_RESULT";
    public static final String RES_NEXT_ROUND = "NEXT_ROUND";
    public static final String RES_MATCH_OVER = "MATCH_OVER";
    public static final String RES_CHAT = "CHAT";
    public static final String RES_REMATCH_REQUESTED = "REMATCH_REQUESTED";
    public static final String RES_REMATCH_STATUS = "REMATCH_STATUS";
    public static final String RES_OPPONENT_LEFT = "OPPONENT_LEFT";
    public static final String RES_LEADERBOARD_DATA = "LEADERBOARD_DATA";
    public static final String RES_HISTORY_DATA = "HISTORY_DATA";
    public static final String RES_ERROR = "ERROR";
    public static final String RES_PONG = "PONG";

    /**
     * Builds a protocol line joined by DELIMITER.
     */
    public static String buildMessage(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(DELIMITER);
            sb.append(parts[i] != null ? parts[i] : "");
        }
        return sb.toString();
    }

    /**
     * Splits a raw line into tokens using DELIMITER.
     */
    public static String[] parseMessage(String raw) {
        if (raw == null) return new String[0];
        return raw.split(java.util.regex.Pattern.quote(DELIMITER), -1);
    }
}
