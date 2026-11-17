package server;

import game.*;
import history.MatchRecord;
import protocol.MessageProtocol;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Manages an authoritative game session between two players.
 * Controls board state, move validation, win/draw detection, series scores (BO1/BO3/BO5),
 * rematch negotiations, and result persistence.
 */
public class GameRoom {
    private final String roomCode;
    private final String matchMode; // "BO1", "BO3", "BO5"
    private final int targetWins;

    private ClientHandler host;
    private ClientHandler guest;

    private boolean hostReady = false;
    private boolean guestReady = false;

    private PlayerSymbol hostSymbol = PlayerSymbol.X;
    private PlayerSymbol guestSymbol = PlayerSymbol.O;
    private PlayerSymbol currentTurn = PlayerSymbol.X;

    private final GameBoard board = new GameBoard();
    private GameStatus status = GameStatus.LOBBY_WAITING;

    private int scoreHost = 0;
    private int scoreGuest = 0;
    private int currentRound = 1;

    private long matchStartTime = 0;
    private boolean hostRematchRequested = false;
    private boolean guestRematchRequested = false;

    public GameRoom(String roomCode, ClientHandler host, String matchMode) {
        this.roomCode = roomCode;
        this.host = host;
        this.matchMode = (matchMode == null || matchMode.trim().isEmpty()) ? "BO3" : matchMode.toUpperCase();

        if ("BO1".equalsIgnoreCase(this.matchMode)) {
            this.targetWins = 1;
        } else if ("BO5".equalsIgnoreCase(this.matchMode)) {
            this.targetWins = 3;
        } else {
            this.targetWins = 2; // Default BO3: first to 2 wins
        }
    }

    public String getRoomCode() { return roomCode; }
    public String getMatchMode() { return matchMode; }
    public int getTargetWins() { return targetWins; }
    public synchronized ClientHandler getHost() { return host; }
    public synchronized ClientHandler getGuest() { return guest; }
    public synchronized GameStatus getStatus() { return status; }
    public synchronized boolean isFull() { return host != null && guest != null; }

    /**
     * Adds guest player to room.
     */
    public synchronized boolean addGuest(ClientHandler client) {
        if (guest != null || status != GameStatus.LOBBY_WAITING) {
            return false;
        }
        this.guest = client;
        this.status = GameStatus.LOBBY_READY;

        // Notify host that player joined
        host.sendMessage(MessageProtocol.buildMessage(
                MessageProtocol.RES_PLAYER_JOINED,
                roomCode,
                guest.getUsername()
        ));

        // Notify guest of successful join
        guest.sendMessage(MessageProtocol.buildMessage(
                MessageProtocol.RES_ROOM_JOINED,
                roomCode,
                host.getUsername(),
                guest.getUsername(),
                matchMode
        ));

        ServerLog.info("ROOM", "Player '" + client.getUsername() + "' (" + client.getRemoteAddress() +
                              ") joined room '" + roomCode + "'");

        broadcastReadyStatus();
        return true;
    }

    /**
     * Toggles or sets player ready status in lobby.
     */
    public synchronized void toggleReady(ClientHandler client) {
        if (client == host) {
            hostReady = !hostReady;
        } else if (client == guest) {
            guestReady = !guestReady;
        }
        broadcastReadyStatus();

        // If both ready, start automatically
        if (hostReady && guestReady && status == GameStatus.LOBBY_READY) {
            startMatch();
        }
    }

    private void broadcastReadyStatus() {
        String msg = MessageProtocol.buildMessage(
                MessageProtocol.RES_PLAYER_READY_STATUS,
                roomCode,
                String.valueOf(hostReady),
                String.valueOf(guestReady)
        );
        broadcast(msg);
    }

    /**
     * Starts the competitive match.
     */
    public synchronized void startMatch() {
        if (host == null || guest == null) return;

        scoreHost = 0;
        scoreGuest = 0;
        currentRound = 1;
        matchStartTime = System.currentTimeMillis();
        hostRematchRequested = false;
        guestRematchRequested = false;

        ServerLog.info("MATCH", "Match started in room '" + roomCode + "': Host '" +
                              host.getUsername() + "' [X] vs Guest '" + guest.getUsername() +
                              "' [O] (Mode: " + matchMode + ", Target Wins: " + targetWins + ")");

        startNewRound(1, PlayerSymbol.X);
    }

