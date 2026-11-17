package server;

import protocol.MessageProtocol;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles communication with an individual connected client.
 * Supports:
 * 1. RFC 6455 WebSockets (for cloud hosting like Render, with WSS / reverse proxies)
 * 2. HTTP Health Checks (GET /health and GET / returning 200 OK for Render)
 * 3. Backward-compatible raw TCP socket messaging
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private InputStream rawIn;
    private OutputStream rawOut;

    private boolean isWebSocket = false;
    private String username = "Player";
    private String currentRoomCode = null;
    private volatile boolean running = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentRoomCode() {
        return currentRoomCode;
    }

    public void setCurrentRoomCode(String roomCode) {
        this.currentRoomCode = roomCode;
    }

    public boolean isConnected() {
        return running && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public String getRemoteAddress() {
        if (socket != null && socket.getRemoteSocketAddress() != null) {
            return socket.getRemoteSocketAddress().toString();
        }
        return "Unknown";
    }

    @Override
    public void run() {
        try {
            rawIn = socket.getInputStream();
            rawOut = socket.getOutputStream();

            // Read the initial line of the connection
            String initialLine = readAsciiLine(rawIn);
            if (initialLine == null || initialLine.trim().isEmpty()) {
                return;
            }

            initialLine = initialLine.trim();

            if (initialLine.startsWith("GET ")) {
                handleHttpOrWebSocketRequest(initialLine);
            } else {
                // Legacy raw TCP connection fallback
                isWebSocket = false;
                processCommand(initialLine);
                String line;
                while (running && (line = readAsciiLine(rawIn)) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        processCommand(line);
                    }
                }
            }
        } catch (IOException e) {
            // Socket disconnected
        } finally {
            cleanup();
        }
    }

    /**
     * Inspects HTTP request: responds to health checks or performs WebSocket handshake.
     */
    private void handleHttpOrWebSocketRequest(String requestLine) throws IOException {
        String[] parts = requestLine.split(" ");
        String path = (parts.length > 1) ? parts[1] : "/";

        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = readAsciiLine(rawIn)) != null && !headerLine.trim().isEmpty()) {
            int colon = headerLine.indexOf(':');
            if (colon > 0) {
                String key = headerLine.substring(0, colon).trim().toLowerCase();
                String value = headerLine.substring(colon + 1).trim();
                headers.put(key, value);
            }
        }

        String upgrade = headers.get("upgrade");
        boolean isWs = upgrade != null && upgrade.equalsIgnoreCase("websocket");

        if (isWs) {
            // Perform WebSocket Handshake
            String secKey = headers.get("sec-websocket-key");
            String acceptKey = WebSocketFrame.computeAcceptKey(secKey);

            String handshakeResponse = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

            rawOut.write(handshakeResponse.getBytes(StandardCharsets.UTF_8));
            rawOut.flush();

            this.isWebSocket = true;
            ServerLog.info("WS", "WebSocket upgrade successful with client " + getRemoteAddress() + " (Path: " + path + ")");

            // Enter WebSocket frame reading loop
            while (running) {
                WebSocketFrame.Frame frame = WebSocketFrame.readFrame(rawIn);
                if (frame.opcode == WebSocketFrame.OPCODE_TEXT) {
                    String message = frame.getAsText();
                    if (message != null && !message.trim().isEmpty()) {
                        processCommand(message.trim());
                    }
                } else if (frame.opcode == WebSocketFrame.OPCODE_PING) {
                    sendRawBytes(WebSocketFrame.encodePongFrame(frame.payload));
                } else if (frame.opcode == WebSocketFrame.OPCODE_CLOSE) {
                    sendRawBytes(WebSocketFrame.encodeCloseFrame());
                    break;
                }
            }
        } else {
            // Standard HTTP Request (e.g. Render Health Check: GET /health or GET /)
            handleHealthCheckRequest(path);
        }
    }

    /**
     * Responds with HTTP 200 OK for Render / monitoring health checks.
     */
    private void handleHealthCheckRequest(String path) throws IOException {
        String json = "{\"status\":\"UP\"," +
                "\"service\":\"Tic-Tac-Toe Arena\"," +
                "\"protocol\":\"WebSocket (RFC 6455)\"," +
                "\"storage\":\"" + ServerManager.getInstance().getStorageService().getStorageType() + "\"," +
                "\"onlinePlayers\":" + ServerManager.getInstance().getOnlineCount() + "}";

        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: " + body.length + "\r\n" +
                "Connection: close\r\n" +
                "Access-Control-Allow-Origin: *\r\n\r\n";

        rawOut.write(response.getBytes(StandardCharsets.UTF_8));
        rawOut.write(body);
        rawOut.flush();
    }

    /**
     * Reads line-by-line byte-by-byte without over-reading binary WebSocket frames.
     */
    private String readAsciiLine(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\n') {
                byte[] bytes = baos.toByteArray();
                int len = bytes.length;
                if (len > 0 && bytes[len - 1] == '\r') {
                    len--;
                }
                return new String(bytes, 0, len, StandardCharsets.UTF_8);
            }
            baos.write(c);
        }
        if (baos.size() == 0) return null;
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Parses and dispatches incoming protocol messages.
     */
    private void processCommand(String rawMessage) {
        String[] tokens = MessageProtocol.parseMessage(rawMessage);
        if (tokens.length == 0) return;

        String cmd = tokens[0].toUpperCase();

        switch (cmd) {
            case MessageProtocol.CMD_LOGIN:
                handleLogin(tokens);
                break;

            case MessageProtocol.CMD_CREATE_ROOM:
                handleCreateRoom(tokens);
                break;

            case MessageProtocol.CMD_JOIN_ROOM:
                handleJoinRoom(tokens);
                break;

            case MessageProtocol.CMD_QUICK_MATCH:
                handleQuickMatch();
                break;

            case MessageProtocol.CMD_TOGGLE_READY:
                handleToggleReady(tokens);
                break;

            case MessageProtocol.CMD_START_MATCH:
                handleStartMatch(tokens);
                break;

            case MessageProtocol.CMD_MAKE_MOVE:
                handleMakeMove(tokens);
                break;

            case MessageProtocol.CMD_CHAT:
                handleChat(tokens);
                break;

            case MessageProtocol.CMD_REMATCH_REQUEST:
                handleRematchRequest(tokens);
                break;

            case MessageProtocol.CMD_REMATCH_RESPONSE:
                handleRematchResponse(tokens);
                break;

            case MessageProtocol.CMD_LEAVE_ROOM:
                handleLeaveRoom(tokens);
                break;

            case MessageProtocol.CMD_GET_LEADERBOARD:
                handleGetLeaderboard();
                break;

            case MessageProtocol.CMD_GET_HISTORY:
                handleGetHistory();
                break;

            case MessageProtocol.CMD_PING:
                sendMessage(MessageProtocol.RES_PONG);
                break;

            default:
                sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_ERROR, "Unknown command: " + cmd));
                break;
        }
    }

    private void handleLogin(String[] tokens) {
        if (tokens.length > 1 && !tokens[1].trim().isEmpty()) {
            this.username = tokens[1].trim();
        }
        ServerManager.getInstance().registerClient(this);
        sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_LOGIN_SUCCESS, this.username));
    }

    private void handleCreateRoom(String[] tokens) {
        String mode = (tokens.length > 1) ? tokens[1] : "BO3";
        GameRoom room = ServerManager.getInstance().createRoom(this, mode);
        this.currentRoomCode = room.getRoomCode();
        sendMessage(MessageProtocol.buildMessage(
                MessageProtocol.RES_ROOM_CREATED,
                room.getRoomCode(),
                room.getMatchMode(),
                this.username
        ));
    }

    private void handleJoinRoom(String[] tokens) {
        if (tokens.length < 2) {
            sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_JOIN_FAILED, "Room code is required."));
            return;
        }
        String code = tokens[1].trim().toUpperCase();
        GameRoom room = ServerManager.getInstance().getRoom(code);
        if (room == null) {
            sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_JOIN_FAILED, "Room not found: " + code));
            return;
        }
        if (room.isFull()) {
            sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_JOIN_FAILED, "Room " + code + " is already full."));
            return;
        }
        if (room.getHost() == this) {
            sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_JOIN_FAILED, "You are already the host of this room."));
            return;
        }

        if (room.addGuest(this)) {
            this.currentRoomCode = room.getRoomCode();
        } else {
            sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_JOIN_FAILED, "Failed to join room " + code));
        }
    }

    private void handleQuickMatch() {
        GameRoom room = ServerManager.getInstance().findQuickMatch(this);
        this.currentRoomCode = room.getRoomCode();
        if (room.getHost() == this) {
            sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.RES_ROOM_CREATED,
                    room.getRoomCode(),
                    room.getMatchMode(),
                    this.username
            ));
        }
    }

    private void handleToggleReady(String[] tokens) {
        String code = (tokens.length > 1 && !tokens[1].trim().isEmpty()) ? tokens[1].trim() : currentRoomCode;
        if (code != null) {
            GameRoom room = ServerManager.getInstance().getRoom(code);
            if (room != null) {
                room.toggleReady(this);
            }
        }
    }

    private void handleStartMatch(String[] tokens) {
        String code = (tokens.length > 1 && !tokens[1].trim().isEmpty()) ? tokens[1].trim() : currentRoomCode;
        if (code != null) {
            GameRoom room = ServerManager.getInstance().getRoom(code);
            if (room != null && room.getHost() == this) {
                room.startMatch();
            }
        }
    }

    private void handleMakeMove(String[] tokens) {
        try {
            int row, col;
            String code = currentRoomCode;
            if (tokens.length >= 4) {
                code = tokens[1].trim();
                row = Integer.parseInt(tokens[2].trim());
                col = Integer.parseInt(tokens[3].trim());
            } else if (tokens.length == 3) {
                row = Integer.parseInt(tokens[1].trim());
                col = Integer.parseInt(tokens[2].trim());
            } else {
                return;
            }

            if (code != null) {
                GameRoom room = ServerManager.getInstance().getRoom(code);
                if (room != null) {
                    room.handleMove(this, row, col);
                }
            }
        } catch (NumberFormatException e) {
            sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_MOVE_REJECTED, "Invalid coordinate format."));
        }
    }

    private void handleChat(String[] tokens) {
        String message;
        String code = currentRoomCode;
        if (tokens.length >= 3) {
            code = tokens[1].trim();
            message = tokens[2];
        } else if (tokens.length == 2) {
            message = tokens[1];
        } else {
            return;
        }

        if (code != null) {
            GameRoom room = ServerManager.getInstance().getRoom(code);
            if (room != null) {
                room.sendChat(this, message);
            }
        }
    }

    private void handleRematchRequest(String[] tokens) {
        String code = (tokens.length > 1 && !tokens[1].trim().isEmpty()) ? tokens[1].trim() : currentRoomCode;
        if (code != null) {
            GameRoom room = ServerManager.getInstance().getRoom(code);
            if (room != null) {
                room.requestRematch(this);
            }
        }
    }

    private void handleRematchResponse(String[] tokens) {
        String code = currentRoomCode;
        boolean accept = false;
        if (tokens.length >= 3) {
            code = tokens[1].trim();
            accept = "ACCEPT".equalsIgnoreCase(tokens[2].trim());
        } else if (tokens.length == 2) {
            accept = "ACCEPT".equalsIgnoreCase(tokens[1].trim());
        } else {
            return;
        }

        if (code != null) {
            GameRoom room = ServerManager.getInstance().getRoom(code);
            if (room != null) {
                room.respondRematch(this, accept);
            }
        }
    }

    private void handleLeaveRoom(String[] tokens) {
        String code = (tokens.length > 1 && !tokens[1].trim().isEmpty()) ? tokens[1].trim() : currentRoomCode;
        if (code != null) {
            GameRoom room = ServerManager.getInstance().getRoom(code);
            if (room != null) {
                room.handlePlayerLeave(this, username + " left the room.");
            }
        }
        currentRoomCode = null;
    }

    private void handleGetLeaderboard() {
        String data = ServerManager.getInstance().encodeLeaderboard();
        sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_LEADERBOARD_DATA, data));
    }

    private void handleGetHistory() {
        String data = ServerManager.getInstance().encodeHistory();
        sendMessage(MessageProtocol.buildMessage(MessageProtocol.RES_HISTORY_DATA, data));
    }

    /**
     * Sends a thread-safe message string to the client.
     * Encodes as WebSocket text frame if upgraded, or raw newline-terminated text if TCP.
     */
    public synchronized void sendMessage(String message) {
        if (!isConnected() || message == null) return;
        try {
            if (isWebSocket) {
                byte[] frame = WebSocketFrame.encodeTextFrame(message);
                rawOut.write(frame);
                rawOut.flush();
            } else {
                rawOut.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                rawOut.flush();
            }
        } catch (IOException e) {
            cleanup();
        }
    }

    private synchronized void sendRawBytes(byte[] bytes) {
        if (!isConnected() || bytes == null) return;
        try {
            rawOut.write(bytes);
            rawOut.flush();
        } catch (IOException e) {
            cleanup();
        }
    }

    private void cleanup() {
        running = false;
        if (currentRoomCode != null) {
            GameRoom room = ServerManager.getInstance().getRoom(currentRoomCode);
            if (room != null) {
                room.handlePlayerLeave(this, username + " disconnected.");
            }
            currentRoomCode = null;
        }
        ServerManager.getInstance().unregisterClient(this);
        try {
            if (socket != null && !socket.isClosed()) socket.close();
            if (rawIn != null) rawIn.close();
            if (rawOut != null) rawOut.close();
        } catch (IOException ignored) {}
    }
}
