package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import app.MainApp;
import network.NetworkManager;

public class MainMenuView {
    private MainApp mainApp;
    private StackPane root;
    private VBox menuContainer;
    private VBox networkContainer;
    private VBox gameOptionsContainer;

    // Network controls
    private Label connectionStatusLabel;
    private Label serverAddressLabel;
    private TextField serverAddressField;
    private Button connectButton;
    private Button disconnectButton;
    private Button reconnectButton;

    // Game options
    private Button freshStartButton;
    private Button gameLevelsButton;
    private Button multiplayerButton;
    private Button createGameButton;
    private Button joinGameButton;
    private Button leaderboardButton;
    private Button gameSettingsButton;
    private Button exitGameButton;

    public MainMenuView(MainApp mainApp) {
        this.mainApp = mainApp;
        System.out.println("MainMenuView created with MainApp instance: " + (mainApp != null ? mainApp.hashCode() : "NULL"));
        System.out.println("MainMenuView - mainApp.gameController: " + (mainApp != null && mainApp.getGameController() != null ? "NOT NULL" : "NULL"));
        System.out.println("MainMenuView - mainApp.primaryStage: " + (mainApp != null && mainApp.getPrimaryStage() != null ? "NOT NULL" : "NULL"));

        // Try to get the correct instance if the provided one is invalid
        if (mainApp == null || mainApp.getGameController() == null) {
            System.out.println("MainMenuView: Invalid MainApp provided, trying to get correct instance");
            MainApp correctInstance = MainApp.getInstance();
            if (correctInstance != null && correctInstance.getGameController() != null) {
                System.out.println("MainMenuView: Found correct instance, updating reference");
                this.mainApp = correctInstance;
            }
        }

        initializeUI();
    }

    public void updateMainAppReference() {
        MainApp correctInstance = MainApp.getInstance();
        if (correctInstance != null && correctInstance.getGameController() != null) {
            System.out.println("MainMenuView: Updating MainApp reference to correct instance");
            this.mainApp = correctInstance;
        }
    }

    private void initializeUI() {
        // Create root container
        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        // Create background rectangle
        Rectangle background = new Rectangle(1000, 700);
        background.setFill(Color.TRANSPARENT);
        background.setStroke(Color.CYAN);
        background.setStrokeWidth(2);

        // Create main menu container
        menuContainer = new VBox(20);
        menuContainer.setAlignment(Pos.CENTER);
        menuContainer.setPadding(new Insets(30));

        // Create title
        Text title = new Text("Network Simulation Game");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setFill(Color.CYAN);

        // Create subtitle
        Text subtitle = new Text("Advanced Programming Course Project - Phase 3");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitle.setFill(Color.LIGHTGRAY);

        // Create network status section
        createNetworkSection();

        // Create game options section
        createGameOptionsSection();

        // Add components to menu container
        menuContainer.getChildren().addAll(
                title, subtitle,
                networkContainer,
                gameOptionsContainer
        );

        // Add everything to root
        root.getChildren().addAll(background, menuContainer);
    }

    private void createNetworkSection() {
        networkContainer = new VBox(10);
        networkContainer.setAlignment(Pos.CENTER);
        networkContainer.setPadding(new Insets(20));
        networkContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");

        // Network section title
        Text networkTitle = new Text("Network Connection");
        networkTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        networkTitle.setFill(Color.CYAN);

        // Connection status
        connectionStatusLabel = new Label("Status: Disconnected");
        connectionStatusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        connectionStatusLabel.setTextFill(Color.RED);

        // Server address input
        HBox serverInputBox = new HBox(10);
        serverInputBox.setAlignment(Pos.CENTER);

        Label serverLabel = new Label("Server:");
        serverLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        serverLabel.setTextFill(Color.WHITE);

        serverAddressField = new TextField("localhost:8081");
        serverAddressField.setPrefWidth(150);
        serverAddressField.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-text-fill: white;" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 1;"
        );

        serverInputBox.getChildren().addAll(serverLabel, serverAddressField);

        // Connection buttons
        HBox connectionButtonsBox = new HBox(10);
        connectionButtonsBox.setAlignment(Pos.CENTER);

        connectButton = createNetworkButton("Connect");
        disconnectButton = createNetworkButton("Disconnect");
        reconnectButton = createNetworkButton("Reconnect");

        // Set up button actions
        connectButton.setOnAction(e -> handleConnect());
        disconnectButton.setOnAction(e -> handleDisconnect());
        reconnectButton.setOnAction(e -> handleReconnect());

        disconnectButton.setDisable(true);
        reconnectButton.setDisable(true);