    private synchronized void startNewRound(int roundNum, PlayerSymbol startingSymbol) {
        board.reset();
        this.currentRound = roundNum;
        this.currentTurn = startingSymbol;
        this.status = GameStatus.IN_PROGRESS;

        ServerLog.info("ROUND", "Room '" + roomCode + "': Starting Round " + roundNum +
                              " (Starting Turn: " + startingSymbol + ")");

        // Send GAME_START to host (assigned X)
        host.sendMessage(MessageProtocol.buildMessage(
                MessageProtocol.RES_GAME_START,
                roomCode,
                host.getUsername(),
                guest.getUsername(),
                hostSymbol.name(),
                currentTurn.name(),
                matchMode,
                String.valueOf(currentRound),
                String.valueOf(targetWins),
                String.valueOf(scoreHost),
                String.valueOf(scoreGuest)
        ));

        // Send GAME_START to guest (assigned O)
        guest.sendMessage(MessageProtocol.buildMessage(
                MessageProtocol.RES_GAME_START,
                roomCode,
                host.getUsername(),
                guest.getUsername(),
                guestSymbol.name(),
                currentTurn.name(),
                matchMode,
                String.valueOf(currentRound),
                String.valueOf(targetWins),
                String.valueOf(scoreHost),
                String.valueOf(scoreGuest)
        ));
    }

