package server;

import network.NetworkMessage;
import reflection.PacketReflectionManager;
import model.GameState;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MultiplayerSession {
    private String sessionId;
    private String player1Id;
    private String player2Id;
    private boolean isActive;
    private GameServer server;

    // Game state
    private GameState gameState;
    private Map<String, PlayerInfo> players;
    private long startTime;
    private long lastUpdateTime;

    // Multiplayer specific state
    private boolean player1Ready;
    private boolean player2Ready;
    private boolean gameStarted;
    private boolean setupPhaseStarted;
    private long setupPhaseStartTime;

    // Timer constants for the 30+30 second system
    private static final long INITIAL_SETUP_DURATION = 30000; // 30 seconds - normal setup
    private static final long PENALTY_PHASE_1_DURATION = 10000; // 10 seconds - Wrath of Penia (packet injection)
    private static final long PENALTY_PHASE_2_DURATION = 10000; // 10 seconds - Wrath of Aergia (cooldown increase)
    private static final long PENALTY_PHASE_3_DURATION = 10000; // 10 seconds - Wrath of Penia (speed increase)
    private static final long TOTAL_SETUP_DURATION = INITIAL_SETUP_DURATION + PENALTY_PHASE_1_DURATION + PENALTY_PHASE_2_DURATION + PENALTY_PHASE_3_DURATION;

    // Penalty system tracking
    private long lastPenaltyActionTime = 0;
    private double currentCooldownMultiplier = 1.0;
    private double currentSpeedMultiplier = 1.0;
    private PacketReflectionManager packetManager;

    public MultiplayerSession(String sessionId, String player1Id, String player2Id) {
        this.sessionId = sessionId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.isActive = true;
        this.gameStarted = false;
        this.setupPhaseStarted = false;
        this.startTime = System.currentTimeMillis();
        this.lastUpdateTime = System.currentTimeMillis();

        this.players = new ConcurrentHashMap<>();
        this.players.put(player1Id, new PlayerInfo(player1Id, "Player 1"));
        if (player2Id != null) {
            this.players.put(player2Id, new PlayerInfo(player2Id, "Player 2"));
        }

        // Start setup phase immediately when the session is created
        // This allows single-player setup and timer countdown to begin right away
        startSetupPhase();

        this.gameState = new GameState();
        this.packetManager = new PacketReflectionManager();
    }

    public void setPlayer2Id(String player2Id) {
        this.player2Id = player2Id;
        if (players.containsKey(player2Id)) {
            players.get(player2Id).setPlayerName("Player 2");
        } else {
            players.put(player2Id, new PlayerInfo(player2Id, "Player 2"));
        }

        // Start setup phase when second player joins
        if (!setupPhaseStarted) {
            startSetupPhase();
        }
    }

    private void startSetupPhase() {
        this.setupPhaseStarted = true;
        this.setupPhaseStartTime = System.currentTimeMillis();

        System.out.println("🎯 SETUP PHASE STARTED - Session: " + sessionId);
    }

    public long getRemainingSetupTime() {
        if (!setupPhaseStarted || gameStarted) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - setupPhaseStartTime;
        long remaining = TOTAL_SETUP_DURATION - elapsed;
        return Math.max(0, remaining);
    }

    public int getCurrentPenaltyPhase() {
        if (!setupPhaseStarted || gameStarted) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - setupPhaseStartTime;
        int previousPhase = currentPenaltyPhase;

        int currentPhase;
        if (elapsed < INITIAL_SETUP_DURATION) {
            currentPhase = 0; // Normal setup phase
        } else if (elapsed < INITIAL_SETUP_DURATION + PENALTY_PHASE_1_DURATION) {
            currentPhase = 1; // Wrath of Penia - packet injection
        } else if (elapsed < INITIAL_SETUP_DURATION + PENALTY_PHASE_1_DURATION + PENALTY_PHASE_2_DURATION) {
            currentPhase = 2; // Wrath of Aergia - cooldown increase
        } else {
            currentPhase = 3; // Wrath of Penia - speed increase
        }

        // Log phase transitions
        if (currentPhase != previousPhase) {
            currentPenaltyPhase = currentPhase;
            if (currentPhase > 0) {
                System.out.println("🔄 PHASE " + currentPhase + " - " + getCurrentPhaseDescription());
            }
        }

        return currentPhase;
    }

    // Track current phase for transition detection
    private int currentPenaltyPhase = 0;

    public String getCurrentPhaseDescription() {
        switch (getCurrentPenaltyPhase()) {
            case 0: return "Setup Phase";
            case 1: return "Wrath of Penia - Packet Injection";
            case 2: return "Wrath of Aergia - Cooldown Penalty";
            case 3: return "Wrath of Penia - Speed Penalty";
            default: return "Unknown Phase";
        }
    }

    public boolean isSetupPhase() {
        return setupPhaseStarted && !gameStarted;
    }

    // Timer for periodic logging
    private long lastLogTime = 0;
    private static final long LOG_INTERVAL = 5000; // Log every 5 seconds

    public void update() {
        if (!isActive) return;

        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // Send game state updates during setup phase (for timer countdown)
        if (setupPhaseStarted && !gameStarted) {
            // Periodic logging every 15 seconds to reduce noise
            if (currentTime - lastLogTime >= 15000) {
                long remaining = (TOTAL_SETUP_DURATION - (currentTime - setupPhaseStartTime)) / 1000;
                if (remaining > 0) {
                    System.out.println("⏰ SETUP: " + remaining + "s remaining - P1:" + player1Ready + " P2:" + player2Ready);
                }
                lastLogTime = currentTime;
            }

            sendGameStateUpdate();

            // Apply penalty effects based on current phase
            applyPenaltyEffects(currentTime);

            // Check if setup phase should end (after all penalty phases)
            if (currentTime >= setupPhaseStartTime + TOTAL_SETUP_DURATION) {
                System.out.println("⏰ SETUP PHASE TIMEOUT - Maximum time reached!");
                endSetupPhase();
                setupPhaseStarted = false; // Prevent repeated calls
            }
        }

        // Update game state if game is running
        if (gameStarted) {
            updateGameState(deltaTime);
        }
    }

    private void endSetupPhase() {
        // Start the game regardless of ready status when setup phase expires
        gameStarted = true;

        if (player1Ready && player2Ready) {
            System.out.println("🎯 GAME STARTING - Both players ready!");
        } else {
            System.out.println("🎯 GAME STARTING - Setup time expired (P1:" + player1Ready + " P2:" + player2Ready + ")");
        }

        // Send immediate game state update to notify clients that game has started
        sendGameStateUpdate();

        // Notify both players that the game is starting
        NetworkMessage startMessage = new NetworkMessage(
                NetworkMessage.MessageType.GAME_STATE_UPDATE,
                "SERVER",
                sessionId,
                "Game starting!"
        );

        if (server != null) {
            server.sendToClient(player1Id, startMessage);
            server.sendToClient(player2Id, startMessage);
        }
    }

    private void updateGameState(long deltaTime) {
        if (gameState == null) return;

        // Update packet movements
        updatePacketMovements(deltaTime);

        // Check for collisions
        checkCollisions();

        // Update player scores
        updatePlayerScores();

        // Check for game over conditions
        checkGameOverConditions();

        // Send game state updates to both players
        sendGameStateUpdate();
    }

    private void updatePacketMovements(long deltaTime) {
        // This would update all active packets in the game state
        // For now, we'll simulate packet movement
        if (gameState.getActivePackets() != null) {
            for (model.Packet packet : gameState.getActivePackets()) {
                if (packet != null && packet.getCurrentPosition() != null && packet.getMovementVector() != null) {
                    // Update packet position based on movement vector and delta time
                    double deltaSeconds = deltaTime / 1000.0;
                    double speedFactor = (setupPhaseStarted && !gameStarted) ? currentSpeedMultiplier : 1.0;
                    double newX = packet.getCurrentPosition().getX() + packet.getMovementVector().getX() * deltaSeconds * speedFactor;
                    double newY = packet.getCurrentPosition().getY() + packet.getMovementVector().getY() * deltaSeconds * speedFactor;

                    packet.setCurrentPosition(new model.Point2D(newX, newY));
                }
            }
        }
    }

    private void checkCollisions() {
        // Simplified collision detection
        // In a real implementation, this would check for packet-to-packet collisions
        // and packet-to-destination collisions
        if (gameState.getActivePackets() != null) {
            // Remove packets that are outside game bounds
            gameState.getActivePackets().removeIf(packet -> {
                if (packet == null || packet.getCurrentPosition() == null) return true;

                double x = packet.getCurrentPosition().getX();
                double y = packet.getCurrentPosition().getY();

                // Remove packets outside game area
                return x < 0 || x > 1400 || y < 0 || y > 900;
            });
        }
    }


    private void updatePlayerScores() {
        // Simplified score update logic
        // In a real implementation, this would check for successful packet deliveries
        // and update player scores accordingly

        // For now, we'll just maintain the existing scores
        // This would be called when packets reach their destinations
    }

    private void checkGameOverConditions() {
        // Check if game should end based on time or score conditions
        long gameDuration = System.currentTimeMillis() - startTime;

        // End game after 5 minutes
        if (gameDuration > 300000) { // 5 minutes
            endSession();
        }

        // Check for score-based game over (first to 50 points wins)
        PlayerInfo player1 = players.get(player1Id);
        PlayerInfo player2 = players.get(player2Id);

        if (player1 != null && player1.getScore() >= 50) {
            endSession();
        } else if (player2 != null && player2.getScore() >= 50) {
            endSession();
        }
    }

    private void applyPenaltyEffects(long currentTime) {
        int penaltyPhase = getCurrentPenaltyPhase();

        switch (penaltyPhase) {
            case 1: // Wrath of Penia - Packet injection every 2 seconds
                if (currentTime - lastPenaltyActionTime >= 2000) {
                    applyPacketInjectionPenalty();
                    lastPenaltyActionTime = currentTime;
                }
                break;

            case 2: // Wrath of Aergia - Cooldown increase by 1% per second
                long elapsedInPhase2 = currentTime - (setupPhaseStartTime + INITIAL_SETUP_DURATION + PENALTY_PHASE_1_DURATION);
                double newCooldownMultiplier = 1.0 + (elapsedInPhase2 / 1000.0 * 0.01);
                currentCooldownMultiplier = newCooldownMultiplier;
                break;

            case 3: // Wrath of Penia - Speed increase by 3% per second
                long elapsedInPhase3 = currentTime - (setupPhaseStartTime + INITIAL_SETUP_DURATION + PENALTY_PHASE_1_DURATION + PENALTY_PHASE_2_DURATION);
                double newSpeedMultiplier = 1.0 + (elapsedInPhase3 / 1000.0 * 0.03);
                currentSpeedMultiplier = newSpeedMultiplier;
                break;
        }
    }

    private void applyPacketInjectionPenalty() {
        if (gameState == null) {
            gameState = new model.GameState();
        }

        // Decide target: penalize the player who has not confirmed readiness (if known), else random
        String targetPlayerId = (!player1Ready && player1Id != null) ? player1Id
                : (!player2Ready && player2Id != null) ? player2Id
                : (Math.random() < 0.5 ? player1Id : player2Id);

        // Create a simple random packet near the opponent side heading inward
        model.PacketType[] candidateTypes = {
                model.PacketType.SMALL_MESSENGER,
                model.PacketType.PROTECTED,
                model.PacketType.BULK_SMALL,
                model.PacketType.TROJAN,
                model.PacketType.BIT_PACKET
        };

        model.PacketType chosen = candidateTypes[(int)(Math.random() * candidateTypes.length)];

        // Position and velocity based on target side
        double startX = (targetPlayerId != null && targetPlayerId.equals(player1Id)) ? 1200.0 : 200.0;
        double dir = (targetPlayerId != null && targetPlayerId.equals(player1Id)) ? -1.0 : 1.0;
        double startY = 200.0 + Math.random() * 400.0;
        model.Point2D pos = new model.Point2D(startX, startY);
        model.Vec2D vec = new model.Vec2D(60.0 * dir, 0.0);

        // Construct a packet via reflection-friendly default constructor path
        model.Packet packet = packetManager.createPacket(chosen, pos, vec);
        if (packet != null) {
            packet.setOwnerId(targetPlayerId);
            gameState.addActivePacket(packet);
        }

        // Notify clients for UI feedback
        java.util.Map<String, Object> penaltyData = new java.util.LinkedHashMap<>();
        penaltyData.put("type", "PACKET_INJECTION");
        penaltyData.put("message", "Wrath of Penia: Random packet injected!");
        penaltyData.put("timestamp", System.currentTimeMillis());
        penaltyData.put("target", targetPlayerId);
        penaltyData.put("packetType", chosen.toString());
        penaltyData.put("position", new double[]{startX, startY});

        NetworkMessage penaltyMessage = new NetworkMessage(
                NetworkMessage.MessageType.PLAYER_ACTION,
                "SERVER",
                sessionId,
                penaltyData
        );

        if (server != null) {
            server.sendToClient(player1Id, penaltyMessage);
            if (player2Id != null) server.sendToClient(player2Id, penaltyMessage);
        }
    }

    private void sendGameStateUpdate() {
        // Create game state data as LinkedHashMap for compatibility with client
        java.util.Map<String, Object> gameStateData = new java.util.LinkedHashMap<>();
        long remainingTime = getRemainingSetupTime();
        gameStateData.put("remainingSetupTime", remainingTime);
        gameStateData.put("setupPhase", isSetupPhase());
        gameStateData.put("gameStarted", gameStarted);
        gameStateData.put("penaltyPhase", getCurrentPenaltyPhase());
        gameStateData.put("phaseDescription", getCurrentPhaseDescription());
        gameStateData.put("cooldownMultiplier", currentCooldownMultiplier);
        gameStateData.put("speedMultiplier", currentSpeedMultiplier);
        gameStateData.put("player1State", null); // would need to be implemented
        gameStateData.put("player2State", null); // would need to be implemented
        gameStateData.put("controllableSystems", null); // would need to be implemented
        gameStateData.put("cooldowns", null); // would need to be implemented
        gameStateData.put("timestamp", System.currentTimeMillis());

        NetworkMessage updateMessage = new NetworkMessage(
                NetworkMessage.MessageType.GAME_STATE_UPDATE,
                "SERVER",
                sessionId,
                gameStateData
        );

        if (server != null) {
            server.sendToClient(player1Id, updateMessage);
            if (player2Id != null) {
                server.sendToClient(player2Id, updateMessage);
            }
        }
    }

    public void setPlayerReady(String playerId) {
        if (playerId.equals(player1Id)) {
            player1Ready = true;
            System.out.println("✅ PLAYER 1 READY");
        } else if (playerId.equals(player2Id)) {
            player2Ready = true;
            System.out.println("✅ PLAYER 2 READY");
        } else {
            System.out.println("❌ UNKNOWN PLAYER: " + playerId);
            return;
        }

        // Check if both players are ready
        if (player1Ready && player2Ready) {
            System.out.println("🎯 BOTH PLAYERS READY - Starting game!");
            endSetupPhase();
        }
    }

    public void handlePlayerAction(String playerId, Object actionData) {
        if (!gameStarted) return;

        // Process the player action
        // This would involve updating the game state based on the action

        // Notify the other player about the action
        String otherPlayerId = playerId.equals(player1Id) ? player2Id : player1Id;

        NetworkMessage actionMessage = new NetworkMessage(
                NetworkMessage.MessageType.PLAYER_ACTION,
                playerId,
                sessionId,
                actionData
        );

        server.sendToClient(otherPlayerId, actionMessage);
    }

    public void endSession() {
        isActive = false;

        // Send game over message to both players
        NetworkMessage gameOverMessage = new NetworkMessage(
                NetworkMessage.MessageType.GAME_OVER,
                "SERVER",
                sessionId,
                "Game session ended"
        );

        server.sendToClient(player1Id, gameOverMessage);
        server.sendToClient(player2Id, gameOverMessage);

        // Remove session from server
        server.removeSession(sessionId);
    }

    /**
     * Sets the server reference for this session.
     */
    public void setServer(GameServer server) {
        this.server = server;
    }

    public String getOtherPlayerId(String playerId) {
        if (playerId.equals(player1Id)) {
            return player2Id;
        } else if (playerId.equals(player2Id)) {
            return player1Id;
        }
        return null;
    }

    public PlayerInfo getPlayer1State() {
        return players.get(player1Id);
    }


    public PlayerInfo getPlayer2State() {
        return players.get(player2Id);
    }

    public boolean canPlayerJoin(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return false;
        }

        // Session must be active
        if (!isActive) {
            return false;
        }

        // Check if game has already started (no new players allowed)
        if (gameStarted) {
            return false;
        }

        // Check if player is already in the session
        if (playerId.equals(player1Id)) {
            return false; // Player 1 already in session
        }

        // Check if there's space for player 2
        if (player2Id == null || player2Id.trim().isEmpty()) {
            return true; // Player 2 slot is available
        }

        // Check if this player is the designated player 2
        if (playerId.equals(player2Id)) {
            return true; // Player 2 can rejoin their own session
        }

        // Session is full
        return false;
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public String getPlayer1Id() {
        return player1Id;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public long getStartTime() {
        return startTime;
    }

    public static class PlayerInfo {
        private String id;
        private String name;
        private int score;
        private boolean isReady;

        public PlayerInfo(String id, String name) {
            this.id = id;
            this.name = name;
            this.score = 0;
            this.isReady = false;
        }

        // Getters and setters
        public String getId() { return id; }
        public String getName() { return name; }
        public void setPlayerName(String name) { this.name = name; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public boolean isReady() { return isReady; }
        public void setReady(boolean ready) { isReady = ready; }
    }
}


