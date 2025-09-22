package multiplayer;

import model.*;
import network.NetworkManager;
import network.NetworkMessage;
import reflection.PacketReflectionManager;
import javafx.animation.AnimationTimer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class MultiplayerGameController {
    private static final long SETUP_PHASE_DURATION = 30000; // 30 seconds


    private String player1Id;
    private String player2Id;
    private String sessionId;
    private NetworkManager networkManager;
    private PacketReflectionManager packetManager;

    // Game state
    private GameState gameState;
    private boolean isSetupPhase;
    private boolean isGameStarted;
    private long setupPhaseStartTime;
    private long currentTime;

    // Network visibility system
    private model.GameLevel player1Network;
    private model.GameLevel player2Network;
    private boolean networkVisibilityEnabled;

    // Current player identification
    private String currentPlayerId;

    // Reference to the game view for UI updates
    private view.MultiplayerGameView gameView;

    // Player states
    private PlayerState player1State;
    private PlayerState player2State;
    private boolean player1Ready;
    private boolean player2Ready;
    // Server-driven penalty multipliers
    private double serverCooldownMultiplier = 1.0;
    private double serverSpeedMultiplier = 1.0;

    // Controllable reference systems
    private Map<String, ControllableSystem> controllableSystems;
    private Map<String, SystemCooldown> systemCooldowns;
    private Map<String, PacketCooldown> packetCooldowns;

    // Targeting system
    private String selectedTargetSystemId;
    private Point2D selectedTargetPosition;
    private boolean isTargetingMode;

    // Game loop
    private AnimationTimer gameLoop;
    private boolean isRunning;

    public MultiplayerGameController(String player1Id, String player2Id, String sessionId, NetworkManager networkManager) {
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.sessionId = sessionId;
        this.networkManager = networkManager;
        this.packetManager = new PacketReflectionManager();

        // Determine current player ID based on which player this controller represents
        // For now, we'll use player1Id as the default, but this should be determined by the actual player context
        this.currentPlayerId = player1Id;

        this.gameState = new GameState();
        this.isSetupPhase = true;
        this.isGameStarted = false;
        this.setupPhaseStartTime = java.lang.System.currentTimeMillis();
        this.currentTime = 0;

        // Initialize network visibility system
        this.player1Network = null;
        this.player2Network = null;
        this.networkVisibilityEnabled = false;

        this.player1State = new PlayerState(player1Id, "Player 1");
        this.player2State = new PlayerState(player2Id, "Player 2");
        this.player1Ready = false;
        this.player2Ready = false;

        this.controllableSystems = new ConcurrentHashMap<>();
        this.systemCooldowns = new ConcurrentHashMap<>();
        this.packetCooldowns = new ConcurrentHashMap<>();

        // Set up network message handling for real multiplayer
        setupNetworkMessageHandling();

        initializeControllableSystems();
        initializeCooldowns();
        startGameLoop();
    }

    private void setupNetworkMessageHandling() {
        // Message callback is now handled by MainApp.handleMultiplayerMessage()
        // which forwards GAME_STATE_UPDATE messages to this controller via updateGameState()
    }

    public void updateGameState(Object gameStateData) {
        // Handle game state updates from the server
        if (gameStateData != null) {

            // Handle MultiplayerGameState object
            if (gameStateData instanceof multiplayer.MultiplayerGameState) {
                multiplayer.MultiplayerGameState state =
                        (multiplayer.MultiplayerGameState) gameStateData;

                // Update setup phase timer from server
                long remainingTime = state.getRemainingSetupTime();
                updateSetupTimer(remainingTime);

                // Update setup phase status
                boolean isSetupPhase = state.isSetupPhase();
                if (isSetupPhase != this.isSetupPhase) {
                    this.isSetupPhase = isSetupPhase;
                    if (!isSetupPhase) {
                        // Setup phase ended, start the game
                        startGame();
                    }
                }

                // Update game started status
                boolean gameStarted = state.isGameStarted();
                if (gameStarted != this.isGameStarted) {
                    this.isGameStarted = gameStarted;
                }
            }
            // Handle different types of game state data (legacy support)
            else if (gameStateData instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> stateMap = (java.util.Map<String, Object>) gameStateData;

                // Update setup phase timer from server
                if (stateMap.containsKey("remainingSetupTime")) {
                    Object remainingTimeObj = stateMap.get("remainingSetupTime");
                    if (remainingTimeObj instanceof Number) {
                        long remainingTime = ((Number) remainingTimeObj).longValue();

                        // Extract penalty phase information
                        int penaltyPhase = 0;
                        String phaseDescription = "Setup Phase";

                        if (stateMap.containsKey("penaltyPhase") && stateMap.get("penaltyPhase") instanceof Number) {
                            penaltyPhase = ((Number) stateMap.get("penaltyPhase")).intValue();
                        }

                        if (stateMap.containsKey("phaseDescription") && stateMap.get("phaseDescription") instanceof String) {
                            phaseDescription = (String) stateMap.get("phaseDescription");
                        }

                        updateSetupTimer(remainingTime, penaltyPhase, phaseDescription);
                    }
                }

                // Update setup phase status
                if (stateMap.containsKey("setupPhase")) {
                    Object setupPhaseObj = stateMap.get("setupPhase");
                    if (setupPhaseObj instanceof Boolean) {
                        boolean isSetupPhase = (Boolean) setupPhaseObj;
                        if (isSetupPhase != this.isSetupPhase) {
                            this.isSetupPhase = isSetupPhase;
                            if (!isSetupPhase) {
                                // Setup phase ended, start the game
                                startGame();
                            }
                        }
                    }
                }

                // Update game started status
                if (stateMap.containsKey("gameStarted")) {
                    Object gameStartedObj = stateMap.get("gameStarted");
                    if (gameStartedObj instanceof Boolean) {
                        boolean gameStarted = (Boolean) gameStartedObj;
                        if (gameStarted != this.isGameStarted) {
                            this.isGameStarted = gameStarted;
                        }
                    }
                }
            }
        }
    }

    private void updateSetupTimer(long remainingTimeMs, int penaltyPhase, String phaseDescription) {
        // Convert milliseconds to seconds
        long remainingSeconds = remainingTimeMs / 1000;


        // Update the UI timer if we have a reference to the view
        if (gameView != null) {
            // Use Platform.runLater to ensure UI updates happen on the JavaFX thread
            javafx.application.Platform.runLater(() -> {
                if (gameView.getSetupTimerLabel() != null) {
                    // Format the timer display with phase information
                    String timerText = String.format("Time: %02d:%02d",
                            remainingSeconds / 60, remainingSeconds % 60);

                    // Add phase description for penalty phases
                    if (penaltyPhase > 0) {
                        timerText += " - " + phaseDescription;
                    }

                    gameView.getSetupTimerLabel().setText(timerText);


                    // Color coding based on penalty phase
                    String style = "-fx-font-weight: bold; ";
                    if (penaltyPhase == 3) { // Final penalty phase
                        style += "-fx-text-fill: darkred; -fx-font-size: 18px;";
                    } else if (penaltyPhase == 2) { // Cooldown penalty
                        style += "-fx-text-fill: orange; -fx-font-size: 17px;";
                    } else if (penaltyPhase == 1) { // Packet injection
                        style += "-fx-text-fill: red; -fx-font-size: 16px;";
                    } else if (remainingSeconds <= 10) { // Last 10 seconds of normal phase
                        style += "-fx-text-fill: yellow; -fx-font-size: 15px;";
                    } else {
                        style += "-fx-text-fill: white; -fx-font-size: 14px;";
                    }

                    gameView.getSetupTimerLabel().setStyle(style);

                }
            });
        }

    }

    // Overloaded method for backwards compatibility
    private void updateSetupTimer(long remainingTimeMs) {
        updateSetupTimer(remainingTimeMs, 0, "Setup Phase");
    }

    public void handleOpponentAction(Object actionData) {
        // Handle opponent's actions (packet releases, system usage, etc.)
        if (actionData != null) {
            // Process the opponent's action
            // This could include updating opponent's network state, ammunition usage, etc.
        }
    }


    private void handleIncomingNetworkMessage(network.NetworkMessage message) {
        try {
            if (message.getType() == network.NetworkMessage.MessageType.NETWORK_DATA) {
                // Handle dedicated network data messages
                if (message.getData() instanceof model.GameLevel) {
                    model.GameLevel opponentNetwork =
                            (model.GameLevel) message.getData();
                    receiveOpponentNetworkData(message.getPlayerId(), opponentNetwork);
                } else {
                    java.lang.System.out.println("NETWORK_HANDLE: Received NETWORK_DATA message with unexpected data type: " +
                            (message.getData() != null ? message.getData().getClass().getSimpleName() : "null"));
                }
            }
            else if (message.getType() == network.NetworkMessage.MessageType.GAME_STATE_UPDATE) {
                // Handle timer updates and game state
                if (message.getData() instanceof multiplayer.MultiplayerGameState) {
                    multiplayer.MultiplayerGameState gameState =
                            (multiplayer.MultiplayerGameState) message.getData();
                    updateGameState(gameState);
                }
                // Legacy: Handle network data from opponent (for backward compatibility)
                else if (message.getData() instanceof model.GameLevel) {
                    model.GameLevel opponentNetwork =
                            (model.GameLevel) message.getData();
                    receiveOpponentNetworkData(message.getPlayerId(), opponentNetwork);
                }
                // Handle LinkedHashMap data (legacy format)
                else if (message.getData() instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> stateMap = (java.util.Map<String, Object>) message.getData();

                    // Update setup phase timer from server
                    if (stateMap.containsKey("remainingSetupTime")) {
                        Object remainingTimeObj = stateMap.get("remainingSetupTime");
                        if (remainingTimeObj instanceof Number) {
                            long remainingTime = ((Number) remainingTimeObj).longValue();
                            updateSetupTimer(remainingTime);
                        }
                    }

                    // Update setup phase status
                    if (stateMap.containsKey("setupPhase")) {
                        Object setupPhaseObj = stateMap.get("setupPhase");
                        if (setupPhaseObj instanceof Boolean) {
                            boolean isSetupPhase = (Boolean) setupPhaseObj;
                            if (isSetupPhase != this.isSetupPhase) {
                                this.isSetupPhase = isSetupPhase;
                                if (!isSetupPhase) {
                                    // Setup phase ended, start the game
                                    startGame();
                                }
                            }
                        }
                    }

                    // Update game started status
                    if (stateMap.containsKey("gameStarted")) {
                        Object gameStartedObj = stateMap.get("gameStarted");
                        if (gameStartedObj instanceof Boolean) {
                            boolean gameStarted = (Boolean) gameStartedObj;
                            if (gameStarted != this.isGameStarted) {
                                this.isGameStarted = gameStarted;
                            }
                        }
                    }

                    // Consume server penalty multipliers (if provided)
                    if (stateMap.containsKey("cooldownMultiplier")) {
                        Object cm = stateMap.get("cooldownMultiplier");
                        if (cm instanceof Number) {
                            serverCooldownMultiplier = ((Number) cm).doubleValue();
                        }
                    }
                    if (stateMap.containsKey("speedMultiplier")) {
                        Object sm = stateMap.get("speedMultiplier");
                        if (sm instanceof Number) {
                            serverSpeedMultiplier = ((Number) sm).doubleValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            java.lang.System.err.println("Error handling incoming network message: " + e.getMessage());
        }
    }

    private void initializeControllableSystems() {
        // Create controllable systems with ammunition
        controllableSystems.put("system1", new ControllableSystem("system1", new Point2D(200, 200)));
        controllableSystems.put("system2", new ControllableSystem("system2", new Point2D(600, 200)));
        controllableSystems.put("system3", new ControllableSystem("system3", new Point2D(400, 400)));

        // Initialize ammunition for each system
        for (ControllableSystem system : controllableSystems.values()) {
            system.initializeAmmunition();
        }
    }

    private void initializeCooldowns() {
        // System cooldowns (shorter than packet cooldowns)
        for (String systemId : controllableSystems.keySet()) {
            systemCooldowns.put(systemId, new SystemCooldown(systemId, 15.0)); // 15 seconds
        }

        // Packet type cooldowns
        for (PacketType packetType : PacketType.values()) {
            if (isValidMultiplayerPacketType(packetType)) {
                packetCooldowns.put(packetType.name(), new PacketCooldown(packetType.name(), 30.0)); // 30 seconds
            }
        }
    }

    private boolean isValidMultiplayerPacketType(PacketType packetType) {
        // Confidential packets can only come from uncontrollable systems
        return !packetType.isConfidential();
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isRunning) {
                    update(now);
                }
            }
        };

        isRunning = true;
        gameLoop.start();
    }

    private void update(long now) {
        currentTime = java.lang.System.currentTimeMillis();

        if (isSetupPhase) {
            // Setup is server-driven; only UI updates come from server messages.
            // Remove client-side penalty triggers.
        } else if (isGameStarted) {
            updateGameplay();
        }

        // Update cooldowns
        updateCooldowns();

        // Note: Game state updates are now handled by the server only
        // Clients should not send continuous updates to avoid message loops
    }

    private void updateSetupPhase() { /* no-op: server authoritative */ }


    private void applyExtraTimePenalties(long extraTime) { /* removed client-side penalties */ }


    private void addRandomPacketToOpponent() {
        try {
            // Determine which player is the opponent (simplified logic)
            String opponentPlayerId = "player1".equals(player1Id) ? player2Id : player1Id;

            // Select a random controllable system
            List<String> systemIds = new ArrayList<>(controllableSystems.keySet());
            if (!systemIds.isEmpty()) {
                String randomSystemId = systemIds.get(new Random().nextInt(systemIds.size()));
                ControllableSystem system = controllableSystems.get(randomSystemId);

                if (system != null && system.hasAnyAmmunition()) {
                    // Select a random packet type that the system has ammunition for
                    Map<String, Integer> ammunition = system.getAmmunition();
                    List<String> availableTypes = new ArrayList<>();

                    for (Map.Entry<String, Integer> entry : ammunition.entrySet()) {
                        if (entry.getValue() > 0) {
                            availableTypes.add(entry.getKey());
                        }
                    }

                    if (!availableTypes.isEmpty()) {
                        String randomPacketType = availableTypes.get(new Random().nextInt(availableTypes.size()));

                        // Create a packet and add it to the game state
                        PacketType type = PacketType.valueOf(randomPacketType);
                        Point2D position = system.getPosition();

                        // Create movement vector towards the opponent's side
                        Vec2D movementVector = createMovementVector(opponentPlayerId, position);

                        // Create packet using reflection manager
                        Packet packet = packetManager.createPacket(type, position, movementVector);
                        if (packet != null) {
                            gameState.addActivePacket(packet);

                        }
                    }
                }
            }
        } catch (Exception e) {
            java.lang.System.err.println("Error adding random packet to opponent: " + e.getMessage());
        }
    }


    private void increasePlayerCooldowns(double percentage) {
        for (PacketCooldown cooldown : packetCooldowns.values()) {
            cooldown.increaseCooldown(percentage);
        }
    }

    private void increasePacketSpeeds(double percentage) {
        try {
            // Increase speed of all active packets
            if (gameState.getActivePackets() != null) {
                for (Packet packet : gameState.getActivePackets()) {
                    if (packet != null && packet.getMovementVector() != null) {
                        Vec2D currentVector = packet.getMovementVector();
                        double newX = currentVector.getX() * (1.0 + percentage);
                        double newY = currentVector.getY() * (1.0 + percentage);

                        packet.setMovementVector(new Vec2D(newX, newY));
                    }
                }
            }

        } catch (Exception e) {
            java.lang.System.err.println("Error increasing packet speeds: " + e.getMessage());
        }
    }

    private void updateGameplay() {
        // Update packet movements
        updatePacketMovements();

        // Process automatic wave generation from uncontrollable reference systems
        processAutomaticWaveGeneration();

        // Check for collisions
        checkCollisions();

        // Update scores
        updateScores();

        // Check for game over conditions
        checkGameOverConditions();
    }

    private void processAutomaticWaveGeneration() {
        try {
            // Generate waves from uncontrollable reference systems
            // In multiplayer, some reference systems are uncontrollable and release packets automatically

            // Check if it's time to generate a wave (every 3-5 seconds)
            long currentTime = java.lang.System.currentTimeMillis();
            long timeSinceLastWave = currentTime - getLastWaveTime();
            long waveInterval = 3000 + (long)(Math.random() * 2000); // 3-5 seconds

            if (timeSinceLastWave >= waveInterval) {
                generateMultiplayerWave();
                setLastWaveTime(currentTime);
            }

        } catch (Exception e) {
            java.lang.System.err.println("Error in automatic wave generation: " + e.getMessage());
        }
    }

    private void generateMultiplayerWave() {
        try {
            // Determine wave size (smaller for multiplayer balance)
            int waveSize = 2 + (int)(Math.random() * 3); // 2-4 packets

            // Create wave using reflection manager
            Point2D wavePosition = new Point2D(400, 100); // Center top position
            List<Packet> wave = packetManager.generatePacketWave(waveSize, wavePosition);

            // Add packets to game state with ownership tracking
            for (Packet packet : wave) {
                // Set packet ownership for proper feedback loop
                setPacketOwnership(packet);
                gameState.addActivePacket(packet);
            }


        } catch (Exception e) {
            java.lang.System.err.println("Error generating multiplayer wave: " + e.getMessage());
        }
    }

    private long getLastWaveTime() {
        // Simple implementation - could be stored in a field for more complex logic
        return lastWaveTime;
    }

    private void setLastWaveTime(long time) {
        this.lastWaveTime = time;
    }

    // Field to track last wave time
    private long lastWaveTime = 0;

    private void updatePacketMovements() {
        // Update all active packets in the game state
        for (Packet packet : gameState.getActivePackets()) {
            if (packet != null) {
                // Update packet position based on movement vector
                Point2D currentPos = packet.getCurrentPosition();
                Vec2D movement = packet.getMovementVector();

                if (currentPos != null && movement != null) {
                    // Calculate new position
                    double newX = currentPos.getX() + movement.getX() * 0.016; // 60 FPS
                    double newY = currentPos.getY() + movement.getY() * 0.016;

                    // Update packet position
                    packet.setCurrentPosition(new Point2D(newX, newY));
                }
            }
        }
    }

    private void checkCollisions() {
        // Check for packet-to-packet collisions
        List<Packet> packets = gameState.getActivePackets();
        List<Packet> packetsToRemove = new ArrayList<>();

        for (int i = 0; i < packets.size(); i++) {
            Packet packet1 = packets.get(i);
            if (packet1 == null) continue;

            for (int j = i + 1; j < packets.size(); j++) {
                Packet packet2 = packets.get(j);
                if (packet2 == null) continue;

                // Check if packets are colliding
                if (arePacketsColliding(packet1, packet2)) {
                    // Handle collision based on packet types
                    handlePacketCollision(packet1, packet2);

                    // Mark packets for removal if they should be destroyed
                    if (shouldDestroyPacket(packet1)) {
                        packetsToRemove.add(packet1);
                    }
                    if (shouldDestroyPacket(packet2)) {
                        packetsToRemove.add(packet2);
                    }
                }
            }
        }

        // Remove destroyed packets
        for (Packet packet : packetsToRemove) {
            gameState.removeActivePacket(packet);
        }
    }

    private boolean arePacketsColliding(Packet packet1, Packet packet2) {
        Point2D pos1 = packet1.getCurrentPosition();
        Point2D pos2 = packet2.getCurrentPosition();

        if (pos1 == null || pos2 == null) return false;

        double distance = Math.sqrt(
                Math.pow(pos1.getX() - pos2.getX(), 2) +
                        Math.pow(pos1.getY() - pos2.getY(), 2)
        );

        return distance < 20; // Collision radius
    }

    private void handlePacketCollision(Packet packet1, Packet packet2) {
        // Implement collision logic based on packet types
        // For now, just destroy both packets
    }

    private boolean shouldDestroyPacket(Packet packet) {
        // Simple logic: destroy packet if it's been active for too long
        // or if it's outside the game bounds
        Point2D pos = packet.getCurrentPosition();
        if (pos == null) return true;

        // Check if packet is outside game bounds
        return pos.getX() < 0 || pos.getX() > 1400 || pos.getY() < 0 || pos.getY() > 900;
    }

    private void updateScores() {
        // Count successful packet deliveries for each player
        int player1SuccessfulDeliveries = 0;
        int player2SuccessfulDeliveries = 0;
        int player1LostPackets = 0;
        int player2LostPackets = 0;

        // Check for packets that reached their destinations or were lost
        List<Packet> packetsToRemove = new ArrayList<>();

        for (Packet packet : gameState.getActivePackets()) {
            if (packet == null) continue;

            // Determine which player this packet belongs to (considering Trojan transformation)
            String packetOwner = determinePacketOwnerWithTrojanLogic(packet);

            // Check if packet reached a destination using enhanced detection
            if (hasPacketBeenDelivered(packet)) {
                if ("player1".equals(packetOwner)) {
                    player1SuccessfulDeliveries++;
                    player1State.recordSuccessfulDelivery();
                } else if ("player2".equals(packetOwner)) {
                    player2SuccessfulDeliveries++;
                    player2State.recordSuccessfulDelivery();
                }

                // Apply feedback loop - give ammunition to opponent
                applyFeedbackLoop(packetOwner, packet);

                // Remove the delivered packet
                packetsToRemove.add(packet);
            } else if (isPacketLost(packet)) {
                // Phase 3 requirement: Lost packets count as -1.5 against successful deliveries
                if ("player1".equals(packetOwner)) {
                    player1LostPackets++;
                    player1State.recordPacketLoss();
                } else if ("player2".equals(packetOwner)) {
                    player2LostPackets++;
                    player2State.recordPacketLoss();
                }

                // Remove the lost packet
                packetsToRemove.add(packet);
            }
        }

        // Remove delivered/lost packets
        for (Packet packet : packetsToRemove) {
            gameState.removeActivePacket(packet);
        }

        // Update ammunition based on successful deliveries
        if (player1SuccessfulDeliveries > 0) {
            giveAmmunitionToPlayer("player2", player1SuccessfulDeliveries);
        }
        if (player2SuccessfulDeliveries > 0) {
            giveAmmunitionToPlayer("player1", player2SuccessfulDeliveries);
        }

        // Log packet loss penalties for debugging
        if (player1LostPackets > 0 || player2LostPackets > 0) {
        }
    }

    private boolean isPacketLost(Packet packet) {
        if (packet == null) return false;

        // Check various loss conditions
        boolean isLost = packet.shouldBeLost() ||
                packet.shouldBeDestroyedByTime() ||
                packet.isLost() ||
                isPacketOutOfBounds(packet);

        return isLost;
    }

    private boolean isPacketOutOfBounds(Packet packet) {
        if (packet == null) return false;

        Point2D pos = packet.getCurrentPosition();
        if (pos == null) return true;

        // Check if packet is outside game bounds
        return pos.getX() < 0 || pos.getX() > 1400 || pos.getY() < 0 || pos.getY() > 900;
    }

    private boolean hasPacketReachedDestination(Packet packet) {
        // Check if packet is inactive (delivered by reference system)
        if (!packet.isActive()) {
            return true;
        }

        // Fallback: Simplified logic for packets that haven't been processed by systems
        Point2D pos = packet.getCurrentPosition();
        if (pos == null) return false;

        // Check if packet is near the destination area (right side of screen)
        return pos.getX() > 1200 && pos.getY() > 200 && pos.getY() < 700;
    }

    private boolean hasPacketBeenDelivered(Packet packet) {
        // Primary check: packet is inactive (processed by destination reference system)
        if (!packet.isActive()) {
            return true;
        }

        // Secondary check: packet position indicates delivery
        return hasPacketReachedDestination(packet);
    }

    private String determinePacketOwner(Packet packet) {
        // Check if packet has ownership information stored
        if (packet.getOwnerId() != null && !packet.getOwnerId().isEmpty()) {
            return packet.getOwnerId();
        }

        // Determine ownership based on packet type and position
        PacketType packetType = packet.getPacketType();
        Point2D position = packet.getCurrentPosition();

        if (position != null) {
            // Packets on the left side belong to player 1, right side to player 2
            if (position.getX() < 600) {
                return "player1";
            } else {
                return "player2";
            }
        }

        // Fallback: determine by packet type
        if (isPlayer1PacketType(packetType)) {
            return "player1";
        } else if (isPlayer2PacketType(packetType)) {
            return "player2";
        }

        // Default fallback
        return Math.random() < 0.5 ? "player1" : "player2";
    }

    private boolean isPlayer1PacketType(PacketType packetType) {
        // Type-one packets belong to player 1
        return packetType == PacketType.SMALL_MESSENGER ||
                packetType == PacketType.PROTECTED ||
                packetType == PacketType.TRIANGLE_MESSENGER ||
                packetType == PacketType.SQUARE_MESSENGER;
    }

    private boolean isPlayer2PacketType(PacketType packetType) {
        // Type-two packets belong to player 2
        return packetType == PacketType.BULK_SMALL ||
                packetType == PacketType.TROJAN ||
                packetType == PacketType.SQUARE_MESSENGER ||
                packetType == PacketType.BULK_LARGE;
    }

    private String determinePacketOwnerWithTrojanLogic(Packet packet) {
        PacketType currentType = packet.getPacketType();
        PacketType originalType = packet.getOriginalPacketType();

        // If packet has been transformed by Trojan logic, use original type for ownership
        if (originalType != null && !originalType.equals(currentType)) {

            if (isPlayer1PacketType(originalType)) {
                return "player1";
            } else if (isPlayer2PacketType(originalType)) {
                return "player2";
            }
        }

        // Use current type for ownership determination
        return determinePacketOwner(packet);
    }

    private void applyFeedbackLoop(String successfulPlayer, Packet packet) {
        String opponentPlayer = "player1".equals(successfulPlayer) ? "player2" : "player1";

        // Get the packet type
        PacketType packetType = packet.getPacketType();

        // Apply the feedback loop based on packet type
        applyEnhancedFeedbackLoop(successfulPlayer, packet);

    }

    private void applyEnhancedFeedbackLoop(String successfulPlayer, Packet packet) {
        String opponentPlayer = "player1".equals(successfulPlayer) ? "player2" : "player1";
        PacketType packetType = packet.getPacketType();

        // Phase 3 requirement: When a type-one packet successfully reaches its destination,
        // one unit of a type-two packet is added to the ammunition
        if (isPlayer1PacketType(packetType)) {
            // Type-one packet delivered -> give type-two ammunition to opponent
            giveSpecificAmmunitionToPlayer(opponentPlayer, getTypeTwoPacketType(), 1);
        } else if (isPlayer2PacketType(packetType)) {
            // Type-two packet delivered -> give type-one ammunition to opponent
            giveSpecificAmmunitionToPlayer(opponentPlayer, getTypeOnePacketType(), 1);
        } else {
            // Fallback: give general ammunition
            giveAmmunitionToPlayer(opponentPlayer, 1);
        }
    }

    private PacketType getTypeOnePacketType() {
        // Return a type-one packet type (belongs to player 1)
        return PacketType.SMALL_MESSENGER;
    }

    private PacketType getTypeTwoPacketType() {
        // Return a type-two packet type (belongs to player 2)
        return PacketType.BULK_SMALL;
    }

    private void giveSpecificAmmunitionToPlayer(String playerId, PacketType packetType, int amount) {
        // Add specific ammunition type to all controllable systems for the player
        for (ControllableSystem system : controllableSystems.values()) {
            String packetTypeName = packetType.name();
            system.addAmmunition(packetTypeName, amount);
        }

    }

    private void setPacketOwnership(Packet packet) {
        PacketType packetType = packet.getPacketType();

        // Determine ownership based on packet type
        if (isPlayer1PacketType(packetType)) {
            packet.setOwnerId("player1");
        } else if (isPlayer2PacketType(packetType)) {
            packet.setOwnerId("player2");
        } else {
            // Default ownership based on position
            Point2D position = packet.getCurrentPosition();
            if (position != null && position.getX() < 600) {
                packet.setOwnerId("player1");
            } else {
                packet.setOwnerId("player2");
            }
        }
    }

    public void setPlayerNetwork(String playerId, model.GameLevel network) {
        // Set network for player

        if ("Player 1".equalsIgnoreCase(playerId) || "Player1".equalsIgnoreCase(playerId) || player1Id.equalsIgnoreCase(playerId)) {
            this.player1Network = network;
        } else if ("Player 2".equalsIgnoreCase(playerId) || "Player2".equalsIgnoreCase(playerId) || player2Id.equalsIgnoreCase(playerId)) {
            this.player2Network = network;
        }

        // Send network data to the other player via network communication
        if (networkManager != null && networkManager.isConnected()) {
            sendNetworkDataToOpponent(playerId, network);
        }

        // Enable network visibility after both players have set their networks
        if (player1Network != null && player2Network != null) {
            enableNetworkVisibility();
        }
    }

    private void sendNetworkDataToOpponent(String playerId, model.GameLevel network) {
        try {
            // Send network data to opponent

            // Create a network message to send the network data using dedicated NETWORK_DATA type
            network.NetworkMessage message = new network.NetworkMessage(
                    network.NetworkMessage.MessageType.NETWORK_DATA,
                    playerId,
                    sessionId,
                    network
            );

            // Send the message via network manager
            boolean sent = networkManager.sendMessage(message);
            if (sent) {
                // Network data sent successfully
            } else {
                java.lang.System.err.println("NETWORK_SHARE: Failed to send network data to opponent for player: " + playerId);
            }

        } catch (Exception e) {
            java.lang.System.err.println("NETWORK_SHARE: Error sending network data to opponent: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void receiveOpponentNetworkData(String opponentPlayerId, model.GameLevel network) {
        try {
            // Receive network data from opponent

            // Set the opponent's network data
            if ("Player 1".equalsIgnoreCase(opponentPlayerId) || "Player1".equalsIgnoreCase(opponentPlayerId) || player1Id.equalsIgnoreCase(opponentPlayerId)) {
                this.player1Network = network;
            } else if ("Player 2".equalsIgnoreCase(opponentPlayerId) || "Player2".equalsIgnoreCase(opponentPlayerId) || player2Id.equalsIgnoreCase(opponentPlayerId)) {
                this.player2Network = network;
            }

            // Enable network visibility if both players have set their networks
            if (player1Network != null && player2Network != null) {
                enableNetworkVisibility();
            }

        } catch (Exception e) {
            java.lang.System.err.println("NETWORK_RECEIVE: Error receiving opponent network data: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void enableNetworkVisibility() {
        java.lang.System.out.println("VISIBILITY: Enabling network visibility - player1Network: " + (player1Network != null) + ", player2Network: " + (player2Network != null));
        this.networkVisibilityEnabled = true;

        // Send network visibility update to both players
        sendNetworkVisibilityUpdate();
    }

    private void sendNetworkVisibilityUpdate() {
        if (networkManager == null) return;

        // Create network visibility message
        NetworkMessage visibilityMessage = new NetworkMessage(
                NetworkMessage.MessageType.GAME_STATE_UPDATE,
                "SERVER",
                sessionId,
                createNetworkVisibilityData()
        );

        networkManager.sendMessage(visibilityMessage);
    }


    private NetworkVisibilityData createNetworkVisibilityData() {
        return new NetworkVisibilityData(
                networkVisibilityEnabled,
                player1Network,
                player2Network
        );
    }

    public Map<String, SystemInfo> getControllableSystems() {
        Map<String, SystemInfo> systemInfoMap = new HashMap<>();
        for (Map.Entry<String, ControllableSystem> entry : controllableSystems.entrySet()) {
            ControllableSystem system = entry.getValue();
            SystemInfo info = new SystemInfo(
                    system.getSystemId(),
                    system.getPosition(),
                    system.getAmmunition(),
                    0.0 // Cooldown will be updated separately
            );
            systemInfoMap.put(entry.getKey(), info);
        }
        return systemInfoMap;
    }

    public Map<String, CooldownInfo> getCooldowns() {
        Map<String, CooldownInfo> cooldownInfoMap = new HashMap<>();

        // Add system cooldowns
        for (Map.Entry<String, SystemCooldown> entry : systemCooldowns.entrySet()) {
            SystemCooldown cooldown = entry.getValue();
            CooldownInfo info = new CooldownInfo(
                    entry.getKey(),
                    cooldown.getRemainingTime(),
                    "SYSTEM"
            );
            cooldownInfoMap.put(entry.getKey(), info);
        }

        // Add packet cooldowns
        for (Map.Entry<String, PacketCooldown> entry : packetCooldowns.entrySet()) {
            PacketCooldown cooldown = entry.getValue();
            // Get cooldown for current player
            double remainingTime = cooldown.getRemainingTimeForPlayer(currentPlayerId);
            CooldownInfo info = new CooldownInfo(
                    entry.getKey(),
                    remainingTime,
                    "PACKET"
            );
            cooldownInfoMap.put(entry.getKey(), info);
        }

        return cooldownInfoMap;
    }

    public Map<String, Integer> getAmmunitionCounts() {
        Map<String, Integer> counts = new HashMap<>();

        for (ControllableSystem system : controllableSystems.values()) {
            Map<String, Integer> systemAmmo = system.getAmmunition();
            for (Map.Entry<String, Integer> entry : systemAmmo.entrySet()) {
                String packetType = entry.getKey();
                int count = entry.getValue();
                counts.put(packetType, counts.getOrDefault(packetType, 0) + count);
            }
        }

        return counts;
    }

    private void giveAmmunitionToPlayer(String playerId, int amount) {
        // Add ammunition to all controllable systems for the player
        for (ControllableSystem system : controllableSystems.values()) {
            // Add small messenger packets as reward
            system.addAmmunition("SMALL_MESSENGER", amount);
        }

    }

    private void checkGameOverConditions() {
        // Check various game over conditions

        // 1. Time-based game over (5 minutes max)
        long gameDuration = currentTime - setupPhaseStartTime;
        if (gameDuration > 300000) { // 5 minutes
            endGame("Time limit reached");
            return;
        }

        // 2. Score-based game over (first to 50 points wins)
        if (player1State.getScore() >= 50) {
            endGame("Player 1 wins with " + player1State.getScore() + " points!");
            return;
        }
        if (player2State.getScore() >= 50) {
            endGame("Player 2 wins with " + player2State.getScore() + " points!");
            return;
        }

        // 3. Ammunition-based game over (if both players run out of ammunition)
        if (areAllSystemsOutOfAmmunition()) {
            // Determine winner by score
            if (player1State.getScore() > player2State.getScore()) {
                endGame("Player 1 wins - out of ammunition! Score: " + player1State.getScore());
            } else if (player2State.getScore() > player1State.getScore()) {
                endGame("Player 2 wins - out of ammunition! Score: " + player2State.getScore());
            } else {
                endGame("Game tied - both players out of ammunition!");
            }
        }
    }

    private boolean areAllSystemsOutOfAmmunition() {
        for (ControllableSystem system : controllableSystems.values()) {
            // Check if system has any ammunition left
            if (system.hasAnyAmmunition()) {
                return false;
            }
        }
        return true;
    }

    private void endGame(String reason) {
        isGameStarted = false;
        isRunning = false;


        // Send game over message to both players
        NetworkMessage gameOverMessage = new NetworkMessage(
                NetworkMessage.MessageType.GAME_OVER,
                "SERVER",
                sessionId,
                reason
        );

        if (networkManager != null) {
            networkManager.sendMessage(gameOverMessage);
        }

        // Stop the game loop
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    private void updateCooldowns() {
        // Update system cooldowns
        for (SystemCooldown cooldown : systemCooldowns.values()) {
            cooldown.update();
            // Apply server multiplier by stretching remaining time
            if (serverCooldownMultiplier > 1.0) {
                // Increase remaining time proportionally to reflect penalty
                // (simple approach: reapply duration growth)
                // SystemCooldown has no direct API; assume update() decrements fixed step.
                // We can simulate by adding back a fraction when active.
            }
        }

        // Update packet cooldowns
        for (PacketCooldown cooldown : packetCooldowns.values()) {
            cooldown.update(serverCooldownMultiplier);
        }
    }

    private void sendGameStateUpdate() {
        MultiplayerGameState gameStateData = new MultiplayerGameState(
                isSetupPhase,
                isGameStarted,
                getRemainingSetupTime(),
                player1State,
                player2State,
                getControllableSystemsInfo(),
                getCooldownInfo()
        );

        NetworkMessage updateMessage = new NetworkMessage(
                NetworkMessage.MessageType.GAME_STATE_UPDATE,
                "SERVER",
                sessionId,
                gameStateData
        );

        // Send to both players
        networkManager.sendMessage(updateMessage);
    }

    private long getRemainingSetupTime() {
        if (!isSetupPhase) return 0;

        long elapsed = currentTime - setupPhaseStartTime;
        long remaining = SETUP_PHASE_DURATION - elapsed;
        return Math.max(0, remaining);
    }

    private Map<String, SystemInfo> getControllableSystemsInfo() {
        Map<String, SystemInfo> systemsInfo = new HashMap<>();

        for (Map.Entry<String, ControllableSystem> entry : controllableSystems.entrySet()) {
            String systemId = entry.getKey();
            ControllableSystem system = entry.getValue();
            SystemCooldown cooldown = systemCooldowns.get(systemId);

            systemsInfo.put(systemId, new SystemInfo(
                    systemId,
                    system.getPosition(),
                    system.getAmmunition(),
                    cooldown != null ? cooldown.getRemainingTime() : 0.0
            ));
        }

        return systemsInfo;
    }

    private Map<String, CooldownInfo> getCooldownInfo() {
        Map<String, CooldownInfo> cooldownInfo = new HashMap<>();

        // Add system cooldowns
        for (SystemCooldown cooldown : systemCooldowns.values()) {
            cooldownInfo.put("system_" + cooldown.getSystemId(),
                    new CooldownInfo(cooldown.getSystemId(), cooldown.getRemainingTime(), "SYSTEM"));
        }

        // Add packet cooldowns
        for (PacketCooldown cooldown : packetCooldowns.values()) {
            cooldownInfo.put("packet_" + cooldown.getPacketType(),
                    new CooldownInfo(cooldown.getPacketType(), cooldown.getRemainingTimeForPlayer("player1"), "PACKET"));
        }

        return cooldownInfo;
    }



    public Map<String, Double> getCooldownTimes() {
        Map<String, Double> cooldownTimes = new HashMap<>();

        // Get system cooldowns
        for (Map.Entry<String, SystemCooldown> entry : systemCooldowns.entrySet()) {
            String systemId = entry.getKey();
            SystemCooldown cooldown = entry.getValue();
            cooldownTimes.put(systemId, cooldown.getRemainingTime());
        }

        return cooldownTimes;
    }

    public int getOpponentScore() {
        // Return the score of the opponent (player2 if current player is player1, and vice versa)
        // For now, we'll return a simulated score based on game state
        if (player1State != null && player2State != null) {
            // Return the score of the player who is not the current player
            // This is a simplified implementation - in a real game, you'd track which player is "current"
            return Math.max(player1State.getScore(), player2State.getScore());
        }
        return 0;
    }

    public Map<String, Integer> getOpponentAmmunition() {
        Map<String, Integer> opponentAmmunition = new HashMap<>();

        // Request ammunition data from opponent via network
        if (networkManager != null && networkManager.isConnected()) {
            try {
                // Send ammunition request to opponent
                NetworkMessage requestMessage = new NetworkMessage(
                        NetworkMessage.MessageType.PLAYER_ACTION,
                        getCurrentPlayerId(),
                        sessionId,
                        "AMMUNITION_REQUEST"
                );

                if (networkManager.sendMessage(requestMessage)) {
                    // For now, return cached opponent ammunition if available
                    // In a full implementation, this would wait for the response
                    opponentAmmunition = getCachedOpponentAmmunition();
                } else {
                }
            } catch (Exception e) {
            }
        } else {
        }

        return opponentAmmunition;
    }

    private Map<String, Integer> getCachedOpponentAmmunition() {
        // For now, return a sample ammunition map for demonstration
        // In a full implementation, this would be updated via network messages
        Map<String, Integer> cachedAmmunition = new HashMap<>();
        cachedAmmunition.put("SMALL_MESSENGER", 5);
        cachedAmmunition.put("PROTECTED", 3);
        cachedAmmunition.put("CONFIDENTIAL", 2);
        return cachedAmmunition;
    }


    public model.GameLevel getOpponentNetwork(String playerId) {
        if ("Player 1".equals(playerId) || player1Id.equals(playerId)) {
            return player2Network;
        } else if ("Player 2".equals(playerId) || player2Id.equals(playerId)) {
            return player1Network;
        }
        return null;
    }

    public boolean isNetworkVisibilityEnabled() {
        return networkVisibilityEnabled;
    }


    public boolean isSetupPhase() {
        return isSetupPhase;
    }


    public boolean isGameStarted() {
        return isGameStarted;
    }


    public String getCurrentPlayerId() {
        return currentPlayerId;
    }


    public void setCurrentPlayerId(String playerId) {
        this.currentPlayerId = playerId;
    }


    public void setGameView(view.MultiplayerGameView gameView) {
        this.gameView = gameView;
    }

    public String getOpponentPlayerId() {
        // For now, we'll return player2Id as the default
        // In a full implementation, this would be determined by the actual player context
        return player2Id;
    }


    public double getOpponentNetworkHealth() {
        // Simulate network health based on game state
        // In a real implementation, this would be based on actual network damage
        double baseHealth = 0.8; // Start with 80% health

        // Reduce health based on successful packet deliveries (damage to network)
        if (player1State != null && player2State != null) {
            int totalDeliveries = player1State.getScore() + player2State.getScore();
            double damage = Math.min(0.4, totalDeliveries * 0.05); // Max 40% damage
            baseHealth = Math.max(0.1, baseHealth - damage); // Min 10% health
        }

        return baseHealth;
    }

    public void setPlayerReady(String playerId) {
        if (playerId.equals(player1Id)) {
            player1Ready = true;

        } else if (playerId.equals(player2Id)) {
            player2Ready = true;

        } else {

            return;
        }

        // Check if both players are ready
        if (player1Ready && player2Ready) {

            startGame();
        }
    }

    public void sendPlayerReady() {
        try {
            if (networkManager != null && sessionId != null) {
                NetworkMessage readyMsg = new NetworkMessage(
                        NetworkMessage.MessageType.PLAYER_READY,
                        getCurrentPlayerId(),
                        sessionId,
                        "READY"
                );
                boolean sent = networkManager.sendMessage(readyMsg);
                if (sent) {

                } else {

                }
            } else {

            }
        } catch (Exception e) {
            java.lang.System.err.println("❌ CLIENT: Error sending PLAYER_READY: " + e.getMessage());
        }
    }

    private void startGame() {
        isSetupPhase = false;
        isGameStarted = true;

        // Update UI to show game has started
        if (gameView != null) {
            javafx.application.Platform.runLater(() -> {
                // Update game phase label
                if (gameView.getGamePhaseLabel() != null) {
                    gameView.getGamePhaseLabel().setText("Phase: Game Active");
                }

                // Update setup instructions
                if (gameView.getSetupInstructionsLabel() != null) {
                    gameView.getSetupInstructionsLabel().setText("Game started! Build your network and compete!");
                }

                // Update ready button
                if (gameView.getReadyButton() != null) {
                    gameView.getReadyButton().setText("Game Active");
                    gameView.getReadyButton().setStyle(
                            "-fx-background-color: darkgreen;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-weight: bold;"
                    );
                }

                // Hide setup timer since game has started
                if (gameView.getSetupTimerLabel() != null) {
                    gameView.getSetupTimerLabel().setText("Game Started!");
                    gameView.getSetupTimerLabel().setStyle(
                            "-fx-text-fill: lime; -fx-font-weight: bold; -fx-font-size: 16px;"
                    );
                }

                // Trigger network sharing and end setup phase
                gameView.endSetupPhase();
            });
        }

        // Note: Network visibility will be enabled automatically when both players
        // share their network data via setPlayerNetwork() -> enableNetworkVisibility()

        // Notify both players that the game is starting
        NetworkMessage startMessage = new NetworkMessage(
                NetworkMessage.MessageType.GAME_STATE_UPDATE,
                "SERVER",
                sessionId,
                "Game starting!"
        );

        networkManager.sendMessage(startMessage);
    }

    public void handlePlayerAction(String playerId, PlayerAction action) {
        if (!isGameStarted) return;

        switch (action.getType()) {
            case RELEASE_PACKET:
                handlePacketRelease(playerId, action);
                break;
            case USE_ABILITY:
                handleAbilityUse(playerId, action);
                break;
            default:
                // Handle other action types
                break;
        }
    }

    private void handlePacketRelease(String playerId, PlayerAction action) {
        String systemId = action.getSystemId();
        String packetType = action.getPacketType();

        // Check if system is available
        if (!isSystemAvailable(systemId)) {
            return; // System is on cooldown
        }

        // Check if packet type is available for this player
        if (!isPacketTypeAvailable(playerId, packetType)) {
            return; // Packet type is on cooldown for this player
        }

        // Check if player has ammunition
        ControllableSystem system = controllableSystems.get(systemId);
        if (system == null || !system.hasAmmunition(packetType)) {
            return; // No ammunition available
        }

        // Release the packet
        releasePacket(playerId, systemId, packetType);

        // Apply cooldowns
        applyCooldowns(systemId, packetType, playerId);
    }

    private boolean isSystemAvailable(String systemId) {
        SystemCooldown cooldown = systemCooldowns.get(systemId);
        return cooldown == null || cooldown.isExpired();
    }

    private boolean isPacketTypeAvailable(String playerId, String packetType) {
        PacketCooldown cooldown = packetCooldowns.get(packetType);
        return cooldown == null || cooldown.isExpiredForPlayer(playerId);
    }

    private void releasePacket(String playerId, String systemId, String packetType) {
        ControllableSystem system = controllableSystems.get(systemId);
        if (system != null) {
            boolean consumed = system.consumeAmmunition(packetType);
            if (!consumed) {

                return;
            }

            // Create and release the packet
            PacketType type = PacketType.valueOf(packetType);
            Point2D position = system.getPosition();

            // Create movement vector towards opponent's side
            Vec2D movementVector = createMovementVector(playerId, position);

            // Create packet using reflection manager
            Packet packet = packetManager.createPacket(type, position, movementVector);
            if (packet != null) {
                // Set packet ownership for proper scoring
                packet.setOwnerId(playerId);

                // Add packet to game state
                gameState.addActivePacket(packet);



                // Trigger UI update for ammunition display
                notifyAmmunitionChanged();
            }
        }
    }

    private void notifyAmmunitionChanged() {
        // This would trigger UI updates in a full implementation
        // For now, we rely on the periodic UI update timer

    }

    private Vec2D createMovementVector(String playerId, Point2D position) {
        // If targeting mode is active and we have a target, move toward the target
        if (isTargetingMode && selectedTargetPosition != null) {
            double deltaX = selectedTargetPosition.getX() - position.getX();
            double deltaY = selectedTargetPosition.getY() - position.getY();

            // Normalize and scale by speed
            double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
            double speed = 100; // Base speed

            if (length > 0) {
                return new Vec2D((deltaX / length) * speed, (deltaY / length) * speed);
            }
        }

        // Fallback to original behavior: move towards the opponent's side
        double targetX = "player1".equals(playerId) ? 1400 : 0; // Player 1 moves right, Player 2 moves left
        double targetY = position.getY(); // Keep same Y level

        // Calculate direction vector
        double deltaX = targetX - position.getX();
        double deltaY = targetY - position.getY();

        // Normalize and scale by speed
        double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        double speed = 100; // Base speed

        if (length > 0) {
            return new Vec2D((deltaX / length) * speed, (deltaY / length) * speed);
        } else {
            return new Vec2D(speed, 0); // Default movement
        }
    }

    public void setTarget(String targetSystemId, Point2D targetPosition) {
        this.selectedTargetSystemId = targetSystemId;
        this.selectedTargetPosition = targetPosition;
        this.isTargetingMode = true;

        // Target set successfully
    }

    public void clearTarget() {
        this.selectedTargetSystemId = null;
        this.selectedTargetPosition = null;
        this.isTargetingMode = false;

        // Target cleared
    }

    public String getCurrentTarget() {
        return selectedTargetSystemId;
    }

    public Point2D getCurrentTargetPosition() {
        return selectedTargetPosition;
    }


    public boolean isTargetingMode() {
        return isTargetingMode;
    }

    private void applyCooldowns(String systemId, String packetType, String playerId) {
        // Apply system cooldown (affects both players)
        SystemCooldown systemCooldown = systemCooldowns.get(systemId);
        if (systemCooldown != null) {
            systemCooldown.activate();
        }

        // Apply packet type cooldown (affects only the player who used it)
        PacketCooldown packetCooldown = packetCooldowns.get(packetType);
        if (packetCooldown != null) {
            packetCooldown.activateForPlayer(playerId);
        }
    }

    private void handleAbilityUse(String playerId, PlayerAction action) {
        // Implementation for handling ability use
        // This would involve checking if the ability is available and applying its effects
    }

    public void stopGame() {
        isRunning = false;
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    // Getters

    public long getRemainingSetupTimeMillis() {
        return getRemainingSetupTime();
    }

    public PlayerState getPlayer1State() {
        return player1State;
    }

    public PlayerState getPlayer2State() {
        return player2State;
    }

    public List<Packet> getOpponentPackets(String currentPlayerId) {
        List<Packet> opponentPackets = new ArrayList<>();

        if (gameState == null || gameState.getActivePackets() == null) {
            return opponentPackets;
        }

        // Determine which player is the opponent
        String opponentPlayerId = getOpponentPlayerId();

        // Filter packets that belong to the opponent
        for (Packet packet : gameState.getActivePackets()) {
            if (packet != null && packet.isActive()) {
                String packetOwner = determinePacketOwnerWithTrojanLogic(packet);
                if (opponentPlayerId.equals(packetOwner)) {
                    opponentPackets.add(packet);
                }
            }
        }

        return opponentPackets;
    }

    public List<Packet> getAllActivePackets() {
        if (gameState == null || gameState.getActivePackets() == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(gameState.getActivePackets());
    }
}



