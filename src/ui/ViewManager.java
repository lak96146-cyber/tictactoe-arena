package ui;

import client.GameClient;

import javax.swing.*;
import java.awt.*;

/**
 * Manages view transitions between application frames.
 * Preserves window coordinates and dimensions for a smooth, unified desktop app experience.
 */
public class ViewManager {
    private final GameClient client;
    private JFrame activeFrame;

    private LoginFrame loginFrame;
    private HomeFrame homeFrame;
    private LobbyFrame lobbyFrame;
    private GameFrame gameFrame;
    private LeaderboardFrame leaderboardFrame;
    private MatchHistoryFrame matchHistoryFrame;

    public ViewManager(GameClient client) {
        this.client = client;
    }

    public GameClient getClient() {
        return client;
    }

    public synchronized void showLogin() {
        if (loginFrame == null) {
            loginFrame = new LoginFrame(client, this);
        }
        switchFrame(loginFrame);
    }

    public synchronized void showHome() {
        if (homeFrame == null) {
            homeFrame = new HomeFrame(client, this);
        }
        homeFrame.refresh();
        switchFrame(homeFrame);
    }

    public synchronized void showLobby() {
        if (lobbyFrame == null) {
            lobbyFrame = new LobbyFrame(client, this);
        }
        lobbyFrame.refresh();
        switchFrame(lobbyFrame);
    }

    public synchronized void showGame() {
        if (gameFrame == null) {
            gameFrame = new GameFrame(client, this);
        }
        gameFrame.refresh();
        switchFrame(gameFrame);
    }

    public synchronized void showLeaderboard() {
        if (leaderboardFrame == null) {
            leaderboardFrame = new LeaderboardFrame(client, this);
        }
        leaderboardFrame.requestData();
        switchFrame(leaderboardFrame);
    }

    public synchronized void showMatchHistory() {
        if (matchHistoryFrame == null) {
            matchHistoryFrame = new MatchHistoryFrame(client, this);
        }
        matchHistoryFrame.requestData();
        switchFrame(matchHistoryFrame);
    }

    public void showResult(String winner, int sHost, int sGuest, int durationSec) {
        if (gameFrame != null && gameFrame.isVisible()) {
            new ResultFrame(gameFrame, client, this, winner, sHost, sGuest, durationSec).setVisible(true);
        }
    }

    public void showRules(JFrame parent) {
        new RulesDialog(parent).setVisible(true);
    }

    public void showSettings(JFrame parent) {
        new SettingsDialog(parent).setVisible(true);
    }

    private void switchFrame(JFrame nextFrame) {
        if (activeFrame != null && activeFrame != nextFrame) {
            Point loc = activeFrame.getLocation();
            Dimension size = activeFrame.getSize();
            activeFrame.setVisible(false);
            nextFrame.setLocation(loc);
            nextFrame.setSize(size);
        } else if (activeFrame == null) {
            nextFrame.setLocationRelativeTo(null);
        }
        activeFrame = nextFrame;
        nextFrame.setVisible(true);
        nextFrame.toFront();
        nextFrame.requestFocus();
    }
}
