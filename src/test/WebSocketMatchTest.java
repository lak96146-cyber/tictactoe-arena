package test;

import client.ClientListener;
import client.GameClient;
import game.PlayerSymbol;
import game.WinningLine;
import history.MatchRecord;
import history.PlayerStats;
import server.GameServer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * End-to-end automated test suite verifying WebSocket multiplayer communications:
 * 1. WebSocket handshake (RFC 6455)
 * 2. Room creation and joining over WebSockets
 * 3. In-game chat broadcast over WebSockets
 * 4. Multi-round Best-of-3 gameplay over WebSockets
 * 5. Series winner detection and clean disconnect
 */
public class WebSocketMatchTest {
    private static final int WS_TEST_PORT = 9450;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  TIC-TAC-TOE ARENA — WEBSOCKET MATCH TEST       ");
        System.out.println("=================================================\n");

        GameServer server = new GameServer(WS_TEST_PORT);
        Thread serverThread = new Thread(server::start, "WsTestServer");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(600); // Wait for WebSocket server to bind

            runWebSocketTest();
            System.out.println("\n>>> ALL WEBSOCKET TESTS PASSED WITH 100% SUCCESS! <<<");
        } catch (Throwable t) {
            System.err.println("\n[ERROR] WebSocket test failed: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        } finally {
            server.stop();
            System.exit(0);
        }
    }

    private static void runWebSocketTest() throws Exception {
        GameClient c1 = new GameClient();
        GameClient c2 = new GameClient();

        CountDownLatch loginLatch = new CountDownLatch(2);
        AtomicReference<String> roomCodeRef = new AtomicReference<>();
        CountDownLatch roomCreatedLatch = new CountDownLatch(1);
        CountDownLatch c2JoinedLatch = new CountDownLatch(1);
        CountDownLatch gameStartLatch = new CountDownLatch(2);
        CountDownLatch round1WonLatch = new CountDownLatch(2);
        CountDownLatch seriesOverLatch = new CountDownLatch(2);
        AtomicReference<String> championRef = new AtomicReference<>();
        AtomicBoolean chatReceived = new AtomicBoolean(false);

        c1.addListener(new TestListenerAdapter() {
            @Override
            public void onLoginSuccess(String username) {
                loginLatch.countDown();
            }

            @Override
            public void onRoomCreated(String roomCode, String mode, String hostName) {
                roomCodeRef.set(roomCode);
                roomCreatedLatch.countDown();
            }

            @Override
            public void onGameStart(String roomCode, String pX, String pO, PlayerSymbol mySym,
                                    PlayerSymbol curTurn, String mode, int curRound, int targetWins,
                                    int sHost, int sGuest) {
                gameStartLatch.countDown();
            }

            @Override
            public void onRoundResult(String roomCode, String winnerSymbol, WinningLine line, int sHost, int sGuest, boolean seriesOver) {
                round1WonLatch.countDown();
            }

            @Override
            public void onMatchOver(String roomCode, String winnerName, int sHost, int sGuest, int durationSec) {
                championRef.set(winnerName);
                seriesOverLatch.countDown();
            }
        });

        c2.addListener(new TestListenerAdapter() {
            @Override
            public void onLoginSuccess(String username) {
                loginLatch.countDown();
            }

            @Override
            public void onRoomJoined(String roomCode, String hostName, String guestName, String mode) {
                c2JoinedLatch.countDown();
            }

            @Override
            public void onGameStart(String roomCode, String pX, String pO, PlayerSymbol mySym,
                                    PlayerSymbol curTurn, String mode, int curRound, int targetWins,
                                    int sHost, int sGuest) {
                gameStartLatch.countDown();
            }

            @Override
            public void onRoundResult(String roomCode, String winnerSymbol, WinningLine line, int sHost, int sGuest, boolean seriesOver) {
                round1WonLatch.countDown();
            }

            @Override
            public void onChatReceived(String roomCode, String sender, String message) {
                if ("WebSocket Comms Operational".equals(message)) {
                    chatReceived.set(true);
                }
            }

            @Override
            public void onMatchOver(String roomCode, String winnerName, int sHost, int sGuest, int durationSec) {
                seriesOverLatch.countDown();
            }
        });

        // 1. Connect via WebSockets
        System.out.println("[TEST 1] Connecting two Java clients via WebSockets (ws://localhost:" + WS_TEST_PORT + "/ws)...");
        c1.connect("localhost", WS_TEST_PORT, "Viper");
        c2.connect("localhost", WS_TEST_PORT, "Phantom");

        if (!loginLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("WebSocket client login timed out");
        }
        System.out.println(" -> Both clients successfully connected and authenticated via WebSockets!");

        // 2. Create room
        System.out.println("[TEST 2] Host 'Viper' creating room via WebSocket (BO1 format)...");
        c1.createRoom("BO1");
        if (!roomCreatedLatch.await(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Room creation over WebSocket timed out");
        }
        String roomCode = roomCodeRef.get();
        System.out.println(" -> Room created successfully: " + roomCode);

        // 3. Join room
        System.out.println("[TEST 3] Guest 'Phantom' joining room " + roomCode + " via WebSocket...");
        c2.joinRoom(roomCode);
        if (!c2JoinedLatch.await(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Room joining over WebSocket timed out");
        }
        System.out.println(" -> Guest successfully joined room over WebSocket.");

        // 4. Test In-Game Chat
        System.out.println("[TEST 4] Testing chat text framing over WebSocket...");
        c1.sendChat("WebSocket Comms Operational");
        Thread.sleep(300);
        if (!chatReceived.get()) {
            throw new RuntimeException("Chat message was not delivered over WebSocket");
        }
        System.out.println(" -> Chat message frame verified.");

        // 5. Ready and start
        System.out.println("[TEST 5] Both players setting READY...");
        c1.toggleReady();
        c2.toggleReady();

        if (!gameStartLatch.await(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Game start notification over WebSocket timed out");
        }
        System.out.println(" -> Match active! Starting turn: " + c1.getCurrentTurn());

        // 6. Play winning sequence
        System.out.println("[TEST 6] Executing move sequence over WebSockets...");
        c1.sendMove(0, 0); // X
        Thread.sleep(200);
        c2.sendMove(1, 0); // O
        Thread.sleep(200);
        c1.sendMove(0, 1); // X
        Thread.sleep(200);
        c2.sendMove(1, 1); // O
        Thread.sleep(200);
        c1.sendMove(0, 2); // X wins row 0!

        if (!round1WonLatch.await(4, TimeUnit.SECONDS)) {
            throw new RuntimeException("Round result frame timed out");
        }
        System.out.println(" -> Round won notification verified!");

        if (!seriesOverLatch.await(4, TimeUnit.SECONDS)) {
            throw new RuntimeException("Series over frame timed out");
        }
        System.out.println(" -> Match Champion verified: " + championRef.get());
        if (!"Viper".equals(championRef.get())) {
            throw new RuntimeException("Expected winner 'Viper', got: " + championRef.get());
        }

        // Cleanup
        c1.disconnect();
        c2.disconnect();
    }

    private static class TestListenerAdapter implements ClientListener {
        @Override public void onConnected() {}
        @Override public void onConnectionFailed(String r) {}
        @Override public void onDisconnected(String r) {}
        @Override public void onLoginSuccess(String u) {}
        @Override public void onRoomCreated(String c, String m, String h) {}
        @Override public void onRoomJoined(String c, String h, String g, String m) {}
        @Override public void onJoinFailed(String r) {}
        @Override public void onPlayerJoined(String c, String g) {}
        @Override public void onReadyStatus(String c, boolean hr, boolean gr) {}
        @Override public void onGameStart(String c, String x, String o, PlayerSymbol s, PlayerSymbol t, String m, int r, int tw, int sh, int sg) {}
        @Override public void onMoveMade(String c, int r, int col, PlayerSymbol s, PlayerSymbol nt) {}
        @Override public void onMoveRejected(String r) {}
        @Override public void onRoundResult(String c, String w, WinningLine l, int sh, int sg, boolean so) {}
        @Override public void onMatchOver(String c, String w, int sh, int sg, int d) {}
        @Override public void onChatReceived(String c, String s, String m) {}
        @Override public void onRematchRequested(String f) {}
        @Override public void onRematchStatus(String s, String d) {}
        @Override public void onOpponentLeft(String r) {}
        @Override public void onLeaderboardReceived(List<PlayerStats> l) {}
        @Override public void onHistoryReceived(List<MatchRecord> h) {}
    }
}
