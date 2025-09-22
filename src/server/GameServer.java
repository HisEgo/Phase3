package server;

import network.NetworkMessage;

import security.DataIntegrityValidator;
import exception.ExceptionHandlerAnnotation;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int DEFAULT_PORT = 8081;
    private static final int MAX_CLIENTS = 10;

    private ServerSocket serverSocket;
    private int port;
    private boolean isRunning;


    // Client management
    private Map<String, ClientHandler> connectedClients;
    private Map<String, MultiplayerSession> activeSessions;

    // Phase 3 components
    private UserManager userManager;
    private OfflineDataHandler offlineDataHandler;
    private DataIntegrityValidator dataValidator;
    private DatabaseManager databaseManager;

    // Thread pools
    private ExecutorService clientHandlerPool;
    private ScheduledExecutorService gameUpdatePool;

    public GameServer() {
        this(DEFAULT_PORT);
    }

    public GameServer(int port) {
        this.port = port;
        this.isRunning = false;

        this.connectedClients = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();

        // Initialize Phase 3 components
        this.userManager = new UserManager();
        this.offlineDataHandler = new OfflineDataHandler();
        this.dataValidator = new DataIntegrityValidator();
        this.databaseManager = new DatabaseManager();

        this.clientHandlerPool = Executors.newCachedThreadPool();
        this.gameUpdatePool = Executors.newScheduledThreadPool(2);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;

            System.out.println("Game Server started on port " + port);
            System.out.println("Waiting for client connections...");

            // Start game update scheduler
            startGameUpdateScheduler();

            // Accept client connections
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (connectedClients.size() < MAX_CLIENTS) {
                        handleNewClient(clientSocket);
                    } else {
                        // Reject connection if at capacity
                        clientSocket.close();
                    }
                } catch (Exception e) {
                    if (isRunning) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;

        // Close all client connections
        for (ClientHandler client : connectedClients.values()) {
            client.disconnect();
        }
        connectedClients.clear();

        // Close all sessions
        activeSessions.clear();

        // Shutdown thread pools
        clientHandlerPool.shutdown();
        gameUpdatePool.shutdown();

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
            // Log error
        }

        System.out.println("Game Server stopped");
    }

    private void handleNewClient(Socket clientSocket) {

        // Identify user using MAC address (Phase 3 requirement)
        String userId = userManager.identifyUser(clientSocket);
        ClientHandler clientHandler = new ClientHandler(userId, clientSocket, this);

        connectedClients.put(userId, clientHandler);
        clientHandlerPool.submit(clientHandler);

        // Set connection status for offline data handler
        offlineDataHandler.setConnectionStatus(true);

        System.out.println("New client connected: " + userId + " (MAC-based identification)");
    }

    private void startGameUpdateScheduler() {
        gameUpdatePool.scheduleAtFixedRate(() -> {
            for (MultiplayerSession session : activeSessions.values()) {
                if (session.isActive()) {
                    session.update();
                }
            }
        }, 0, 50, TimeUnit.MILLISECONDS); // 20 FPS updates
    }


    public void removeClient(String clientId) {
        ClientHandler client = connectedClients.remove(clientId);
        if (client != null) {
            System.out.println("Client disconnected: " + clientId);
        }
    }


    public String createMultiplayerSession(String player1Id, String player2Id) {
        String sessionId = generateSessionId();
        MultiplayerSession session = new MultiplayerSession(sessionId, player1Id, player2Id);
        session.setServer(this); // Set the server reference
        activeSessions.put(sessionId, session);

        System.out.println("Created multiplayer session: " + sessionId);
        return sessionId;
    }


    public String createMultiplayerSessionWithId(String sessionId, String player1Id, String player2Id) {
        MultiplayerSession session = new MultiplayerSession(sessionId, player1Id, player2Id);
        session.setServer(this); // Set the server reference
        activeSessions.put(sessionId, session);

        System.out.println("✅ SERVER: Session created: " + sessionId);
        return sessionId;
    }


    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + new Random().nextInt(1000);
    }

    public MultiplayerSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    public MultiplayerSession findWaitingSessionById(String gameId) {

        MultiplayerSession session = activeSessions.get(gameId);

        if (session != null) {
            boolean isWaiting = (session.getPlayer2Id() == null || session.getPlayer2Id().isEmpty());

            if (isWaiting) {
                return session;
            }
        }
        return null;
    }

    public void removeSession(String sessionId) {
        MultiplayerSession session = activeSessions.remove(sessionId);
        if (session != null) {
            System.out.println("Removed session: " + sessionId);
        }
    }

    public void broadcastMessage(NetworkMessage message) {
        for (ClientHandler client : connectedClients.values()) {
            client.sendMessage(message);
        }
    }

    public Map<String, ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    public Map<String, MultiplayerSession> getActiveSessions() {
        return activeSessions;
    }


    public void sendToClient(String clientId, NetworkMessage message) {
        ClientHandler client = connectedClients.get(clientId);
        if (client != null) {
            client.sendMessage(message);
        }
    }

    // Getters
    public boolean isRunning() {
        return isRunning;
    }

    public int getConnectedClientCount() {
        return connectedClients.size();
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    // Phase 3 component getters
    public UserManager getUserManager() {
        return userManager;
    }

    public OfflineDataHandler getOfflineDataHandler() {
        return offlineDataHandler;
    }

    public DataIntegrityValidator getDataValidator() {
        return dataValidator;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }


    @ExceptionHandlerAnnotation(level = "WARNING", message = "IO error in game server")
    public static void handleIOException(IOException e, Object context) {
        System.err.println("IO Error in GameServer: " + e.getMessage());
        // Could implement retry logic or graceful degradation here
    }

    @ExceptionHandlerAnnotation(level = "INFO", message = "Socket error in game server")
    public static void handleSocketException(java.net.SocketException e, Object context) {
        System.err.println("Socket Error in GameServer: " + e.getMessage());
        // Could implement reconnection logic here
    }

    @ExceptionHandlerAnnotation(level = "SEVERE", message = "Unexpected error in game server")
    public static void handleGeneralException(Exception e, Object context) {
        System.err.println("Unexpected Error in GameServer: " + e.getMessage());
        e.printStackTrace();
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + DEFAULT_PORT);
            }
        }

        GameServer server = new GameServer(port);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}