    /**
     * Authoritative move processing.
     */
    public synchronized void handleMove(ClientHandler player, int row, int col) {
        PlayerSymbol playerSymbol = (player == host) ? hostSymbol : guestSymbol;

        // 1. Validate move using MoveValidator
        MoveValidator.ValidationResult val = MoveValidator.validate(
                board, status, currentTurn, playerSymbol, row, col);

        if (!val.isValid) {
            ServerLog.warn("MOVE-REJECTED", "Room '" + roomCode + "': Move at (" + row + ", " + col +
                                          ") rejected for '" + player.getUsername() + "': " + val.errorMessage);
            player.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.RES_MOVE_REJECTED,
                    roomCode,
                    val.errorMessage
            ));
            return;
        }

        // 2. Apply move to server board
        board.placeMove(row, col, playerSymbol);
        ServerLog.info("MOVE", "Room '" + roomCode + "': Player '" + player.getUsername() +
                              "' [" + playerSymbol + "] placed move at (" + row + ", " + col + ")");

        // 3. Determine next turn
        PlayerSymbol nextTurn = currentTurn.opposite();

        // 4. Broadcast the move to both clients immediately
        broadcast(MessageProtocol.buildMessage(
                MessageProtocol.RES_MOVE_MADE,
                roomCode,
                String.valueOf(row),
                String.valueOf(col),
                playerSymbol.name(),
                nextTurn.name()
        ));

        // 5. Evaluate win or draw conditions using WinDetector
        WinDetector.WinResult result = WinDetector.evaluate(board);

        if (result.hasWinner) {
            handleRoundWinner(result.winner, result.winningLine);
        } else if (result.isDraw) {
            handleRoundDraw();
        } else {
            // Still in progress
            this.currentTurn = nextTurn;
        }
    }

    private synchronized void handleRoundWinner(PlayerSymbol winner, WinningLine line) {
        this.status = GameStatus.ROUND_ENDED;
        boolean hostWon = (winner == hostSymbol);
        if (hostWon) {
            scoreHost++;
        } else {
            scoreGuest++;
        }

        boolean seriesOver = (scoreHost >= targetWins) || (scoreGuest >= targetWins);
        String coords = (line != null) ? line.toCoordinateString() : "NONE";

        ServerLog.info("ROUND", "Room '" + roomCode + "': Round " + currentRound + " won by " + winner +
                              " | Score: Host " + scoreHost + " - " + scoreGuest + " Guest");

        broadcast(MessageProtocol.buildMessage(
                MessageProtocol.RES_ROUND_RESULT,
                roomCode,
                winner.name(),
                coords,
                String.valueOf(scoreHost),
                String.valueOf(scoreGuest),
                String.valueOf(seriesOver)
        ));

        if (seriesOver) {
            finishMatch(hostWon ? host.getUsername() : guest.getUsername());
        } else {
            // Schedule next round with alternate starting symbol
            scheduleNextRound();
        }
    }

    private synchronized void handleRoundDraw() {
        this.status = GameStatus.ROUND_ENDED;
        // In case of round draw, no scores are added; check if series is over (rare)
        boolean seriesOver = false;

        ServerLog.info("ROUND", "Room '" + roomCode + "': Round " + currentRound +
                              " ended in DRAW | Score: Host " + scoreHost + " - " + scoreGuest + " Guest");

        broadcast(MessageProtocol.buildMessage(
                MessageProtocol.RES_ROUND_RESULT,
                roomCode,
                "DRAW",
                "NONE",
                String.valueOf(scoreHost),
                String.valueOf(scoreGuest),
                String.valueOf(seriesOver)
        ));

        scheduleNextRound();
    }

    private void scheduleNextRound() {
        // Start next round after short delay so players can see the completed board
        new Thread(() -> {
            try {
                Thread.sleep(2200);
            } catch (InterruptedException ignored) {}

            synchronized (GameRoom.this) {
                if (status == GameStatus.ROUND_ENDED) {
                    PlayerSymbol nextStartingSymbol = (currentRound % 2 == 1) ? PlayerSymbol.O : PlayerSymbol.X;
                    startNewRound(currentRound + 1, nextStartingSymbol);
                }
            }
        }, "RoundTransition-" + roomCode).start();
    }

    private synchronized void finishMatch(String winnerName) {
        this.status = GameStatus.MATCH_FINISHED;
        int durationSec = (int) ((System.currentTimeMillis() - matchStartTime) / 1000);
        if (durationSec < 1) durationSec = 1;

        // Persist to match history and player statistics
        String matchId = "ARENA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateTimeStr = sdf.format(new Date());

        String scoreStr = scoreHost + " - " + scoreGuest;

        MatchRecord record = new MatchRecord(
                matchId,
                host.getUsername(),
                guest.getUsername(),
                winnerName,
                scoreStr,
                dateTimeStr,
                durationSec,
                matchMode
        );

        ServerManager.getInstance().getStorageService().saveMatch(record);
        ServerManager.getInstance().getStorageService().recordMatchResult(
                host.getUsername(),
                guest.getUsername(),
                winnerName
        );

        ServerLog.info("MATCH-OVER", "Room '" + roomCode + "': Series won by '" + winnerName +
                              "' (" + scoreStr + ") in " + durationSec + "s");
        ServerLog.info("PERSIST", "Match " + matchId + " stored to CSV history and rankings updated.");

        broadcast(MessageProtocol.buildMessage(
                MessageProtocol.RES_MATCH_OVER,
                roomCode,
                winnerName,
                String.valueOf(scoreHost),
                String.valueOf(scoreGuest),
                String.valueOf(durationSec)
        ));
    }

    /**
     * In-game chat broadcast.
     */
    public synchronized void sendChat(ClientHandler sender, String message) {
        if (message == null || message.trim().isEmpty()) return;
        String sanitized = message.replace(MessageProtocol.DELIMITER, " ").trim();
        broadcast(MessageProtocol.buildMessage(
                MessageProtocol.RES_CHAT,
                roomCode,
                sender.getUsername(),
                sanitized
        ));
    }

    /**
     * Rematch request handling: requires mutual approval.
     */
    public synchronized void requestRematch(ClientHandler sender) {
        if (sender == host) {
            hostRematchRequested = true;
            if (guest != null) {
                guest.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.RES_REMATCH_REQUESTED,
                        roomCode,
                        host.getUsername()
                ));
            }
        } else if (sender == guest) {
            guestRematchRequested = true;
            if (host != null) {
                host.sendMessage(MessageProtocol.buildMessage(
                        MessageProtocol.RES_REMATCH_REQUESTED,
                        roomCode,
                        guest.getUsername()
                ));
            }
        }

        // If both agreed, restart!
        if (hostRematchRequested && guestRematchRequested) {
            broadcast(MessageProtocol.buildMessage(
                    MessageProtocol.RES_REMATCH_STATUS,
                    roomCode,
                    "ACCEPTED",
                    "Both players accepted rematch!"
            ));
            startMatch();
        }
    }

    public synchronized void respondRematch(ClientHandler responder, boolean accept) {
        if (!accept) {
            broadcast(MessageProtocol.buildMessage(
                    MessageProtocol.RES_REMATCH_STATUS,
                    roomCode,
                    "DECLINED",
                    responder.getUsername() + " declined the rematch."
            ));
            hostRematchRequested = false;
            guestRematchRequested = false;
        } else {
            requestRematch(responder);
        }
    }

    /**
     * Handles unexpected player disconnection or exit.
     */
    public synchronized void handlePlayerLeave(ClientHandler leaver, String reason) {
        if (status == GameStatus.ABORTED) return;
        this.status = GameStatus.ABORTED;

        ServerLog.info("LEAVE", "Room '" + roomCode + "': Player '" + leaver.getUsername() + "' left: " + reason);

        ClientHandler remaining = (leaver == host) ? guest : host;
        if (remaining != null && remaining.isConnected()) {
            remaining.sendMessage(MessageProtocol.buildMessage(
                    MessageProtocol.RES_OPPONENT_LEFT,
                    roomCode,
                    reason != null ? reason : "Opponent disconnected from the match."
            ));
            // If match was in progress, remaining player gets win recorded
            if (status == GameStatus.IN_PROGRESS || status == GameStatus.ROUND_ENDED) {
                finishMatch(remaining.getUsername());
            }
        }

        ServerManager.getInstance().removeRoom(roomCode);
    }

    public synchronized void broadcast(String message) {
        if (host != null && host.isConnected()) {
            host.sendMessage(message);
        }
        if (guest != null && guest.isConnected()) {
            guest.sendMessage(message);
        }
    }
}