        connectionButtonsBox.getChildren().addAll(connectButton, disconnectButton, reconnectButton);

        // Add to network container
        networkContainer.getChildren().addAll(
                networkTitle, connectionStatusLabel, serverInputBox, connectionButtonsBox
        );
    }

    private void createGameOptionsSection() {
        gameOptionsContainer = new VBox(15);
        gameOptionsContainer.setAlignment(Pos.CENTER);
        gameOptionsContainer.setPadding(new Insets(20));
        gameOptionsContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");

        // Game options title
        Text gameOptionsTitle = new Text("Game Options");
        gameOptionsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gameOptionsTitle.setFill(Color.CYAN);

        // Single player options
        HBox singlePlayerBox = new HBox(10);
        singlePlayerBox.setAlignment(Pos.CENTER);

        freshStartButton = createMenuButton("Fresh Start");
        freshStartButton.setOnAction(e -> {
            System.out.println("Fresh Start button clicked");
            System.out.println("mainApp reference: " + (mainApp != null ? mainApp.hashCode() : "NULL"));

            // Try to update the reference first
            updateMainAppReference();

            // Try instance method first
            if (mainApp != null && mainApp.getGameController() != null) {
                System.out.println("Using instance method for Fresh Start");
                mainApp.startFreshGame("level1");
            } else {
                System.out.println("Instance method failed, trying static method");
                // Use static method as fallback
                MainApp.startFreshGameStatic("level1");
            }
        });

        gameLevelsButton = createMenuButton("Game Levels");
        gameLevelsButton.setOnAction(e -> {
            // Use static instance as fallback if mainApp reference is corrupted
            MainApp appToUse = mainApp;
            if (appToUse == null) {
                appToUse = MainApp.getInstance();
            }
            if (appToUse != null) {
                appToUse.showLevelSelect();
            } else {
                System.err.println("ERROR: No valid MainApp instance found for Game Levels!");
            }
        });

        singlePlayerBox.getChildren().addAll(freshStartButton, gameLevelsButton);

        // Multiplayer options
        HBox multiplayerBox = new HBox(10);
        multiplayerBox.setAlignment(Pos.CENTER);

        multiplayerButton = createMenuButton("Multiplayer Mode");
        multiplayerButton.setOnAction(e -> showMultiplayerOptions());

        createGameButton = createMenuButton("Create Game");
        createGameButton.setOnAction(e -> {
            // Use static instance as fallback if mainApp reference is corrupted
            MainApp appToUse = mainApp;
            if (appToUse == null) {
                appToUse = MainApp.getInstance();
            }
            if (appToUse != null) {
                appToUse.createMultiplayerGame();
            } else {
                System.err.println("ERROR: No valid MainApp instance found for Create Game!");
            }
        });
        createGameButton.setDisable(true);

        joinGameButton = createMenuButton("Join Game");
        joinGameButton.setOnAction(e -> {
            // Use static instance as fallback if mainApp reference is corrupted
            MainApp appToUse = mainApp;
            if (appToUse == null) {
                appToUse = MainApp.getInstance();
            }
            if (appToUse != null) {
                appToUse.joinMultiplayerGame();
            } else {
                System.err.println("ERROR: No valid MainApp instance found for Join Game!");
            }
        });
        joinGameButton.setDisable(true);

        multiplayerBox.getChildren().addAll(multiplayerButton, createGameButton, joinGameButton);

        // Other options
        HBox otherOptionsBox = new HBox(10);
        otherOptionsBox.setAlignment(Pos.CENTER);

        leaderboardButton = createMenuButton("Leaderboard");
        leaderboardButton.setOnAction(e -> {
            // Use static instance as fallback if mainApp reference is corrupted
            MainApp appToUse = mainApp;
            if (appToUse == null) {
                appToUse = MainApp.getInstance();
            }
            if (appToUse != null) {
                appToUse.showLeaderboard();
            } else {
                System.err.println("ERROR: No valid MainApp instance found for Leaderboard!");
            }
        });

        gameSettingsButton = createMenuButton("Game Settings");
        gameSettingsButton.setOnAction(e -> {
            // Use static instance as fallback if mainApp reference is corrupted
            MainApp appToUse = mainApp;
            if (appToUse == null) {
                appToUse = MainApp.getInstance();
            }
            if (appToUse != null) {
                appToUse.showSettings();
            } else {
                System.err.println("ERROR: No valid MainApp instance found!");
            }
        });

        exitGameButton = createMenuButton("Exit Game");
        exitGameButton.setOnAction(e -> {
            // Use static instance as fallback if mainApp reference is corrupted
            MainApp appToUse = mainApp;
            if (appToUse == null) {
                appToUse = MainApp.getInstance();
            }
            if (appToUse != null && appToUse.getPrimaryStage() != null) {
                appToUse.getPrimaryStage().fireEvent(
                        new javafx.stage.WindowEvent(appToUse.getPrimaryStage(),
                                javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
            } else {
                System.err.println("ERROR: No valid MainApp instance or primaryStage found for Exit Game!");
            }
        });

        otherOptionsBox.getChildren().addAll(leaderboardButton, gameSettingsButton, exitGameButton);

        // Add to game options container
        gameOptionsContainer.getChildren().addAll(
                gameOptionsTitle, singlePlayerBox, multiplayerBox, otherOptionsBox
        );
    }

    private void showMultiplayerOptions() {
        // Check if network is connected before enabling multiplayer buttons
        boolean isConnected = connectionStatusLabel.getText().contains("Connected");

        if (isConnected) {
            createGameButton.setDisable(false);
            joinGameButton.setDisable(false);
            multiplayerButton.setStyle(
                    "-fx-background-color: rgba(0, 255, 0, 0.3);" +
                            "-fx-border-color: lime;" +
                            "-fx-border-width: 2;" +
                            "-fx-text-fill: lime;"
            );
        } else {
            // Show error message if not connected
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Network Required");
            alert.setHeaderText("Connection Required");
            alert.setContentText("You must be connected to the server to use multiplayer features. Please connect first.");
            alert.showAndWait();
        }
    }

    private Button createNetworkButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(100);
        button.setPrefHeight(35);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: cyan;" +
                        "-fx-cursor: hand;"
        );

        // Hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(0, 255, 255, 0.2);" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: cyan;" +
                            "-fx-cursor: hand;"
            );
        });

        return button;
    }

    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.setPrefWidth(150);
        button.setPrefHeight(40);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 2;" +
                        "-fx-text-fill: cyan;" +
                        "-fx-cursor: hand;"
        );

        // Hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(0, 255, 255, 0.2);" +
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

    private void handleConnect() {
        try {
            String serverAddress = serverAddressField.getText();
            String[] parts = serverAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            // Create network manager and attempt connection
            NetworkManager networkManager = new NetworkManager(host, port);
            networkManager.setConnectionStatusCallback(status -> {
                if (status.contains("Connected")) {
                    updateConnectionStatus(status, true);
                } else {
                    updateConnectionStatus(status, false);
                }
            });

            // Attempt connection in background thread to avoid blocking UI
            new Thread(() -> {
                boolean success = networkManager.connect();
                if (success) {
                    // Store network manager reference for later use
                    mainApp.setNetworkManager(networkManager);
                    javafx.application.Platform.runLater(() -> {
                        updateConnectionStatus("Connected", true);
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        updateConnectionStatus("Connection Failed", false);
                    });
                }
            }).start();

        } catch (Exception e) {
            updateConnectionStatus("Invalid Server Address", false);
        }
    }

    private void handleDisconnect() {
        // Get the network manager from MainApp and disconnect
        NetworkManager networkManager = mainApp.getNetworkManager();
        if (networkManager != null && networkManager.isConnected()) {
            networkManager.disconnect();
            updateConnectionStatus("Disconnected", false);
        } else {
            updateConnectionStatus("Not connected", false);
        }
    }

    private void handleReconnect() {
        handleDisconnect();
        try {
            Thread.sleep(1000); // Wait 1 second before reconnecting
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        handleConnect();
    }

    public void updateConnectionStatus(String status, boolean isConnected) {
        javafx.application.Platform.runLater(() -> {
            connectionStatusLabel.setText("Status: " + status);
            if (isConnected) {
                connectionStatusLabel.setTextFill(Color.GREEN);
                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
                reconnectButton.setDisable(true);
                // Don't automatically enable multiplayer features - they should only be enabled
                // when the Multiplayer Mode button is clicked AND connected
            } else {
                connectionStatusLabel.setTextFill(Color.RED);
                connectButton.setDisable(false);
                disconnectButton.setDisable(true);
                reconnectButton.setDisable(false);
                // Disable multiplayer features when disconnected
                createGameButton.setDisable(true);
                joinGameButton.setDisable(true);
            }
        });
    }

    public StackPane getRoot() {
        return root;
    }

    // Getters for network controls
    public TextField getServerAddressField() { return serverAddressField; }
    public Button getConnectButton() { return connectButton; }
    public Button getDisconnectButton() { return disconnectButton; }
    public Button getReconnectButton() { return reconnectButton; }
}

