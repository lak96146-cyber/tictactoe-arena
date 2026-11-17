package test;

import game.*;
import history.HistoryManager;
import history.MatchRecord;
import history.PlayerStats;
import history.ScoreManager;

import java.io.File;

/**
 * Automated test suite verifying the game engine, move validator, win detector,
 * and data persistence logic without requiring GUI or networking.
 */
public class GameLogicTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   TIC-TAC-TOE ARENA — CORE LOGIC TEST SUITE     ");
        System.out.println("=================================================\n");

        int passed = 0;
        int failed = 0;

        try {
            testRowWins();
            System.out.println("[PASS] Horizontal Row Win Tests (3 rows)");
            passed++;
        } catch (Throwable e) {
            System.err.println("[FAIL] Horizontal Row Win Tests: " + e.getMessage());
            failed++;
        }

        try {
            testColumnWins();
            System.out.println("[PASS] Vertical Column Win Tests (3 columns)");
            passed++;
        } catch (Throwable e) {
            System.err.println("[FAIL] Vertical Column Win Tests: " + e.getMessage());
            failed++;
        }

        try {
            testDiagonalWins();
            System.out.println("[PASS] Diagonal Win Tests (Main & Anti-diagonal)");
            passed++;
        } catch (Throwable e) {
            System.err.println("[FAIL] Diagonal Win Tests: " + e.getMessage());
            failed++;
        }

        try {
            testDrawDetection();
            System.out.println("[PASS] Draw / Stalemate Detection Test");
            passed++;
        } catch (Throwable e) {
            System.err.println("[FAIL] Draw Detection Test: " + e.getMessage());
            failed++;
        }

        try {
            testMoveValidation();
            System.out.println("[PASS] Authoritative Move Validation Test");
            passed++;
        } catch (Throwable e) {
            System.err.println("[FAIL] Move Validation Test: " + e.getMessage());
            failed++;
        }

        try {
            testHistoryAndScoreManagers();
            System.out.println("[PASS] File Persistence & Leaderboard Calculation Test");
            passed++;
        } catch (Throwable e) {
            System.err.println("[FAIL] File Persistence Test: " + e.getMessage());
            failed++;
        }

        System.out.println("\n-------------------------------------------------");
        System.out.println("RESULTS: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("-------------------------------------------------");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testRowWins() {
        for (int r = 0; r < 3; r++) {
            GameBoard board = new GameBoard();
            board.placeMove(r, 0, PlayerSymbol.X);
            board.placeMove(r, 1, PlayerSymbol.X);
            board.placeMove(r, 2, PlayerSymbol.X);

            WinDetector.WinResult res = WinDetector.evaluate(board);
            if (!res.hasWinner || res.winner != PlayerSymbol.X) {
                throw new AssertionError("Failed to detect horizontal win on row " + r);
            }
            if (res.winningLine == null || !res.winningLine.contains(r, 0) || !res.winningLine.contains(r, 2)) {
                throw new AssertionError("WinningLine coordinates mismatch on row " + r);
            }
        }
    }

    private static void testColumnWins() {
        for (int c = 0; c < 3; c++) {
            GameBoard board = new GameBoard();
            board.placeMove(0, c, PlayerSymbol.O);
            board.placeMove(1, c, PlayerSymbol.O);
            board.placeMove(2, c, PlayerSymbol.O);

            WinDetector.WinResult res = WinDetector.evaluate(board);
            if (!res.hasWinner || res.winner != PlayerSymbol.O) {
                throw new AssertionError("Failed to detect vertical win on col " + c);
            }
            if (res.winningLine == null || !res.winningLine.contains(0, c) || !res.winningLine.contains(2, c)) {
                throw new AssertionError("WinningLine coordinates mismatch on col " + c);
            }
        }
    }

    private static void testDiagonalWins() {
        // Main diagonal: (0,0), (1,1), (2,2)
        GameBoard board1 = new GameBoard();
        board1.placeMove(0, 0, PlayerSymbol.X);
        board1.placeMove(1, 1, PlayerSymbol.X);
        board1.placeMove(2, 2, PlayerSymbol.X);
        WinDetector.WinResult res1 = WinDetector.evaluate(board1);
        if (!res1.hasWinner || res1.winner != PlayerSymbol.X) {
            throw new AssertionError("Failed main diagonal win");
        }

        // Anti diagonal: (0,2), (1,1), (2,0)
        GameBoard board2 = new GameBoard();
        board2.placeMove(0, 2, PlayerSymbol.O);
        board2.placeMove(1, 1, PlayerSymbol.O);
        board2.placeMove(2, 0, PlayerSymbol.O);
        WinDetector.WinResult res2 = WinDetector.evaluate(board2);
        if (!res2.hasWinner || res2.winner != PlayerSymbol.O) {
            throw new AssertionError("Failed anti-diagonal win");
        }
    }

    private static void testDrawDetection() {
        /*
           X O X
           X X O
           O X O
        */
        GameBoard board = new GameBoard();
        board.placeMove(0, 0, PlayerSymbol.X);
        board.placeMove(0, 1, PlayerSymbol.O);
        board.placeMove(0, 2, PlayerSymbol.X);

        board.placeMove(1, 0, PlayerSymbol.X);
        board.placeMove(1, 1, PlayerSymbol.X);
        board.placeMove(1, 2, PlayerSymbol.O);

        board.placeMove(2, 0, PlayerSymbol.O);
        board.placeMove(2, 1, PlayerSymbol.X);
        board.placeMove(2, 2, PlayerSymbol.O);

        WinDetector.WinResult res = WinDetector.evaluate(board);
        if (res.hasWinner) {
            throw new AssertionError("Incorrectly declared winner on draw board");
        }
        if (!res.isDraw) {
            throw new AssertionError("Failed to detect draw condition");
        }
    }

    private static void testMoveValidation() {
        GameBoard board = new GameBoard();
        board.placeMove(0, 0, PlayerSymbol.X);

        // 1. Cell occupied check
        MoveValidator.ValidationResult r1 = MoveValidator.validate(
                board, GameStatus.IN_PROGRESS, PlayerSymbol.O, PlayerSymbol.O, 0, 0);
        if (r1.isValid) {
            throw new AssertionError("Allowed move on occupied cell (0,0)");
        }

        // 2. Out of turn check
        MoveValidator.ValidationResult r2 = MoveValidator.validate(
                board, GameStatus.IN_PROGRESS, PlayerSymbol.O, PlayerSymbol.X, 1, 1);
        if (r2.isValid) {
            throw new AssertionError("Allowed player to move out of turn");
        }

        // 3. Out of bounds check
        MoveValidator.ValidationResult r3 = MoveValidator.validate(
                board, GameStatus.IN_PROGRESS, PlayerSymbol.O, PlayerSymbol.O, 3, 1);
        if (r3.isValid) {
            throw new AssertionError("Allowed move out of bounds (3, 1)");
        }

        // 4. Inactive game check
        MoveValidator.ValidationResult r4 = MoveValidator.validate(
                board, GameStatus.ROUND_ENDED, PlayerSymbol.O, PlayerSymbol.O, 1, 1);
        if (r4.isValid) {
            throw new AssertionError("Allowed move when round ended");
        }

        // 5. Valid move check
        MoveValidator.ValidationResult r5 = MoveValidator.validate(
                board, GameStatus.IN_PROGRESS, PlayerSymbol.O, PlayerSymbol.O, 1, 1);
        if (!r5.isValid) {
            throw new AssertionError("Rejected valid move: " + r5.errorMessage);
        }
    }

    private static void testHistoryAndScoreManagers() {
        String testHistPath = "data/test_history.csv";
        String testStatsPath = "data/test_stats.csv";

        // Clean up any test leftovers
        new File(testHistPath).delete();
        new File(testStatsPath).delete();

        HistoryManager hm = new HistoryManager(testHistPath);
        ScoreManager sm = new ScoreManager(testStatsPath);

        MatchRecord r = new MatchRecord("TEST-01", "Alice", "Bob", "Alice", "2 - 0", "2026-11-17 12:00:00", 65, "BO3");
        hm.saveMatch(r);

        java.util.List<MatchRecord> all = hm.getAllMatches();
        if (all.size() != 1 || !all.get(0).getMatchId().equals("TEST-01")) {
            throw new AssertionError("HistoryManager failed to save or read match");
        }

        sm.recordMatchResult("Alice", "Bob", "Alice");
        PlayerStats alice = sm.getOrCreatePlayer("Alice");
        PlayerStats bob = sm.getOrCreatePlayer("Bob");

        if (alice.getWins() != 1 || alice.getLosses() != 0) {
            throw new AssertionError("Alice stats incorrect: " + alice.getWins());
        }
        if (bob.getWins() != 0 || bob.getLosses() != 1) {
            throw new AssertionError("Bob stats incorrect: " + bob.getLosses());
        }

        // Cleanup
        new File(testHistPath).delete();
        new File(testStatsPath).delete();
    }
}
