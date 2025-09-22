package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


public class MultiplayerLobbyView {
    private StackPane root;
    private TextField playerNameField;
    private TextField gameIdField;
    private Button createGameButton;
    private Button joinGameButton;
    private Button refreshButton;
    private Button helpButton;
    private Button startGameButton;
    private Label createGameStatus;
    private Label joinGameStatus;
    private ListView<String> activeGamesList;

    private String currentGameId;
    private String currentPlayerName;
    private boolean isHost;
    private boolean gameStarted = false;

    private app.MainApp mainApp;

    public MultiplayerLobbyView(app.MainApp mainApp) {
        this.mainApp = mainApp;
        this.root = new StackPane();
        this.root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        initializeUI();
    }

    private void initializeUI() {
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(20));

        // Title
        Text title = new Text("Multiplayer Lobby");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(Color.CYAN);

        // Player name input
        HBox playerNameBox = new HBox(10);
        playerNameBox.setAlignment(Pos.CENTER);

        Label playerNameLabel = new Label("Player Name:");
        playerNameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        playerNameLabel.setTextFill(Color.WHITE);

        playerNameField = new TextField();
        playerNameField.setPromptText("Enter your name");
        playerNameField.setPrefWidth(200);

        playerNameBox.getChildren().addAll(playerNameLabel, playerNameField);

        // Create panels
        HBox panelsContainer = new HBox(20);
        panelsContainer.setAlignment(Pos.CENTER);

        VBox createGamePanel = createCreateGamePanel();
        VBox joinGamePanel = createJoinGamePanel();
        VBox activeGamesPanel = createActiveGamesPanel();

        panelsContainer.getChildren().addAll(createGamePanel, joinGamePanel, activeGamesPanel);

        // Control buttons
        HBox controlButtons = new HBox(10);
        controlButtons.setAlignment(Pos.CENTER);

        Button backToMenuButton = createStyledButton("Back to Menu");
        backToMenuButton.setOnAction(e -> backToMainMenu());

        refreshButton = createStyledButton("Refresh Games");
        refreshButton.setOnAction(e -> refreshActiveGames());

        helpButton = createStyledButton("Help");
        helpButton.setOnAction(e -> showHelpDialog());

        controlButtons.getChildren().addAll(backToMenuButton, refreshButton, helpButton);

