package client;

import app.MainApp;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Screen;
import view.MainMenuView;
import view.LeaderboardView;
import view.MultiplayerGameView;
import view.MultiplayerLobbyView;
import controller.GameController;
import model.GameState;
import model.GameLevel;

import java.awt.*;

public class ClientApp extends Application {

    private Stage primaryStage;
    private GameController gameController;
    private GameState gameState;
    private MainMenuView mainMenuView;
    private LeaderboardView leaderboardView;
    private MultiplayerGameView multiplayerGameView;
    private MultiplayerLobbyView multiplayerLobbyView;
    private network.NetworkManager networkManager;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        try {
            initializeApplication();
            showMainMenu();

        } catch (Exception e) {
            showErrorDialog("Application Error", "Failed to start client application", e.getMessage());
            Platform.exit();
        }
    }

    private void initializeApplication() {
        // Initialize game state and controller
        gameState = new GameState();
        gameController = new GameController(gameState);
        // Note: We'll create a MainApp adapter for compatibility
        gameController.setMainApp(new MainAppAdapter(this));

        // Initialize network manager
        networkManager = new network.NetworkManager("localhost", 8081);

        // Initialize main menu view
        mainMenuView = new MainMenuView(new MainAppAdapter(this));

        // Initialize other views
        leaderboardView = new LeaderboardView(new MainAppAdapter(this));
        multiplayerGameView = new MultiplayerGameView(new MainAppAdapter(this));
        multiplayerLobbyView = new MultiplayerLobbyView(new MainAppAdapter(this));

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
        primaryStage.setTitle("Network Simulation Game - Client");

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
        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(mainMenuView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(mainMenuView.getRoot());
        }
        primaryStage.show();
    }

    public void showLeaderboard() {
        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(leaderboardView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(leaderboardView.getRoot());
        }
        primaryStage.show();
    }

    public void startGame(GameLevel level) {
        try {
            // Load the level using the available method
            gameController.loadLevel(level.getLevelId());

            // Create and show the game view
            view.GameView gameView = new view.GameView(gameController);

            Scene scene = new Scene(gameView.getRoot());
            primaryStage.setScene(scene);
            primaryStage.show();

            // Start the game
            gameController.startGame();

        } catch (Exception e) {
            showErrorDialog("Game Error", "Failed to start game", e.getMessage());
            e.printStackTrace();
        }
    }

    public void returnToMainMenu() {
        if (gameController != null) {
            gameController.stopGame();
        }
        showMainMenu();
    }


    public void restartCurrentLevel() {
        if (gameController != null) {
            // Use the available restart method
            gameController.restartLevelPreservingPrevious();
        }
    }


    private void showGameIdDialog(String gameId, boolean isCreating) {
        javafx.scene.control.TextInputDialog dialog;

        if (isCreating) {
            // Show the created game ID
            dialog = new javafx.scene.control.TextInputDialog(gameId);
            dialog.setTitle("Game Created!");
            dialog.setHeaderText("Your game has been created!");
            dialog.setContentText("Game ID (share this with other players):");
        } else {
            // Ask for game ID to join
            dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Join Game");
            dialog.setHeaderText("Enter Game ID");
            dialog.setContentText("Please enter the Game ID to join:");
        }

        // TextInputDialog already has OK and Cancel buttons, no need to add more

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String enteredGameId = result.get().trim();
            if (!enteredGameId.isEmpty()) {
                if (isCreating) {
                    // For creating, also use the lobby system for proper server validation

                    showMultiplayerLobby();

                    // Set the game ID in the lobby and trigger the create process
                    if (multiplayerLobbyView != null) {
                        multiplayerLobbyView.setGameIdAndCreate(enteredGameId);
                    } else {
                        showErrorDialog("Multiplayer Error", "Lobby not available",
                                "The multiplayer lobby is not available. Please try again.");
                    }
                } else {
                    // For joining, use the lobby system for proper server validation

                    showMultiplayerLobby();

                    // Set the game ID in the lobby and trigger the join process
                    if (multiplayerLobbyView != null) {
                        multiplayerLobbyView.setGameIdAndJoin(enteredGameId);
                    } else {
                        showErrorDialog("Multiplayer Error", "Lobby not available",
                                "The multiplayer lobby is not available. Please try again.");
                    }
                }
            } else {

            }
        } else {

        }
    }


    public void showMultiplayerGame() {
        // Create multiplayer game controller with default values
        createMultiplayerGameController();
    }

    public void showMultiplayerGame(String gameId, String playerName, boolean isHost) {
        // Create multiplayer game controller with specific parameters
        createMultiplayerGameController(gameId, playerName, isHost);

        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(multiplayerGameView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(multiplayerGameView.getRoot());
        }
        primaryStage.show();

        // Ensure setup phase UI initializes immediately; countdown will be driven by server updates
        if (multiplayerGameView != null) {
            multiplayerGameView.startSetupPhase();
        }
    }

    private void createMultiplayerGameController() {
        createMultiplayerGameController("GAME001", "Player 1", true);
    }


    private void createMultiplayerGameController(String gameId, String playerName, boolean isHost) {
        // Reuse the existing NetworkManager so server timer updates reach this client consistently
        network.NetworkManager networkManager = this.networkManager;

        if (networkManager == null) {
            // Fallback: create one if missing
            networkManager = new network.NetworkManager("localhost", 8081);
            this.networkManager = networkManager;
        }

        // Set up callbacks so messages route to the game handler (not the lobby)
        networkManager.setConnectionStatusCallback(status -> {
            javafx.application.Platform.runLater(() -> {
                System.out.println("Network Status: " + status);
            });
        });
        networkManager.setMessageCallback(message -> {
            javafx.application.Platform.runLater(() -> {
                handleMultiplayerMessage(message);
            });
        });

        // Ensure we are connected
        if (!networkManager.isConnected() && !networkManager.connect()) {
            System.err.println("Failed to connect to server");
            return;
        }

        // Determine player IDs based on host status
        String player1Id = isHost ? playerName : "Player 1";
        String player2Id = isHost ? "Player 2" : playerName;

        // Create the multiplayer game controller
        multiplayer.MultiplayerGameController controller =
                new multiplayer.MultiplayerGameController(
                        player1Id, player2Id, gameId, networkManager
                );

        // Set the current player ID for this controller instance
        String currentPlayerId = isHost ? player1Id : player2Id;
        controller.setCurrentPlayerId(currentPlayerId);

        // Connect the controller to the view
        multiplayerGameView.setGameController(controller);
        // Provide the controller a reference to the view so it can update the timer label
        controller.setGameView(multiplayerGameView);

        // Do NOT send create/join requests here; the lobby already handled it.
        // This avoids duplicate JOIN_GAME that can cause GAME_JOIN_FAILED.


        System.out.println("Multiplayer game controller created for game " + gameId +
                " with player " + playerName + " as " + (isHost ? "host" : "client"));
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


    private void handleMultiplayerMessage(network.NetworkMessage message) {
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
                System.out.println("🔄 CLIENT: Forwarding NETWORK_DATA to MainApp");
                handleNetworkData(message);
                break;
            case PLAYER_ACTION:
                handlePlayerAction(message);
                break;
            case GAME_OVER:
                handleGameOver(message);
                break;
            default:
                System.out.println("⚠️ CLIENT: Unhandled message type: " + message.getType());
                break;
        }
    }


    private void handleMultiplayerInvite(network.NetworkMessage message) {
        String data = (String) message.getData();

        if ("GAME_CREATED".equals(data)) {

            // Game created successfully, continue with multiplayer setup
        } else if ("GAME_CREATION_FAILED".equals(data)) {

            System.err.println("Failed to create a new game. Please try again.");
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

            System.err.println("No game is available to join. Please create a new game or check the game ID.");
            // Stop the multiplayer setup since join failed
            return;
        } else {
        }
    }


    private void handleMultiplayerReject(network.NetworkMessage message) {
        System.out.println("Multiplayer game rejected: " + message.getData());
        System.err.println("The other player has rejected your game invitation.");
    }

    private void handleGameStateUpdate(network.NetworkMessage message) {
        // Forward the message to the multiplayer game controller for proper handling
        if (multiplayerGameView != null && multiplayerGameView.getGameController() != null) {
            // Check if the message contains GameLevel data for network sharing
            if (message.getData() instanceof model.GameLevel) {
                model.GameLevel networkData = (model.GameLevel) message.getData();
                String opponentPlayerId = message.getPlayerId();
                multiplayerGameView.getGameController().receiveOpponentNetworkData(opponentPlayerId, networkData);
            } else {
                // Handle other game state updates
                multiplayerGameView.getGameController().updateGameState(message.getData());
            }
        }
    }

    private void handleNetworkData(network.NetworkMessage message) {
        System.out.println("📨 CLIENT: Received NETWORK_DATA from " + message.getPlayerId());
        // Forward to multiplayer game controller if available
        if (multiplayerGameView != null && multiplayerGameView.getGameController() != null) {
            model.GameLevel networkData = null;

            // Check if the message contains GameLevel data for network sharing
            if (message.getData() instanceof model.GameLevel) {
                networkData = (model.GameLevel) message.getData();
                System.out.println("✅ CLIENT: Received GameLevel directly");
            } else if (message.getData() instanceof java.util.Map) {
                // Convert LinkedHashMap back to GameLevel using Jackson
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    networkData = mapper.convertValue(message.getData(), model.GameLevel.class);
                    System.out.println("✅ CLIENT: Converted LinkedHashMap to GameLevel");
                } catch (Exception e) {
                    System.err.println("❌ CLIENT: Failed to convert LinkedHashMap to GameLevel: " + e.getMessage());
                    return;
                }
            } else {
                System.out.println("❌ CLIENT: Network data is not GameLevel or Map: " +
                        (message.getData() != null ? message.getData().getClass().getSimpleName() : "null"));
                return;
            }

            if (networkData != null) {
                String opponentPlayerId = message.getPlayerId();
                System.out.println("✅ CLIENT: Forwarding GameLevel data to controller (systems: " +
                        networkData.getSystems().size() + ", connections: " +
                        networkData.getWireConnections().size() + ")");
                multiplayerGameView.getGameController().receiveOpponentNetworkData(opponentPlayerId, networkData);
            }
        } else {
            System.out.println("❌ CLIENT: No game controller available - multiplayerGameView: " +
                    (multiplayerGameView != null) + ", controller: " +
                    (multiplayerGameView != null ? multiplayerGameView.getGameController() != null : "N/A"));
        }
    }


    private void handlePlayerAction(network.NetworkMessage message) {
        // Only log non-ammunition requests to reduce noise
        if (message.getData() != null && !message.getData().toString().contains("AMMUNITION_REQUEST")) {
            System.out.println("Player action received: " + message.getData());
        }
        // Handle player action logic
    }


    private void handleGameOver(network.NetworkMessage message) {
        System.out.println("Game over: " + message.getData());
        // Handle game over logic
    }


    public void showMultiplayerLobby() {
        if (primaryStage.getScene() == null) {
            Scene scene = new Scene(multiplayerLobbyView.getRoot());
            primaryStage.setScene(scene);
        } else {
            primaryStage.getScene().setRoot(multiplayerLobbyView.getRoot());
        }
        primaryStage.show();
    }


    private void showErrorDialog(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void showExitConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Game");
        alert.setHeaderText("Are you sure you want to exit?");
        alert.setContentText("Any unsaved progress will be lost.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (gameController != null) {
                    gameController.stopGame();
                }
                Platform.exit();
            }
        });
    }

    /**
     * Gets the game controller.
     */
    public GameController getGameController() {
        return gameController;
    }


    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public network.NetworkManager getNetworkManager() {
        return networkManager;
    }

    public static void main(String[] args) {
        System.out.println("Starting Network Simulation Game - Client Application");
        System.out.println("This client will connect to a separate server application.");
        launch(args);
    }


    private static class MainAppAdapter extends MainApp {
        private final ClientApp clientApp;

        public MainAppAdapter(ClientApp clientApp) {
            this.clientApp = clientApp;
            // Set this adapter as the static instance for proper reference management
            MainApp.setStaticInstance(this);
            // Initialize the adapter's references to match the client app
            this.gameController = clientApp.getGameController();
            this.primaryStage = clientApp.getPrimaryStage();
            System.out.println("MainAppAdapter created - gameController: " + (this.gameController != null ? "NOT NULL" : "NULL"));
            System.out.println("MainAppAdapter created - primaryStage: " + (this.primaryStage != null ? "NOT NULL" : "NULL"));
        }

        @Override
        public void showMainMenu() {
            clientApp.showMainMenu();
        }

        @Override
        public void showLeaderboard() {
            clientApp.showLeaderboard();
        }

        @Override
        public void showMultiplayerLobby() {
            clientApp.showMultiplayerLobby();
        }

        @Override
        public void startGame(String levelId) {
            // Convert levelId to GameLevel if needed
            // For now, we'll use a simple approach
            try {
                model.GameLevel level = new model.GameLevel();
                level.setLevelId(levelId);
                clientApp.startGame(level);
            } catch (Exception e) {
                System.err.println("Error starting game: " + e.getMessage());
            }
        }

        @Override
        public void createMultiplayerGame() {

            // Generate a unique game ID
            String gameId = "GAME" + String.format("%03d", (int)(Math.random() * 1000));
            // Show game ID to the player and start the game
            clientApp.showGameIdDialog(gameId, true);
        }

        @Override
        public void joinMultiplayerGame() {

            // Show dialog to ask for game ID
            clientApp.showGameIdDialog("", false);
        }

        @Override
        public void startMultiplayerGame(String gameId, String playerName, boolean isHost) {
            clientApp.showMultiplayerGame(gameId, playerName, isHost);
        }

        @Override
        public void returnToMainMenu() {
            clientApp.returnToMainMenu();
        }

        @Override
        public void restartCurrentLevel() {
            clientApp.restartCurrentLevel();
        }

        @Override
        public GameController getGameController() {
            return clientApp.getGameController();
        }

        @Override
        public Stage getPrimaryStage() {
            return clientApp.getPrimaryStage();
        }

        @Override
        public network.NetworkManager getNetworkManager() {
            return clientApp.getNetworkManager();
        }
    }
}



