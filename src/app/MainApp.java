package app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Screen;
import leaderboard.LeaderboardManager;
import leaderboard.LevelRecord;
import leaderboard.PlayerRecord;
import leaderboard.ScoreRecord;
import view.MainMenuView;
import view.LeaderboardView;
import view.MultiplayerGameView;
import view.MultiplayerLobbyView;
import controller.GameController;
import model.GameState;
import model.GameLevel;

import java.awt.*;

public class MainApp {
    
    public static class AppLauncher extends Application {
        private static MainApp mainApp;
        
        public AppLauncher() {
            // Default constructor required by JavaFX
        }
        
        @Override
        public void start(Stage primaryStage) {
            mainApp = new MainApp();
            mainApp.start(primaryStage);
        }
    }
    
    public static void launch(Class<? extends MainApp> appClass, String[] args) {
        Application.launch(AppLauncher.class, args);
    }
    private Stage primaryStage;
    private GameController gameController;
    private GameState gameState;
    private MainMenuView mainMenuView;


    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            initializeApplication();
            showMainMenu();

            // Don't minimize other applications here - do it when game starts

        } catch (Exception e) {
            showErrorDialog("Application Error", "Failed to start application", e.getMessage());
            Platform.exit();
        }
    }

    private void initializeApplication() {
        // Initialize game state and controller
        gameState = new GameState();
        gameController = new GameController(gameState);
        gameController.setMainApp(this);

        // Initialize main menu view
        mainMenuView = new MainMenuView(this);

        // Set up navigation callbacks AFTER views are initialized
        gameController.setupNavigationCallbacks(
                this::returnToMainMenu,
                this::restartCurrentLevel
        );

        // Configure primary stage
        configurePrimaryStage();
    }

    private void configurePrimaryStage() {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Network Simulation Game");

        // Set to fullscreen
        Screen screen = Screen.getPrimary();
        primaryStage.setX(screen.getVisualBounds().getMinX());
        primaryStage.setY(screen.getVisualBounds().getMinY());
        primaryStage.setWidth(screen.getVisualBounds().getWidth());
        primaryStage.setHeight(screen.getVisualBounds().getHeight());

        // Add close handler
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            showExitConfirmation();
        });
    }

    public void showMainMenu() {
        // Use setRoot instead of creating a new Scene to avoid the "already set as root" error
        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(mainMenuView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(mainMenuView.getRoot());
        }
        primaryStage.show();
    }

    public void startGame(String levelId) {
        try {
            java.lang.System.out.println("MainApp.startGame called with levelId: " + levelId);

            // Force new game start to ensure JSON loading path is used
            // Previously checked for save file and offered to continue; bypass for now
            startNewGame(levelId);

        } catch (Exception e) {
            showErrorDialog("Game Error", "Failed to start game", e.getMessage());
        }
    }


    public void startFreshGame(String levelId) {
        try {
            java.lang.System.out.println("MainApp.startFreshGame called with levelId: " + levelId);

            // Game always starts fresh (no connections preserved)

            // Start a completely fresh game with no preserved connections
            startNewGame(levelId);

        } catch (Exception e) {
            showErrorDialog("Game Error", "Failed to start fresh game", e.getMessage());
        }
    }



    private void showContinuePreviousGameDialog(String newLevelId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Continue Previous Game");
        alert.setHeaderText("A saved game from a previous session was found.");
        alert.setContentText("Would you like to continue the previously saved game? If you decline, the save file will be deleted and a new game will begin.");

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // User wants to continue previous game
                continueFromSaveFile();
            } else {
                // User declines, delete save file and start new game
                gameController.deleteSaveFile();
                startNewGame(newLevelId);
            }
        });
    }

    private void continueFromSaveFile() {
        try {
            if (gameController.loadSavedGame()) {
                // Show static game state for a few seconds before resuming
                showStaticGameStateAndResume();
            } else {
                showErrorDialog("Load Error", "Failed to load saved game", "The save file may be corrupted. Starting a new game instead.");
                gameController.deleteSaveFile();
                startNewGame("level1"); // Default to level 1 if load fails
            }
        } catch (Exception e) {
            showErrorDialog("Load Error", "Failed to load saved game", e.getMessage());
            gameController.deleteSaveFile();
            startNewGame("level1");
        }
    }

    private void startNewGame(String levelId) {
        try {
            // Load the level first
            gameController.loadLevel(levelId);

            // Now start the game (this will ensure the level is set in the game view)
            gameController.startGame();

            // Switch to game view
            java.lang.System.out.println("Switching to game view...");
            if (primaryStage.getScene() == null) {
                Scene gameScene = new Scene(gameController.getGameView().getRoot());
                primaryStage.setScene(gameScene);
                java.lang.System.out.println("Created new scene with game view");
            } else {
                primaryStage.getScene().setRoot(gameController.getGameView().getRoot());
                java.lang.System.out.println("Set game view as scene root");
            }

            // Ensure the game window is fully visible and focused first
            Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.requestFocus();
                primaryStage.toFront();

                // Only minimize other applications if the feature is enabled
                // For now, let's disable this feature to prevent issues
                // TODO: Implement a more robust minimize feature that doesn't affect the game window
                /*
                // Add a small delay to ensure the window is fully visible and focused
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Wait 500ms for window to be fully visible
                        Platform.runLater(this::minimizeOtherApplications);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                */
            });

        } catch (Exception e) {
            showErrorDialog("Game Error", "Failed to start new game", e.getMessage());
        }
    }

    private void showStaticGameStateAndResume() {
        try {
            // Switch to game view first
            if (primaryStage.getScene() == null) {
                Scene gameScene = new Scene(gameController.getGameView().getRoot());
                primaryStage.setScene(gameScene);
            } else {
                primaryStage.getScene().setRoot(gameController.getGameView().getRoot());
            }

            // Ensure window is visible
            primaryStage.show();
            primaryStage.requestFocus();
            primaryStage.toFront();

            // Show static state for 3 seconds before resuming
            Platform.runLater(() -> {
                // The game view will show the static state automatically when loaded
                // After 3 seconds, start the game
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); // Show static state for 3 seconds
                        Platform.runLater(() -> {
                            gameController.startGame(); // Resume the game
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            });

        } catch (Exception e) {
            showErrorDialog("Resume Error", "Failed to resume game", e.getMessage());
        }
    }

    public void showLevelSelect() {
        try {
            if (primaryStage.getScene() == null) {
                Scene levelScene = new Scene(gameController.getLevelSelectView().getRoot());
                primaryStage.setScene(levelScene);
            } else {
                primaryStage.getScene().setRoot(gameController.getLevelSelectView().getRoot());
            }
        } catch (Exception e) {
            showErrorDialog("Level Select Error", "Failed to load level selection", e.getMessage());
        }
    }

    public void showSettings() {
        try {
            if (primaryStage.getScene() == null) {
                Scene settingsScene = new Scene(gameController.getSettingsView().getRoot());
                primaryStage.setScene(settingsScene);
            } else {
                primaryStage.getScene().setRoot(gameController.getSettingsView().getRoot());
            }
        } catch (Exception e) {
            showErrorDialog("Settings Error", "Failed to load settings", e.getMessage());
        }
    }

    public void returnToMainMenu() {
        // Disable auto-save when manually returning to main menu
        if (gameController != null) {
            gameController.setAutoSaveEnabled(false);
            // Delete the save file since this is a manual exit
            gameController.deleteSaveFile();
        }
        showMainMenu();
    }

    public void restartCurrentLevel() {
        if (gameController != null && gameController.getGameState() != null) {
            GameLevel currentLevel = gameController.getGameState().getCurrentLevel();
            if (currentLevel != null) {
                // Preserve previous-level wires per spec; clear only current-level wiring
                gameController.restartLevelPreservingPrevious();
                
                // Start the game after restart
                gameController.startGame();
                
                // Switch back to game view
                try {
                    if (primaryStage.getScene() == null) {
                        Scene gameScene = new Scene(gameController.getGameView().getRoot());
                        primaryStage.setScene(gameScene);
                    } else {
                        primaryStage.getScene().setRoot(gameController.getGameView().getRoot());
                    }
                    primaryStage.show();
                    primaryStage.requestFocus();
                    primaryStage.toFront();
                } catch (Exception e) {
                    System.err.println("Failed to switch to game view after restart: " + e.getMessage());
                }
            }
        }
    }

    public void restartCurrentLevelNow() {
        if (gameController != null && gameController.getGameState() != null) {
            GameLevel currentLevel = gameController.getGameState().getCurrentLevel();
            if (currentLevel != null) {
                // Ensure we don't prompt to continue; wipe any save and restart while
                // preserving prior-level wiring per spec
                gameController.deleteSaveFile();
                gameController.restartLevelPreservingPrevious();
                
                // Start the game after restart
                gameController.startGame();
                
                // Switch back to game view (for pause menu restart, we're already in game view, but ensure it's focused)
                try {
                    if (primaryStage.getScene() == null) {
                        Scene gameScene = new Scene(gameController.getGameView().getRoot());
                        primaryStage.setScene(gameScene);
                    } else {
                        primaryStage.getScene().setRoot(gameController.getGameView().getRoot());
                    }
                    primaryStage.show();
                    primaryStage.requestFocus();
                    primaryStage.toFront();
                } catch (Exception e) {
                    System.err.println("Failed to switch to game view after pause menu restart: " + e.getMessage());
                }
            }
        }
    }

    private void minimizeOtherApplications() {
        try {
            // First, ensure our game window is focused and visible
            Platform.runLater(() -> {
                primaryStage.requestFocus();
                primaryStage.toFront();
            });

            // Wait a bit for the focus to take effect
            Thread.sleep(200);

            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("windows")) {
                minimizeWindowsApplications();
            } else if (osName.contains("mac")) {
                minimizeMacApplications();
            } else if (osName.contains("linux")) {
                minimizeLinuxApplications();
            }
        } catch (Exception e) {
            // Silently handle any errors in minimizing other applications
            System.err.println("Could not minimize other applications: " + e.getMessage());
        }
    }

    private void minimizeWindowsApplications() {
        try {
            // Use Robot to press Windows + D to show desktop (more reliable than Windows + M)
            Robot robot = new Robot();
            robot.setAutoDelay(50); // Set auto delay for more reliable key presses

            robot.keyPress(java.awt.event.KeyEvent.VK_WINDOWS);
            robot.keyPress(java.awt.event.KeyEvent.VK_D);
            robot.delay(100);
            robot.keyRelease(java.awt.event.KeyEvent.VK_D);
            robot.keyRelease(java.awt.event.KeyEvent.VK_WINDOWS);

            // Wait a bit for the minimize action to complete
            robot.delay(200);

        } catch (AWTException e) {
            System.err.println("Failed to minimize Windows applications: " + e.getMessage());
        }
    }

    private void minimizeMacApplications() {
        try {
            // Use Robot to press Command + H to hide applications
            Robot robot = new Robot();
            robot.setAutoDelay(50); // Set auto delay for more reliable key presses

            robot.keyPress(java.awt.event.KeyEvent.VK_META);
            robot.keyPress(java.awt.event.KeyEvent.VK_H);
            robot.delay(100);
            robot.keyRelease(java.awt.event.KeyEvent.VK_H);
            robot.keyRelease(java.awt.event.KeyEvent.VK_META);

            // Wait a bit for the minimize action to complete
            robot.delay(200);

        } catch (AWTException e) {
            System.err.println("Failed to minimize Mac applications: " + e.getMessage());
        }
    }

    private void minimizeLinuxApplications() {
        try {
            // Use Robot to press Super + D to show desktop (most Linux DEs)
            Robot robot = new Robot();
            robot.setAutoDelay(50); // Set auto delay for more reliable key presses

            robot.keyPress(java.awt.event.KeyEvent.VK_META);
            robot.keyPress(java.awt.event.KeyEvent.VK_D);
            robot.delay(100);
            robot.keyRelease(java.awt.event.KeyEvent.VK_D);
            robot.keyRelease(java.awt.event.KeyEvent.VK_META);

            // Wait a bit for the minimize action to complete
            robot.delay(200);

        } catch (AWTException e) {
            System.err.println("Failed to minimize Linux applications: " + e.getMessage());
        }
    }

    private void showExitConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved progress will be lost.");

        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // Disable auto-save and delete save file since this is a manual exit
                if (gameController != null) {
                    gameController.setAutoSaveEnabled(false);
                    gameController.deleteSaveFile();
                }
                Platform.exit();
            }
        });
    }

    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public GameController getGameController() {
        return gameController;
    }

    public GameState getGameState() {
        return gameState;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setNetworkManager(network.NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public network.NetworkManager getNetworkManager() {
        return networkManager;
    }

    public void showLeaderboard() {
        // Check if primaryStage is properly initialized
        if (primaryStage == null) {
            showErrorDialog("Leaderboard Error", "Application not properly initialized",
                    "The primary stage is null. Please restart the application.");
            return;
        }

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(leaderboardView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(leaderboardView.getRoot());
        }
        primaryStage.show();
    }

    public void createMultiplayerGame() {

        // Generate a unique game ID
        String gameId = "GAME" + String.format("%03d", (int)(Math.random() * 1000));

        // Show game ID to the player
        showGameIdDialog(gameId, true);

        // Check if primaryStage is properly initialized
        if (primaryStage == null) {
            showErrorDialog("Multiplayer Error", "Application not properly initialized",
                    "The primary stage is null. Please restart the application.");
            return;
        }

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(multiplayerGameView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(multiplayerGameView.getRoot());
        }
        primaryStage.show();

        // Create and connect the multiplayer game controller with the generated game ID
        createMultiplayerGameController(gameId, "Player 1", true);

        // Start the setup phase
        multiplayerGameView.startSetupPhase();
    }

    private void createMultiplayerGameController() {
        createMultiplayerGameController("GAME001", "Player 1", true);
    }

    private void createMultiplayerGameController(String gameId, String playerName, boolean isHost) {

        // Reuse the existing NetworkManager so the same server-side clientId is used
        // This ensures server GAME_STATE_UPDATE messages (timer) reach this client
        network.NetworkManager networkManager = this.networkManager;

        if (networkManager == null) {

            // Use server default port (8081) to ensure joiner receives updates
            networkManager = new network.NetworkManager("localhost", 8081);
            // Store it for reuse across lobby and game controller
            this.networkManager = networkManager;
        }

        // Set up connection status callback
        networkManager.setConnectionStatusCallback(status -> {
            Platform.runLater(() -> {
                System.out.println("Network Status: " + status);
            });
        });

        // Ensure messages are routed to MainApp handler (controller/lobby will delegate as needed)
        networkManager.setMessageCallback(message -> {
            Platform.runLater(() -> {
                handleMultiplayerMessage(message);
            });
        });

        // Connect if not already connected
        if (!networkManager.isConnected()) {

            if (!networkManager.connect()) {
                showErrorDialog("Connection Error", "Failed to connect to server",
                        "Could not establish connection to the game server. Please ensure the server is running.");
                return;
            }

        } else {

        }

        // Determine player IDs based on host status
        // Host is always Player 1, joiner is always Player 2
        String player1Id = "Player 1";
        String player2Id = "Player 2";

        // Create the multiplayer game controller with real network connection
        multiplayer.MultiplayerGameController controller =
                new multiplayer.MultiplayerGameController(
                        player1Id, player2Id, gameId, networkManager
                );

        // Set the current player ID for this controller instance
        // Host is always Player 1, joiner is always Player 2
        String currentPlayerId = isHost ? "Player 1" : "Player 2";
        controller.setCurrentPlayerId(currentPlayerId);
        System.out.println("  Controller currentPlayerId set to: " + currentPlayerId);

        // Connect the controller to the view
        multiplayerGameView.setGameController(controller);

        // Set the game view reference in the controller for UI updates
        controller.setGameView(multiplayerGameView);

        // Send multiplayer session request to server
        if (isHost) {

            sendCreateGameRequest(gameId, playerName, networkManager);
        } else {

            sendJoinGameRequest(gameId, playerName, networkManager);
        }


        System.out.println("Multiplayer game controller created for game " + gameId +
                " with player " + playerName + " as " + (isHost ? "host" : "client"));
    }

    private void handleMultiplayerMessage(network.NetworkMessage message) {
        // Always log message type for debugging
        System.out.println("🔍 MAINAPP: Received " + message.getType() + " from: " + message.getPlayerId());

        // Only log important message types to reduce noise (keeping for reference)
        // if (message.getType() == com.networksimulation.network.NetworkMessage.MessageType.NETWORK_DATA ||
        //     message.getType() == com.networksimulation.network.NetworkMessage.MessageType.GAME_OVER ||
        //     message.getType() == com.networksimulation.network.NetworkMessage.MessageType.MULTIPLAYER_INVITE) {
        //     System.out.println("🔍 MAINAPP: Received " + message.getType() + " from: " + message.getPlayerId());
        // }

        switch (message.getType()) {
            case MULTIPLAYER_INVITE:
                handleMultiplayerInvite(message);
                break;
            case MULTIPLAYER_ACCEPT:
                handleMultiplayerAccept(message);
                break;
            case MULTIPLAYER_REJECT:
                handleMultiplayerReject(message);
                break;
            case GAME_STATE_UPDATE:
                handleGameStateUpdate(message);
                break;
            case NETWORK_DATA:
                System.out.println("📡 MAINAPP: Processing NETWORK_DATA message");
                handleNetworkData(message);
                break;
            case PLAYER_ACTION:
                handlePlayerAction(message);
                break;
            case GAME_OVER:
                handleGameOver(message);
                break;
            default:
                System.out.println("⚠️ MAINAPP: Unhandled message type: " + message.getType());
                break;
        }
    }

    private void sendCreateGameRequest(String gameId, String playerName, network.NetworkManager networkManager) {
        network.NetworkMessage message = new network.NetworkMessage(
                network.NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                playerName,
                gameId,
                "CREATE_GAME"
        );
        boolean sent = networkManager.sendMessage(message);
    }

    private void sendJoinGameRequest(String gameId, String playerName, network.NetworkManager networkManager) {
        network.NetworkMessage message = new network.NetworkMessage(
                network.NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                playerName,
                gameId,
                "JOIN_GAME"
        );
        boolean sent = networkManager.sendMessage(message);
    }

    private void handleMultiplayerInvite(network.NetworkMessage message) {
        String data = (String) message.getData();

        if ("GAME_CREATED".equals(data)) {

            // Game created successfully, continue with multiplayer setup
        } else if ("GAME_CREATION_FAILED".equals(data)) {

            showErrorDialog("Create Game Failed", "Game Creation Failed",
                    "Failed to create a new game. Please try again.");
            // Stop the multiplayer setup since creation failed
            return;
        } else {
        }
    }

    private void handleMultiplayerAccept(network.NetworkMessage message) {
        String data = (String) message.getData();

        if ("GAME_JOINED".equals(data)) {

            // Game joined successfully, continue with multiplayer setup
        } else if ("GAME_JOIN_FAILED".equals(data)) {

            showErrorDialog("Join Game Failed", "No Available Game",
                    "No game is available to join. Please create a new game or check the game ID.");
            // Stop the multiplayer setup since join failed
            return;
        } else {
        }
    }

    private void handleMultiplayerReject(network.NetworkMessage message) {
        System.out.println("Multiplayer game rejected: " + message.getData());
        showErrorDialog("Game Rejected", "Game Request Rejected",
                "The other player has rejected your game invitation.");
    }


    private void handleGameStateUpdate(network.NetworkMessage message) {
        // Forward the message to the multiplayer game controller for proper handling
        if (multiplayerGameView != null && multiplayerGameView.getGameController() != null) {

            // Check if this is network data (GameLevel object)
            if (message.getData() instanceof model.GameLevel) {

                multiplayerGameView.getGameController().receiveOpponentNetworkData(
                        message.getPlayerId(),
                        (model.GameLevel) message.getData()
                );
            } else {
                // Handle other game state updates (including timer updates)

                multiplayerGameView.getGameController().updateGameState(message.getData());
            }
        } else {

            if (multiplayerGameView != null) {
            }
        }
    }

    private void handlePlayerAction(network.NetworkMessage message) {
        // Only log non-ammunition requests to reduce noise
        if (message.getData() != null && !message.getData().toString().contains("AMMUNITION_REQUEST")) {
            System.out.println("Player action received: " + message.getData());
        }
        // Handle opponent's actions
        if (multiplayerGameView != null && multiplayerGameView.getGameController() != null) {
            multiplayerGameView.getGameController().handleOpponentAction(message.getData());
        }
    }

    private void handleGameOver(network.NetworkMessage message) {
        System.out.println("Game over: " + message.getData());
        // Handle game end logic
        Platform.runLater(() -> {
            showErrorDialog("Game Over", "Game Ended",
                    "The multiplayer game has ended. " + message.getData());
        });
    }


    public void showMultiplayerLobby() {
        // Check if primaryStage is properly initialized
        if (primaryStage == null) {
            showErrorDialog("Multiplayer Lobby Error", "Application not properly initialized",
                    "The primary stage is null. Please restart the application.");
            return;
        }

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(multiplayerLobbyView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(multiplayerLobbyView.getRoot());
        }
        primaryStage.show();
    }

    public void startMultiplayerGame(String gameId, String playerName, boolean isHost) {
        // Reset the multiplayer view to ensure clean state
        multiplayerGameView.resetToInitialState();

        // Debug logging

        System.out.println("  gameId: " + gameId);
        System.out.println("  playerName: " + playerName);
        System.out.println("  isHost: " + isHost);

        // Determine current player ID based on host status
        // Host is always Player 1, joiner is always Player 2
        String currentPlayerId = isHost ? "Player 1" : "Player 2";
        System.out.println("  currentPlayerId determined: " + currentPlayerId + " (playerName: " + playerName + ", isHost: " + isHost + ")");
        multiplayerGameView.setCurrentPlayerId(currentPlayerId);

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(multiplayerGameView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(multiplayerGameView.getRoot());
        }
        primaryStage.show();

        // Ensure that, from this moment, all server messages (including timer updates)
        // are routed to the MainApp handler instead of the lobby.
        // This prevents the joiner's setup timer from staying at the initial 01:00.
        if (this.networkManager != null) {
            network.NetworkManager nm = this.networkManager;
            nm.setMessageCallback(message -> {
                javafx.application.Platform.runLater(() -> {
                    handleMultiplayerMessage(message);
                });
            });
        }

        // Create and connect the multiplayer game controller with proper player info
        createMultiplayerGameController(gameId, playerName, isHost);

        // Start the setup phase
        multiplayerGameView.startSetupPhase();
    }

    public void forwardGameStateUpdate(Object gameStateData) {

        // Check if we're in multiplayer game view with a controller
        if (multiplayerGameView != null && multiplayerGameView.getGameController() != null) {

            multiplayerGameView.getGameController().updateGameState(gameStateData);
        }
        // Check if we're in lobby view - forward to lobby for timer updates
        else if (multiplayerLobbyView != null) {

            multiplayerLobbyView.handleLobbyMessage(new com.networksimulation.network.NetworkMessage(
                    com.networksimulation.network.NetworkMessage.MessageType.GAME_STATE_UPDATE,
                    "SERVER",
                    "LOBBY",
                    gameStateData
            ));
        } else {

            if (multiplayerGameView != null) {
            }
        }
    }

    private void handleNetworkData(network.NetworkMessage message) {
        System.out.println("📨 MAINAPP: Received NETWORK_DATA from " + message.getPlayerId());

        // Forward to multiplayer game controller if available
        if (multiplayerGameView != null && multiplayerGameView.getGameController() != null) {
            model.GameLevel opponentNetwork = null;

            // Extract network data from message
            Object networkData = message.getData();
            if (networkData instanceof model.GameLevel) {
                opponentNetwork = (model.GameLevel) networkData;
                System.out.println("✅ MAINAPP: Received GameLevel directly");
            } else if (networkData instanceof java.util.Map) {
                // Convert LinkedHashMap back to GameLevel using Jackson
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    opponentNetwork = mapper.convertValue(networkData, model.GameLevel.class);
                    System.out.println("✅ MAINAPP: Converted LinkedHashMap to GameLevel");
                } catch (Exception e) {
                    System.err.println("❌ MAINAPP: Failed to convert LinkedHashMap to GameLevel: " + e.getMessage());
                    return;
                }
            } else {
                System.out.println("❌ MAINAPP: Network data is not GameLevel or Map: " +
                        (networkData != null ? networkData.getClass().getSimpleName() : "null"));
                return;
            }

            if (opponentNetwork != null) {
                System.out.println("✅ MAINAPP: Forwarding GameLevel data to controller (systems: " +
                        opponentNetwork.getSystems().size() + ", connections: " +
                        opponentNetwork.getWireConnections().size() + ")");
                multiplayerGameView.getGameController().receiveOpponentNetworkData(
                        message.getPlayerId(), opponentNetwork);
            }
        } else {
            System.out.println("❌ MAINAPP: No game controller available - multiplayerGameView: " +
                    (multiplayerGameView != null) + ", controller: " +
                    (multiplayerGameView != null ? multiplayerGameView.getGameController() != null : "N/A"));
        }
    }

    public void joinMultiplayerGame() {

        // Ask for game ID
        showGameIdDialog("", false);
    }

    private void showGameIdDialog(String gameId, boolean isCreating) {
        javafx.scene.control.TextInputDialog dialog;

        if (isCreating) {
            // Show the created game ID
            dialog = new javafx.scene.control.TextInputDialog(gameId);
            dialog.setTitle("Game Created!");
            dialog.setHeaderText("Your game has been created successfully!");
            dialog.setContentText("Game ID: " + gameId + "\n\nShare this Game ID with the other player so they can join your game.\n\nClick OK to continue to the game setup.");
            dialog.getEditor().setEditable(false);
        } else {
            // Ask for game ID to join
            dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Join Game");
            dialog.setHeaderText("Enter Game ID to join an existing game");
            dialog.setContentText("Please enter the Game ID provided by the host:");
        }

        dialog.showAndWait().ifPresent(enteredGameId -> {
            if (!isCreating && !enteredGameId.trim().isEmpty()) {
                // Join the game with the entered ID
                joinGameWithId(enteredGameId.trim());
            }
        });
    }

    private void joinGameWithId(String gameId) {

        // Check if primaryStage is properly initialized
        if (primaryStage == null) {
            showErrorDialog("Multiplayer Error", "Application not properly initialized",
                    "The primary stage is null. Please restart the application.");
            return;
        }

        // Use the proper lobby system for validation instead of bypassing it
        // This ensures the game ID is validated by the server before starting the game
        showMultiplayerLobby();

        // Set the game ID in the lobby and trigger the join process
        if (multiplayerLobbyView != null) {
            // Set the game ID field and trigger join
            multiplayerLobbyView.setGameIdAndJoin(gameId);
        } else {
            showErrorDialog("Multiplayer Error", "Lobby not available",
                    "The multiplayer lobby is not available. Please try again.");
        }
    }

    public MultiplayerGameView getMultiplayerGameView() {
        return multiplayerGameView;
    }

} 