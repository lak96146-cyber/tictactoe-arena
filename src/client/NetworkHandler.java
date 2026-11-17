package client;

import game.PlayerSymbol;
import game.WinningLine;
import history.MatchRecord;
import history.PlayerStats;
import protocol.MessageProtocol;

import javax.swing.SwingUtilities;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Handles WebSocket network I/O using Java 11+ built-in java.net.http.WebSocket.
 * Supports both unencrypted ws:// (local testing) and TLS-secured wss:// (Render cloud deployment).
 * Marshals all events onto the Swing Event Dispatch Thread (EDT).
 */
public class NetworkHandler {
    private WebSocket webSocket;
    private HttpClient httpClient;
    private volatile boolean running = false;
    private ClientListener listener;

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    public synchronized boolean isConnected() {
        return running && webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed();
    }

    /**
     * Connects asynchronously to the WebSocket game server.
     */
    public void connect(String hostOrUrl, int port) {
        URI uri = normalizeWebSocketUri(hostOrUrl, port);

        new Thread(() -> {
            try {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(7))
                            .build();
                }

                httpClient.newWebSocketBuilder()
                        .connectTimeout(Duration.ofSeconds(7))
                        .buildAsync(uri, new WsListener())
                        .whenComplete((ws, throwable) -> {
                            if (throwable != null) {
                                running = false;
                                postToEdt(() -> {
                                    if (listener != null) {
                                        String err = throwable.getMessage();
                                        if (err == null && throwable.getCause() != null) {
                                            err = throwable.getCause().getMessage();
                                        }
                                        listener.onConnectionFailed(err != null ? err : "Failed to connect to " + uri);
                                    }
                                });
                            } else {
                                this.webSocket = ws;
                                this.running = true;
                                postToEdt(() -> {
                                    if (listener != null) listener.onConnected();
                                });
                            }
                        });
            } catch (Exception e) {
                running = false;
                postToEdt(() -> {
                    if (listener != null) listener.onConnectionFailed(e.getMessage());
                });
            }
        }, "WebSocketConnectThread").start();
    }

    /**
     * Converts raw host, IP, or URL string into a proper ws:// or wss:// URI.
     */
    public static URI normalizeWebSocketUri(String hostOrUrl, int port) {
        String input = (hostOrUrl == null || hostOrUrl.trim().isEmpty()) ? "localhost" : hostOrUrl.trim();

        // Convert HTTP prefixes to WebSocket schemes
        if (input.startsWith("http://")) {
            input = "ws://" + input.substring(7);
        } else if (input.startsWith("https://")) {
            input = "wss://" + input.substring(8);
        }

        // If no scheme was provided, determine based on domain or port
        if (!input.startsWith("ws://") && !input.startsWith("wss://")) {
            boolean isRender = input.toLowerCase().contains("onrender.com");
            boolean isSecurePort = (port == 443);
            String scheme = (isRender || isSecurePort) ? "wss://" : "ws://";

            if (input.contains(":")) {
                input = scheme + input;
            } else {
                if (port > 0 && port != 80 && port != 443) {
                    input = scheme + input + ":" + port;
                } else {
                    input = scheme + input;
                }
            }
        }

        // Ensure /ws path
        if (!input.contains("/ws") && !input.contains("/health")) {
            if (input.endsWith("/")) {
                input = input + "ws";
            } else {
                input = input + "/ws";
            }
        }

        return URI.create(input);
    }

    /**
     * Sends a protocol message as a WebSocket text frame.
     */
    public synchronized void send(String message) {
        if (webSocket != null && isConnected() && message != null) {
            webSocket.sendText(message, true);
        }
    }

    /**
     * Closes the WebSocket connection cleanly.
     */
    public synchronized void disconnect(String reason) {
        if (!running) return;
        running = false;
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnect");
            } catch (Exception ignored) {}
        }
        postToEdt(() -> {
            if (listener != null) listener.onDisconnected(reason);
        });
    }

    private void postToEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    /**
     * WebSocket listener handling incoming text frames, closures, and errors.
     */
    private class WsListener implements WebSocket.Listener {
        private final StringBuilder textBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            ws.request(1);
            WebSocket.Listener.super.onOpen(ws);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String fullMessage = textBuffer.toString();
                textBuffer.setLength(0);
                postToEdt(() -> processIncomingMessage(fullMessage));
            }
            ws.request(1);
            return WebSocket.Listener.super.onText(ws, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            running = false;
            postToEdt(() -> {
                if (listener != null) {
                    listener.onDisconnected("Server closed connection (" + statusCode + "): " + reason);
                }
            });
            return WebSocket.Listener.super.onClose(ws, statusCode, reason);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            running = false;
            postToEdt(() -> {
                if (listener != null) {
                    String msg = error.getMessage();
                    listener.onDisconnected("WebSocket error: " + (msg != null ? msg : error.toString()));
                }
            });
            WebSocket.Listener.super.onError(ws, error);
        }
    }

    /**
     * Decodes server responses and triggers listener callbacks on EDT.
     */
    private void processIncomingMessage(String raw) {
        if (listener == null) return;
        String[] tokens = MessageProtocol.parseMessage(raw);
        if (tokens.length == 0) return;

        String cmd = tokens[0].toUpperCase();

        switch (cmd) {
            case MessageProtocol.RES_LOGIN_SUCCESS:
                listener.onLoginSuccess(tokens.length > 1 ? tokens[1] : "Player");
                break;

            case MessageProtocol.RES_ROOM_CREATED:
                listener.onRoomCreated(
                        tokens.length > 1 ? tokens[1] : "",
                        tokens.length > 2 ? tokens[2] : "BO3",
                        tokens.length > 3 ? tokens[3] : ""
                );
                break;

            case MessageProtocol.RES_ROOM_JOINED:
                listener.onRoomJoined(
                        tokens.length > 1 ? tokens[1] : "",
                        tokens.length > 2 ? tokens[2] : "",
                        tokens.length > 3 ? tokens[3] : "",
                        tokens.length > 4 ? tokens[4] : "BO3"
                );
                break;

            case MessageProtocol.RES_JOIN_FAILED:
                listener.onJoinFailed(tokens.length > 1 ? tokens[1] : "Failed to join room.");
                break;

            case MessageProtocol.RES_PLAYER_JOINED:
                listener.onPlayerJoined(
                        tokens.length > 1 ? tokens[1] : "",
                        tokens.length > 2 ? tokens[2] : "Guest"
                );
                break;

            case MessageProtocol.RES_PLAYER_READY_STATUS:
                boolean hReady = tokens.length > 2 && Boolean.parseBoolean(tokens[2]);
                boolean gReady = tokens.length > 3 && Boolean.parseBoolean(tokens[3]);
                listener.onReadyStatus(tokens.length > 1 ? tokens[1] : "", hReady, gReady);
                break;

            case MessageProtocol.RES_GAME_START:
                try {
                    String roomCode = tokens[1];
                    String pX = tokens[2];
                    String pO = tokens[3];
                    PlayerSymbol mySym = PlayerSymbol.fromString(tokens[4]);
                    PlayerSymbol curTurn = PlayerSymbol.fromString(tokens[5]);
                    String mode = tokens[6];
                    int curRound = Integer.parseInt(tokens[7]);
                    int targetWins = Integer.parseInt(tokens[8]);
                    int sHost = Integer.parseInt(tokens[9]);
                    int sGuest = Integer.parseInt(tokens[10]);

                    listener.onGameStart(roomCode, pX, pO, mySym, curTurn, mode, curRound, targetWins, sHost, sGuest);
                } catch (Exception e) {
                    System.err.println("[NetworkHandler] Error parsing GAME_START: " + e.getMessage());
                }
                break;

            case MessageProtocol.RES_MOVE_MADE:
                try {
                    String roomCode = tokens[1];
                    int r = Integer.parseInt(tokens[2]);
                    int c = Integer.parseInt(tokens[3]);
                    PlayerSymbol sym = PlayerSymbol.fromString(tokens[4]);
                    PlayerSymbol nextTurn = PlayerSymbol.fromString(tokens[5]);
                    listener.onMoveMade(roomCode, r, c, sym, nextTurn);
                } catch (Exception e) {
                    System.err.println("[NetworkHandler] Error parsing MOVE_MADE: " + e.getMessage());
                }
                break;

            case MessageProtocol.RES_MOVE_REJECTED:
                listener.onMoveRejected(tokens.length > 2 ? tokens[2] : "Move rejected");
                break;

            case MessageProtocol.RES_ROUND_RESULT:
                try {
                    String roomCode = tokens[1];
                    String winnerSym = tokens[2];
                    WinningLine line = WinningLine.fromCoordinateString(tokens[3]);
                    int sHost = Integer.parseInt(tokens[4]);
                    int sGuest = Integer.parseInt(tokens[5]);
                    boolean seriesOver = Boolean.parseBoolean(tokens[6]);
                    listener.onRoundResult(roomCode, winnerSym, line, sHost, sGuest, seriesOver);
                } catch (Exception e) {
                    System.err.println("[NetworkHandler] Error parsing ROUND_RESULT: " + e.getMessage());
                }
                break;

            case MessageProtocol.RES_MATCH_OVER:
                try {
                    String roomCode = tokens[1];
                    String winnerName = tokens[2];
                    int sHost = Integer.parseInt(tokens[3]);
                    int sGuest = Integer.parseInt(tokens[4]);
                    int duration = Integer.parseInt(tokens[5]);
                    listener.onMatchOver(roomCode, winnerName, sHost, sGuest, duration);
                } catch (Exception e) {
                    System.err.println("[NetworkHandler] Error parsing MATCH_OVER: " + e.getMessage());
                }
                break;

            case MessageProtocol.RES_CHAT:
                listener.onChatReceived(
                        tokens.length > 1 ? tokens[1] : "",
                        tokens.length > 2 ? tokens[2] : "Player",
                        tokens.length > 3 ? tokens[3] : ""
                );
                break;

            case MessageProtocol.RES_REMATCH_REQUESTED:
                listener.onRematchRequested(tokens.length > 2 ? tokens[2] : "Opponent");
                break;

            case MessageProtocol.RES_REMATCH_STATUS:
                listener.onRematchStatus(
                        tokens.length > 2 ? tokens[2] : "",
                        tokens.length > 3 ? tokens[3] : ""
                );
                break;

            case MessageProtocol.RES_OPPONENT_LEFT:
                listener.onOpponentLeft(tokens.length > 2 ? tokens[2] : "Opponent has left.");
                break;

            case MessageProtocol.RES_LEADERBOARD_DATA:
                parseLeaderboardData(tokens.length > 1 ? tokens[1] : "");
                break;

            case MessageProtocol.RES_HISTORY_DATA:
                parseHistoryData(tokens.length > 1 ? tokens[1] : "");
                break;

            case MessageProtocol.RES_PONG:
                // Heartbeat response
                break;

            default:
                break;
        }
    }

    private void parseLeaderboardData(String rawData) {
        List<PlayerStats> list = new ArrayList<>();
        if (rawData == null || rawData.isEmpty()) {
            listener.onLeaderboardReceived(list);
            return;
        }
        String[] entries = rawData.split("~");
        for (int i = 1; i < entries.length; i++) {
            String[] fields = entries[i].split(";");
            if (fields.length >= 5) {
                try {
                    String name = fields[0];
                    int matches = Integer.parseInt(fields[1]);
                    int wins = Integer.parseInt(fields[2]);
                    int losses = Integer.parseInt(fields[3]);
                    int draws = Integer.parseInt(fields[4]);
                    list.add(new PlayerStats(name, matches, wins, losses, draws));
                } catch (Exception ignored) {}
            }
        }
        listener.onLeaderboardReceived(list);
    }

    private void parseHistoryData(String rawData) {
        List<MatchRecord> list = new ArrayList<>();
        if (rawData == null || rawData.isEmpty()) {
            listener.onHistoryReceived(list);
            return;
        }
        String[] entries = rawData.split("~");
        for (int i = 1; i < entries.length; i++) {
            String[] fields = entries[i].split(";");
            if (fields.length >= 8) {
                try {
                    String id = fields[0];
                    String p1 = fields[1];
                    String p2 = fields[2];
                    String winner = fields[3];
                    String result = fields[4];
                    String dateTime = fields[5];
                    String durStr = fields[6];
                    String mode = fields[7];

                    int durSec = 60;
                    list.add(new MatchRecord(id, p1, p2, winner, result, dateTime, durSec, mode));
                } catch (Exception ignored) {}
            }
        }
        listener.onHistoryReceived(list);
    }
}
