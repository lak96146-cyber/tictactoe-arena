package server;

import protocol.MessageProtocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Game Server for TIC-TAC-TOE ARENA.
 * Listens for incoming client TCP connections on all network interfaces (0.0.0.0),
 * maintains the authoritative game engine, and distributes client handling
 * across multithreaded worker tasks. Fully compatible with headless Linux cloud deployments.
 */
public class GameServer {
    private final String bindHost;
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();
    private volatile boolean running = false;

    public GameServer(int port) {
        this("0.0.0.0", port);
    }

    public GameServer(String bindHost, int port) {
        this.bindHost = (bindHost == null || bindHost.trim().isEmpty()) ? "0.0.0.0" : bindHost.trim();
        this.port = port;
    }

    public void start() {
        running = true;
        try {
            InetAddress bindAddr = InetAddress.getByName(bindHost);
            serverSocket = new ServerSocket(port, 100, bindAddr);

            printBanner();
            ServerLog.info("SERVER", "Tic-Tac-Toe Arena Authoritative Server starting...");
            ServerLog.info("SERVER", "Java Runtime: " + System.getProperty("java.version") +
                                     " (" + System.getProperty("java.vendor") + ")");
            ServerLog.info("SERVER", "Operating System: " + System.getProperty("os.name") +
                                     " " + System.getProperty("os.version") +
                                     " (" + System.getProperty("os.arch") + ")");
            ServerLog.info("SERVER", "Listening on " + bindAddr.getHostAddress() + ":" + port +
                                     " (All network interfaces)");
            ServerLog.info("SERVER", "Data Storage: " +
                                     ServerManager.getInstance().getHistoryManager().getFile().getAbsolutePath());
            ServerLog.info("SERVER", "Ready for remote player connections.\n");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ServerLog.info("CONNECT", "Inbound TCP connection accepted from: " +
                                             clientSocket.getRemoteSocketAddress());
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clientThreadPool.submit(handler);
                } catch (IOException e) {
                    if (!running) {
                        break; // Server stopped normally
                    }
                    ServerLog.error("SERVER", "Socket accept error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            ServerLog.error("SERVER", "Failed to bind to " + bindHost + ":" + port + ": " + e.getMessage());
        } finally {
            stop();
        }
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        ServerLog.info("SHUTDOWN", "Shutting down Tic-Tac-Toe Arena Server...");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        clientThreadPool.shutdownNow();
        ServerLog.info("SHUTDOWN", "Server halted safely.");
    }

    private void printBanner() {
        System.out.println("============================================================");
        System.out.println("   _______ _____ _____       _______       _____ _______ ____  ______ ");
        System.out.println("  |__   __|_   _/ ____|     |__   __|/\\   / ____|__   __/ __ \\|  ____|");
        System.out.println("     | |    | || |    ______   | |  /  \\ | |       | | | |  | | |__   ");
        System.out.println("     | |    | || |   |______|  | | / /\\ \\| |       | | | |  | |  __|  ");
        System.out.println("     | |   _| || |____         | |/ ____ \\ |____   | | | |__| | |____ ");
        System.out.println("     |_|  |_____\\_____|        |_/_/    \\_\\_____|  |_|  \\____/|______|");
        System.out.println("                         A R E N A                            ");
        System.out.println("            AUTHORITATIVE MULTIPLAYER GAME SERVER            ");
        System.out.println("============================================================");
    }

    public static void main(String[] args) {
        // Enforce headless mode on servers to avoid any display requirement
        System.setProperty("java.awt.headless", "true");

        int port = MessageProtocol.DEFAULT_PORT;

        // Check environment variable PORT (common in cloud environments like Heroku, Render, AWS, Docker)
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        // Check CLI argument override
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException ignored) {}
        }

        String bindHost = "0.0.0.0";
        if (args.length > 1 && !args[1].trim().isEmpty()) {
            bindHost = args[1].trim();
        }

        GameServer server = new GameServer(bindHost, port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "Server-Shutdown-Hook"));
        server.start();
    }
}
