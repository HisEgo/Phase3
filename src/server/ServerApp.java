package server;

import database.HibernateUtil;
import exception.GlobalExceptionHandler;

import java.util.Scanner;


public class ServerApp {

    private static final int DEFAULT_PORT = 8081;
    private GameServer gameServer;
    private boolean isRunning;

    public ServerApp() {
        this.isRunning = false;
    }

    public void start() {
        try {
            System.out.println("========================================");
            System.out.println("Network Simulation Game - Server");
            System.out.println("========================================");

            // Initialize database connection
            initializeDatabase();

            // Initialize exception handling
            initializeExceptionHandling();

            // Get server port from user or use default
            int port = getServerPort();

            // Create and start the game server
            gameServer = new GameServer(port);

            System.out.println("Starting server on port " + port + "...");
            System.out.println("Server will accept client connections and manage multiplayer sessions.");
            System.out.println("Press 'q' and Enter to quit the server.");
            System.out.println("========================================");

            isRunning = true;

            // Start server in a separate thread
            Thread serverThread = new Thread(() -> {
                try {
                    gameServer.start();
                } catch (Exception e) {
                    System.err.println("Server error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            // Monitor for quit command
            monitorForQuit();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void initializeDatabase() {
        try {
            System.out.println("Initializing database connection...");
            boolean connected = HibernateUtil.testConnection();
            if (connected) {
                System.out.println("✓ Database connection established successfully");
            } else {
                System.out.println("⚠ Database connection failed - using fallback storage");
            }
        } catch (Exception e) {
            System.out.println("⚠ Database initialization failed: " + e.getMessage());
            System.out.println("   Server will use fallback storage methods");
        }
    }

    private void initializeExceptionHandling() {
        try {
            System.out.println("Initializing exception handling system...");
            GlobalExceptionHandler.getInstance();
            System.out.println("✓ Exception handling system initialized");
        } catch (Exception e) {
            System.out.println("⚠ Exception handling initialization failed: " + e.getMessage());
        }
    }

    private int getServerPort() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter server port (default " + DEFAULT_PORT + "): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
            return DEFAULT_PORT;
        }

        try {
            int port = Integer.parseInt(input);
            if (port < 1 || port > 65535) {
                System.out.println("Invalid port number. Using default port " + DEFAULT_PORT);
                return DEFAULT_PORT;
            }
            return port;
        } catch (NumberFormatException e) {
            System.out.println("Invalid port format. Using default port " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    private void monitorForQuit() {
        Scanner scanner = new Scanner(System.in);

        while (isRunning) {
            try {
                String input = scanner.nextLine().trim().toLowerCase();

                if ("q".equals(input) || "quit".equals(input) || "exit".equals(input)) {
                    System.out.println("Shutting down server...");
                    stop();
                    break;
                } else if ("status".equals(input) || "s".equals(input)) {
                    showServerStatus();
                } else if ("help".equals(input) || "h".equals(input)) {
                    showHelp();
                } else if (!input.isEmpty()) {
                    System.out.println("Unknown command. Type 'help' for available commands.");
                }

            } catch (Exception e) {
                System.err.println("Error reading input: " + e.getMessage());
            }
        }
    }

    private void showServerStatus() {
        if (gameServer != null) {
            System.out.println("========================================");
            System.out.println("Server Status:");
            System.out.println("  Running: " + (gameServer.isRunning() ? "Yes" : "No"));
            System.out.println("  Connected Clients: " + gameServer.getConnectedClientCount());
            System.out.println("  Active Sessions: " + gameServer.getActiveSessionCount());
            System.out.println("========================================");
        } else {
            System.out.println("Server is not running.");
        }
    }

    private void showHelp() {
        System.out.println("========================================");
        System.out.println("Available Commands:");
        System.out.println("  q, quit, exit  - Shutdown the server");
        System.out.println("  s, status      - Show server status");
        System.out.println("  h, help        - Show this help");
        System.out.println("========================================");
    }

    public void stop() {
        isRunning = false;

        if (gameServer != null) {
            System.out.println("Stopping game server...");
            gameServer.stop();
        }

        System.out.println("Closing database connections...");
        HibernateUtil.shutdown();

        System.out.println("Server shutdown complete.");
        System.exit(0);
    }

    public static void main(String[] args) {
        // Handle command line arguments
        if (args.length > 0) {
            if ("--help".equals(args[0]) || "-h".equals(args[0])) {
                System.out.println("Network Simulation Game - Server");
                System.out.println("Usage: java -cp target/classes server.ServerApp [options]");
                System.out.println("Options:");
                System.out.println("  --help, -h    Show this help message");
                System.out.println("  --port PORT   Set server port (default: 8081)");
                System.exit(0);
            }
        }

        // Create and start the server application
        ServerApp serverApp = new ServerApp();

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received...");
            serverApp.stop();
        }));

        serverApp.start();
    }
}