        mainContainer.getChildren().addAll(title, playerNameBox, panelsContainer, controlButtons);
        root.getChildren().add(mainContainer);
    }

    private VBox createCreateGamePanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: rgba(0, 100, 0, 0.3); -fx-background-radius: 8;");
        panel.setPadding(new Insets(15));

        // Title
        Text title = new Text("Create Game");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setFill(Color.LIGHTGREEN);

        // Create game button
        createGameButton = new Button("Create New Game");
        createGameButton.setStyle(
                "-fx-background-color: green;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        createGameButton.setPrefWidth(200);
        createGameButton.setPrefHeight(40);
        createGameButton.setOnAction(e -> createNewGame());

        // Start game button (initially hidden)
        startGameButton = new Button("Start Game");
        startGameButton.setStyle(
                "-fx-background-color: orange;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        startGameButton.setPrefWidth(200);
        startGameButton.setPrefHeight(40);
        startGameButton.setVisible(false);
        startGameButton.setOnAction(e -> startMultiplayerGame());

        // Status label
        createGameStatus = new Label("");
        createGameStatus.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        createGameStatus.setTextFill(Color.YELLOW);
        createGameStatus.setWrapText(true);

        panel.getChildren().addAll(title, createGameButton, startGameButton, createGameStatus);
        return panel;
    }

    private VBox createJoinGamePanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: rgba(100, 0, 0, 0.3); -fx-background-radius: 8;");
        panel.setPadding(new Insets(15));

        // Title
        Text title = new Text("Join Game");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setFill(Color.RED);

        // Game ID input
        Label gameIdLabel = new Label("Game ID:");
        gameIdLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gameIdLabel.setTextFill(Color.WHITE);

        gameIdField = new TextField();
        gameIdField.setPromptText("Enter game ID");
        gameIdField.setPrefWidth(200);

        // Join game button
        joinGameButton = new Button("Join Game");
        joinGameButton.setStyle(
                "-fx-background-color: red;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        joinGameButton.setPrefWidth(200);
        joinGameButton.setPrefHeight(40);
        joinGameButton.setOnAction(e -> joinExistingGame());

        // Status label
        joinGameStatus = new Label("");
        joinGameStatus.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        joinGameStatus.setTextFill(Color.YELLOW);
        joinGameStatus.setWrapText(true);

        panel.getChildren().addAll(title, gameIdLabel, gameIdField, joinGameButton, joinGameStatus);
        return panel;
    }


    private VBox createActiveGamesPanel() {
        VBox panel = new VBox(15);
        panel.setAlignment(Pos.CENTER);
        panel.setPrefWidth(300);
        panel.setStyle("-fx-background-color: rgba(0, 0, 100, 0.3); -fx-background-radius: 8;");
        panel.setPadding(new Insets(15));

        // Title
        Text title = new Text("Active Games");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setFill(Color.LIGHTBLUE);

        // Active games list
        activeGamesList = new ListView<>();
        activeGamesList.setPrefHeight(150);
        activeGamesList.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        // Join selected game button
        Button joinSelectedButton = new Button("Join Selected");
        joinSelectedButton.setStyle(
                "-fx-background-color: blue;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        joinSelectedButton.setPrefWidth(200);
        joinSelectedButton.setOnAction(e -> joinSelectedGame());

        panel.getChildren().addAll(title, activeGamesList, joinSelectedButton);

        // Don't populate immediately - wait for user interaction or manual refresh
        activeGamesList.getItems().add("Click 'Refresh Games' to load active games");

        return panel;
    }

    private Button createStyledButton(String text) {
        Button button = new Button(text);
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: cyan;" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: cyan;" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 2;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 2;" +
                            "-fx-text-fill: cyan;" +
                            "-fx-cursor: hand;"
            );
        });

        return button;
    }

    private void createNewGame() {
        createNewGame(null);
    }

    private void createNewGame(String providedGameId) {
        String playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            createGameStatus.setText("Please enter your name");
            return;
        }

        // Use provided Game ID or generate a new one
        if (providedGameId != null && !providedGameId.isEmpty()) {
            currentGameId = providedGameId;
        } else {
            currentGameId = generateGameId();
        }
        currentPlayerName = playerName;
        isHost = true;

        // Show the generated Game ID immediately
        createGameStatus.setText("Game ID: " + currentGameId + " - Creating...");
        // Also show the Game ID in the join field for easy copying
        gameIdField.setText(currentGameId);
        createGameButton.setDisable(true);

        // Use the existing network manager from MainApp
        network.NetworkManager networkManager = mainApp.getNetworkManager();
        if (networkManager == null) {
            createGameStatus.setText("Network manager not available");
            createGameButton.setDisable(false);
            return;
        }

        // Set up message callback for lobby responses (only if game hasn't started)
        if (mainApp == null || mainApp.getMultiplayerGameView() == null ||
                mainApp.getMultiplayerGameView().getGameController() == null) {
            networkManager.setMessageCallback(message -> {
                javafx.application.Platform.runLater(() -> {
                    handleLobbyMessage(message);
                });
            });
        } else {

        }

        // Check if already connected, if not attempt to connect
        if (!networkManager.isConnected()) {

            if (!networkManager.connect()) {

                createGameStatus.setText("Failed to connect to server");
                createGameButton.setDisable(false);
                return;
            }

        } else {

        }

        // Send create game request to server
        network.NetworkMessage createMessage = new network.NetworkMessage(
                network.NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                currentPlayerName,
                currentGameId,
                "CREATE_GAME"
        );

        if (networkManager.sendMessage(createMessage)) {
            createGameStatus.setText("Game creation request sent...");
        } else {
            createGameStatus.setText("Failed to send game creation request");
            createGameButton.setDisable(false);
        }
    }


    private void joinExistingGame() {
        String gameId = gameIdField.getText().trim();
        if (gameId.isEmpty()) {
            joinGameStatus.setText("Please enter a game ID");
            return;
        }

        String playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            joinGameStatus.setText("Please enter your name");
            return;
        }

        currentGameId = gameId;
        currentPlayerName = playerName;
        isHost = false;

        joinGameStatus.setText("Joining game...");
        joinGameButton.setDisable(true);

        // Use the existing network manager from MainApp
        network.NetworkManager networkManager = mainApp.getNetworkManager();
        if (networkManager == null) {
            joinGameStatus.setText("Network manager not available");
            joinGameButton.setDisable(false);
            return;
        }

        // Set up message callback for lobby responses (only if game hasn't started)
        if (mainApp == null || mainApp.getMultiplayerGameView() == null ||
                mainApp.getMultiplayerGameView().getGameController() == null) {
            networkManager.setMessageCallback(message -> {
                javafx.application.Platform.runLater(() -> {
                    handleLobbyMessage(message);
                });
            });
        } else {

        }

        // Check if already connected, if not attempt to connect
        if (!networkManager.isConnected()) {

            if (!networkManager.connect()) {

                joinGameStatus.setText("Failed to connect to server");
                joinGameButton.setDisable(false);
                return;
            }

        } else {

        }

        // Send join game request to server
        network.NetworkMessage joinMessage = new network.NetworkMessage(
                network.NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                currentPlayerName,
                currentGameId,
                "JOIN_GAME"
        );

        if (networkManager.sendMessage(joinMessage)) {
            joinGameStatus.setText("Join request sent...");
        } else {
            joinGameStatus.setText("Failed to send join request");
            joinGameButton.setDisable(false);
        }
    }

    private void joinSelectedGame() {
        String selectedGame = activeGamesList.getSelectionModel().getSelectedItem();
        if (selectedGame == null) {
            return;
        }

        // Check if this is a real game or a status message
        if (selectedGame.contains("No active games") ||
                selectedGame.contains("Loading") ||
                selectedGame.contains("Failed") ||
                selectedGame.contains("Network not available")) {
            // Don't try to join status messages
            return;
        }

        // Extract game ID from the display string
        String gameId = selectedGame.split(" - ")[0];
        gameIdField.setText(gameId);
        joinExistingGame();
    }

    private void populateActiveGamesList() {

        // Clear existing items
        activeGamesList.getItems().clear();

        // Show loading message
        activeGamesList.getItems().add("Loading active games...");

        // Request active games from server
        requestActiveGamesFromServer();
    }

    private void requestActiveGamesFromServer() {

        network.NetworkManager networkManager = mainApp.getNetworkManager();
        if (networkManager == null) {

            activeGamesList.getItems().clear();
            activeGamesList.getItems().add("Network not available");
            return;
        }


        // Set up message callback for lobby responses (only if game hasn't started)
        if (mainApp == null || mainApp.getMultiplayerGameView() == null ||
                mainApp.getMultiplayerGameView().getGameController() == null) {
            networkManager.setMessageCallback(message -> {
                javafx.application.Platform.runLater(() -> {
                    handleLobbyMessage(message);
                });
            });
        } else {

        }

        // Check if already connected, if not attempt to connect
        if (!networkManager.isConnected()) {

            if (!networkManager.connect()) {

                activeGamesList.getItems().clear();
                activeGamesList.getItems().add("Failed to connect to server");
                return;
            }

        } else {

        }

        // Send request for active games
        network.NetworkMessage requestMessage = new network.NetworkMessage(
                network.NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                "CLIENT",
                "REFRESH",
                "GET_ACTIVE_GAMES"
        );

        if (networkManager.sendMessage(requestMessage)) {

        } else {

            activeGamesList.getItems().clear();
            activeGamesList.getItems().add("Failed to request games");
        }
    }

    private void refreshActiveGames() {

        populateActiveGamesList();
    }

    public void handleLobbyMessage(network.NetworkMessage message) {
        switch (message.getType()) {
            case MULTIPLAYER_INVITE:
                if ("GAME_CREATED".equals(message.getData())) {
                    createGameStatus.setText("Game created successfully! ID: " + currentGameId);
                    // Show the start game button
                    startGameButton.setVisible(true);
                    // Refresh the active games list to show the new game
                    populateActiveGamesList();
                    // Re-enable the create button for creating another game
                    createGameButton.setDisable(false);
                } else if ("GAME_CREATION_FAILED".equals(message.getData())) {
                    createGameStatus.setText("Failed to create game");
                    createGameButton.setDisable(false);
                }
                break;
            case MULTIPLAYER_ACCEPT:
                if ("GAME_JOINED".equals(message.getData())) {
                    // Update the current game ID to match the server's session ID
                    if (message.getSessionId() != null && !message.getSessionId().isEmpty()) {
                        currentGameId = message.getSessionId();
                    }

                    joinGameStatus.setText("Joined game " + currentGameId);
                    // Start the multiplayer game as client (only once)
                    if (!gameStarted) {
                        startMultiplayerGame();
                    }
                } else if ("PLAYER_JOINED".equals(message.getData())) {
                    // Host receives this when a player joins their game
                    // Update the current game ID to match the server's session ID
                    if (message.getSessionId() != null && !message.getSessionId().isEmpty()) {
                        currentGameId = message.getSessionId();
                    }

                    createGameStatus.setText("Player joined! Click 'Start Game' to begin.");
                    // Don't start the game automatically - let the host decide when to start
                } else if ("GAME_JOIN_FAILED".equals(message.getData())) {
                    joinGameStatus.setText("Failed to join game");
                    joinGameButton.setDisable(false);
                } else if ("SESSION_NOT_FOUND".equals(message.getData())) {
                    joinGameStatus.setText("Invalid Game ID: Game not found");
                    showErrorDialog("Invalid Game ID", "Game Not Found",
                            "The Game ID you entered does not exist. Please check the Game ID and try again, or create a new game.");
                    joinGameButton.setDisable(false);
                } else if ("NOT_AUTHORIZED".equals(message.getData())) {
                    joinGameStatus.setText("Not authorized to join this game");
                    showErrorDialog("Join Game Failed", "Not Authorized",
                            "You are not authorized to join this game. The game may be full or you may already be in the session.");
                    joinGameButton.setDisable(false);
                } else if ("SESSION_INACTIVE".equals(message.getData())) {
                    joinGameStatus.setText("Game session is inactive");
                    showErrorDialog("Join Game Failed", "Game Session Inactive",
                            "This game session is no longer active. Please create a new game or find another active session.");
                    joinGameButton.setDisable(false);
                } else {
                    joinGameStatus.setText("Unexpected server response: " + message.getData());
                    showErrorDialog("Join Game Failed", "Unexpected Response",
                            "Received an unexpected response from the server. Please try again.");
                    joinGameButton.setDisable(false);
                }
                break;
            case GAME_STATE_UPDATE:
                if (message.getData() instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> gamesList = (java.util.List<String>) message.getData();
                    // Update active games list with real data from server
                    updateActiveGamesList(gamesList);
                } else {
                    // Only handle timer updates in lobby if the game hasn't started yet
                    // If the game has started, timer updates should be handled by the game controller
                    if (mainApp != null && mainApp.getMultiplayerGameView() != null &&
                            mainApp.getMultiplayerGameView().getGameController() != null) {

                        mainApp.getMultiplayerGameView().getGameController().updateGameState(message.getData());
                    } else {

                        handleTimerUpdate(message.getData());
                    }
                }
                break;
            default:
                System.out.println("Unhandled lobby message: " + message.getType());
                break;
        }
    }

    private void updateActiveGamesList(java.util.List<String> games) {
        activeGamesList.getItems().clear();
        activeGamesList.getItems().addAll(games);
    }


    private void handleTimerUpdate(Object timerData) {

        // Check if this is a timer update with remaining time
        if (timerData instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> timerMap = (java.util.Map<String, Object>) timerData;

            if (timerMap.containsKey("remainingSetupTime")) {
                Object remainingTime = timerMap.get("remainingSetupTime");
                if (remainingTime instanceof Number) {
                    long remainingMs = ((Number) remainingTime).longValue();
                    long remainingSeconds = remainingMs / 1000;


                    // Update UI with remaining time if needed
                    // For now, just log it - you can add UI updates here later
                    if (remainingSeconds > 0) {
                    } else {

                    }
                }
            }
        }
    }


    private String generateGameId() {
        return "GAME" + String.format("%03d", (int)(Math.random() * 1000));
    }


    private void startMultiplayerGame() {
        // Create a proper multiplayer session (only once)
        if (!gameStarted) {
            gameStarted = true;


            // Start the full multiplayer experience with all Phase 3 features
            mainApp.startMultiplayerGame(currentGameId, currentPlayerName, isHost);

            // The MultiplayerGameController will handle:
            // - 30-second setup phase with timer synchronization
            // - Controllable reference systems with ammunition
            // - Penalty system (Wrath of Penia/Aergia)
            // - Network visibility system
            // - Packet color differentiation
            // - Cooldown systems
            // - Feedback loop mechanics
        } else {

        }
    }

    private void backToMainMenu() {
        if (mainApp != null) {
            mainApp.showMainMenu();
        }
    }


    public void setGameIdAndJoin(String gameId) {
        if (gameIdField != null) {
            gameIdField.setText(gameId);
            // Trigger the join process
            joinExistingGame();
        } else {
            System.err.println("ERROR: gameIdField is null, cannot set game ID");
        }
    }


    public void setGameIdAndCreate(String gameId) {
        if (gameIdField != null) {
            gameIdField.setText(gameId);
            // Set player name if not already set
            if (playerNameField != null && playerNameField.getText().trim().isEmpty()) {
                playerNameField.setText("Player 1");
            }
            // Trigger the create process with the provided Game ID
            createNewGame(gameId);
        } else {
            System.err.println("ERROR: gameIdField is null, cannot set game ID");
        }
    }


    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Multiplayer Help");
        alert.setHeaderText("How to Play Multiplayer");
        alert.setContentText(
                "1. Create Game: Enter your name and click 'Create New Game' to host a game\n" +
                        "2. Join Game: Enter a game ID and your name to join an existing game\n" +
                        "3. Active Games: Browse and join games from the list\n" +
                        "4. Game Setup: You have 30 seconds to design your network\n" +
                        "5. Competition: Compete to deliver more packets than your opponent"
        );
        alert.showAndWait();
    }

    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public StackPane getRoot() {
        return root;
    }
}



