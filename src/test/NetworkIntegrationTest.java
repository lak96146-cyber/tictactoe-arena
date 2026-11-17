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
 * Full end-to-end network integration test:
 * Boots an in-process GameServer, connects two live GameClients over TCP sockets,
 * performs room creation, joining, ready negotiation, move validation, win detection,
 * chat message broadcasting, and match completion verification.
 */
public class NetworkIntegrationTest {
    private static final int TEST_PORT = 9123;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  TIC-TAC-TOE ARENA — NETWORK INTEGRATION TEST   ");
        System.out.println("=================================================\n");

        GameServer server = new GameServer(TEST_PORT);
        Thread serverThread = new Thread(server::start, "TestServer");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(500); // Wait for server to bind

            runFullMatchTest();
            System.out.println("\n>>> ALL NETWORK INTEGRATION TESTS COMPLETED SUCCESSFULLY! <<<");
        } catch (Throwable t) {
            System.err.println("\n[ERROR] Integration test failed: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        } finally {
            server.stop();
            System.exit(0);
        }
    }

    private static void runFullMatchTest() throws Exception {
        GameClient c1 = new GameClient();
        GameClient c2 = new GameClient();

        CountDownLatch c1LoginLatch = new CountDownLatch(1);
        CountDownLatch c2LoginLatch = new CountDownLatch(1);
        AtomicReference<String> roomCodeRef = new AtomicReference<>();
        CountDownLatch roomCreatedLatch = new CountDownLatch(1);
        CountDownLatch c2JoinedLatch = new CountDownLatch(1);
        CountDownLatch gameStartLatch = new CountDownLatch(2);
        CountDownLatch matchOverLatch = new CountDownLatch(2);
        AtomicReference<String> finalWinnerRef = new AtomicReference<>();
        AtomicBoolean chatReceived = new AtomicBoolean(false);

        c1.addListener(new TestListenerAdapter() {
            @Override
            public void onLoginSuccess(String username) {
                c1LoginLatch.countDown();
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
            public void onMatchOver(String roomCode, String winnerName, int sHost, int sGuest, int durationSec) {
                finalWinnerRef.set(winnerName);
                matchOverLatch.countDown();
            }
        });

        c2.addListener(new TestListenerAdapter() {
            @Override
            public void onLoginSuccess(String username) {
                c2LoginLatch.countDown();
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
            public void onChatReceived(String roomCode, String sender, String message) {
                if ("Hello from C1".equals(message)) {
                    chatReceived.set(true);
                }
            }

            @Override
            public void onMatchOver(String roomCode, String winnerName, int sHost, int sGuest, int durationSec) {
                matchOverLatch.countDown();
            }
        });

        // 1. Connect and Login
        System.out.println("[STEP 1] Connecting Client 1 ('Alpha') and Client 2 ('Beta')...");
        c1.connect("localhost", TEST_PORT, "Alpha");
        c2.connect("localhost", TEST_PORT, "Beta");

        if (!c1LoginLatch.await(3, TimeUnit.SECONDS) || !c2LoginLatch.await(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Client login timed out");
        }
        System.out.println(" -> Clients logged in successfully.");

        // 2. Client 1 creates private room (BO1)
        System.out.println("[STEP 2] Client 1 creating room BO1...");
        c1.createRoom("BO1");
        if (!roomCreatedLatch.await(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Room creation timed out");
        }
        String roomCode = roomCodeRef.get();
        System.out.println(" -> Room created with code: " + roomCode);

        // 3. Client 2 joins the room
        System.out.println("[STEP 3] Client 2 joining room " + roomCode + "...");
        c2.joinRoom(roomCode);
        if (!c2JoinedLatch.await(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Room joining timed out");
        }
        System.out.println(" -> Client 2 joined room.");

        // 4. Test In-Game Chat
        System.out.println("[STEP 4] Testing in-room chat broadcasting...");
        c1.sendChat("Hello from C1");
        Thread.sleep(300);
        if (!chatReceived.get()) {
            throw new RuntimeException("In-room chat was not received by opponent");
        }
        System.out.println(" -> Chat broadcast verified.");

        // 5. Both players ready -> match starts
        System.out.println("[STEP 5] Both players setting READY status...");
        c1.toggleReady();
        c2.toggleReady();

        if (!gameStartLatch.await(3, TimeUnit.SECONDS)) {
            throw new RuntimeException("Game start timed out");
        }
        System.out.println(" -> Match started! C1 symbol: " + c1.getMySymbol() + ", C2 symbol: " + c2.getMySymbol());

        // 6. Test Out-Of-Turn Rejection (C2 attempts to move first when it is C1's turn)
        System.out.println("[STEP 6] Testing invalid out-of-turn move rejection...");
        CountDownLatch rejectionLatch = new CountDownLatch(1);
        c2.addListener(new TestListenerAdapter() {
            @Override
            public void onMoveRejected(String reason) {
                rejectionLatch.countDown();
            }
        });
        c2.sendMove(1, 1);
        if (!rejectionLatch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("Server failed to reject out-of-turn move!");
        }
        System.out.println(" -> Server correctly rejected out-of-turn move.");

        // 7. Play match to victory for Client 1:
        // C1: (0,0) -> C2: (1,0) -> C1: (0,1) -> C2: (1,1) -> C1: (0,2) [C1 wins top row!]
        System.out.println("[STEP 7] Executing winning move sequence...");
        c1.sendMove(0, 0);
        Thread.sleep(200);
        c2.sendMove(1, 0);
        Thread.sleep(200);
        c1.sendMove(0, 1);
        Thread.sleep(200);
        c2.sendMove(1, 1);
        Thread.sleep(200);
        c1.sendMove(0, 2); // Winning move!

        if (!matchOverLatch.await(5, TimeUnit.SECONDS)) {
            throw new RuntimeException("Match over notification timed out");
        }

        System.out.println(" -> Match concluded! Declared Winner: " + finalWinnerRef.get());
        if (!"Alpha".equals(finalWinnerRef.get())) {
            throw new RuntimeException("Expected winner 'Alpha', got: " + finalWinnerRef.get());
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
