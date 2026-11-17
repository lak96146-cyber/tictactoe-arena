import client.GameClient;
import server.GameServer;
import ui.Theme;
import ui.ViewManager;
import ui.ModernComponents;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main application launcher for TIC-TAC-TOE ARENA.
 * Supports launching as a Server, Client, 2-Player Live Demo,
 * or displaying an interactive launcher window.
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            String mode = args[0].trim().toLowerCase();
            switch (mode) {
                case "server":
                    int port = 8888;
                    if (args.length > 1) {
                        try {
                            port = Integer.parseInt(args[1]);
                        } catch (NumberFormatException ignored) {}
                    }
                    new GameServer(port).start();
                    return;

                case "client":
                    launchClient();
                    return;

                case "demo":
                    launchDemo();
                    return;

                default:
                    System.out.println("Unknown mode: " + mode);
                    System.out.println("Usage: java Main [server|client|demo]");
                    break;
            }
        }

        // Default: Open Graphical Launcher
        SwingUtilities.invokeLater(Main::showLauncherUI);
    }

    public static void launchClient() {
        SwingUtilities.invokeLater(() -> {
            GameClient client = new GameClient();
            ViewManager vm = new ViewManager(client);
            vm.showLogin();
        });
    }

    public static void launchDemo() {
        System.out.println("[DEMO] Starting background server on port 8888...");
        Thread serverThread = new Thread(() -> {
            new GameServer(8888).start();
        }, "Demo-Server-Thread");
        serverThread.setDaemon(true);
        serverThread.start();

        try {
            Thread.sleep(600); // Wait for server to bind
        } catch (InterruptedException ignored) {}

        System.out.println("[DEMO] Spawning Client 1 (Player 1) and Client 2 (Player 2)...");
        SwingUtilities.invokeLater(() -> {
            // Client 1 (left side of screen)
            GameClient c1 = new GameClient();
            ViewManager vm1 = new ViewManager(c1);
            vm1.showLogin();

            // Client 2 (right side of screen)
            GameClient c2 = new GameClient();
            ViewManager vm2 = new ViewManager(c2);
            vm2.showLogin();
        });
    }

    private static void showLauncherUI() {
        JFrame frame = new JFrame("Tic-Tac-Toe Arena — Quick Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(440, 480);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(Theme.BG_DARK);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(30, 36, 30, 36));

        JLabel logo = new JLabel("⚡", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 46));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("TIC-TAC-TOE ARENA", SwingConstants.CENTER);
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("SELECT LAUNCH MODE", SwingConstants.CENTER);
        subtitle.setFont(Theme.FONT_SMALL_BOLD);
        subtitle.setForeground(Theme.ACCENT_CYAN);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Option 1: Play Client
        ModernComponents.ModernButton clientBtn = new ModernComponents.ModernButton(
                "🎮 LAUNCH GAME CLIENT", Theme.ACCENT_PURPLE, Theme.ACCENT_BLUE);
        clientBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        clientBtn.setMaximumSize(new Dimension(360, 46));
        clientBtn.setGlowingBorder(true);
        clientBtn.addActionListener(e -> {
            frame.dispose();
            launchClient();
        });

        // Option 2: Live 2-Player Demo
        ModernComponents.ModernButton demoBtn = new ModernComponents.ModernButton(
                "⚡ LIVE 2-PLAYER DEMO (SERVER + 2 CLIENTS)", Theme.BG_CARD, Theme.BORDER_ACTIVE);
        demoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        demoBtn.setMaximumSize(new Dimension(360, 46));
        demoBtn.addActionListener(e -> {
            frame.dispose();
            launchDemo();
        });

        // Option 3: Dedicated Server
        ModernComponents.ModernButton serverBtn = new ModernComponents.ModernButton(
                "🖥️ START DEDICATED SERVER", Theme.BG_PANEL, Theme.BORDER_COLOR);
        serverBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        serverBtn.setMaximumSize(new Dimension(360, 46));
        serverBtn.addActionListener(e -> {
            frame.dispose();
            new Thread(() -> new GameServer(8888).start(), "Dedicated-Server").start();
        });

        p.add(logo);
        p.add(Box.createVerticalStrut(10));
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(subtitle);
        p.add(Box.createVerticalStrut(32));
        p.add(clientBtn);
        p.add(Box.createVerticalStrut(12));
        p.add(demoBtn);
        p.add(Box.createVerticalStrut(12));
        p.add(serverBtn);

        frame.setContentPane(p);
        frame.setVisible(true);
    }
}
