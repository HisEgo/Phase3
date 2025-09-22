package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import app.MainApp;
import controller.GameController;
import model.*;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.List;


public class MultiplayerGameView {
    private MainApp mainApp;
    private StackPane root;
    private VBox mainContainer;

    // Game state panels
    private VBox setupPhasePanel;
    private VBox gamePhasePanel;
    private VBox opponentNetworkPanel;
    private VBox playerActionsPanel;
    private VBox gameStatusPanel;

    // Network visibility components
    private Label opponentNetworkStatusLabel;
    private Label opponentScoreLabel;
    private Label opponentAmmunitionLabel;
    private ProgressBar opponentNetworkHealthBar;

    // Setup phase controls
    private Label setupTimerLabel;
    private Button readyButton;
    private Label setupInstructionsLabel;

    // Game phase controls
    private Label gameTimerLabel;
    private Label player1ScoreLabel;
    private Label player2ScoreLabel;
    private VBox controllableSystemsBox;
    private VBox ammunitionBox;
    private VBox cooldownTimersBox;

    // Game status
    private Label gamePhaseLabel;
    private Label penaltyStatusLabel;
    private Label networkStatusLabel;

    // Control buttons
    private Button pauseButton;
    private Button surrenderButton;
    private Button backToMenuButton;

    // System selection
    private String selectedSystemId;
    private VBox ammunitionPanel;
    private boolean isAmmunitionPanelVisible;

    // Targeting system
    private String selectedTargetSystemId;
    private Point2D selectedTargetPosition;
    private boolean isTargetingMode;
    private Canvas targetingCanvas;
    private GraphicsContext targetingGC;

    // Real-time UI updates
    private Timeline uiUpdateTimer;
    private Map<String, Label> ammunitionLabels;
    private Map<String, Label> cooldownLabels;

    // Cooldown tracking
    private Map<String, Double> systemCooldowns;
    private Map<String, Double> packetCooldowns;
    private Map<String, Button> ammunitionButtons;

    // Opponent network visibility
    private Canvas opponentNetworkCanvas;
    private GraphicsContext opponentNetworkGC;

    // Temporal progress feature
    private Button temporalProgressButton;
    private boolean temporalProgressEnabled;
    private VBox temporalProgressPanel;

    // Temporal analysis components
    private VBox temporalAnalysisOverlay;
    private javafx.animation.Timeline temporalAnalysisTimer;

    // Game controller reference
    private multiplayer.MultiplayerGameController gameController;

    // Current player information
    private String currentPlayerId;

    // Network building interface (from GameView)
    private Canvas networkCanvas;
    private GraphicsContext networkGC;
    private GameController networkGameController;
    private GameLevel currentLevel;

    // Wire preview for network building
    private Point2D wirePreviewStart = null;
    private Point2D wirePreviewEnd = null;
    private boolean showWirePreview = false;

    public MultiplayerGameView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.ammunitionLabels = new HashMap<>();
        this.cooldownLabels = new HashMap<>();
        this.systemCooldowns = new HashMap<>();
        this.packetCooldowns = new HashMap<>();
        this.ammunitionButtons = new HashMap<>();
        this.temporalProgressEnabled = false;
        initializeUI();
    }

    public void setCurrentPlayerId(String playerId) {
        this.currentPlayerId = playerId;
    }


    public String getCurrentPlayerId() {
        // If currentPlayerId is null, try to get it from the game controller
        if (currentPlayerId == null && gameController != null) {
            // Try to determine player ID from game controller
            String controllerPlayerId = gameController.getCurrentPlayerId();
            if (controllerPlayerId != null) {
                currentPlayerId = controllerPlayerId;
                return currentPlayerId;
            }
        }
        return currentPlayerId;
    }


    public Label getSetupTimerLabel() {
        return setupTimerLabel;
    }

    public Label getGamePhaseLabel() {
        return gamePhaseLabel;
    }

    public Label getSetupInstructionsLabel() {
        return setupInstructionsLabel;
    }


    public Button getReadyButton() {
        return readyButton;
    }

    public void updateSetupTimerDisplay(long remainingTimeMs) {
        if (setupTimerLabel != null) {
            long remainingSeconds = remainingTimeMs / 1000;
            String timerText = String.format("Time Remaining: %02d:%02d",
                    remainingSeconds / 60, remainingSeconds % 60);
            setupTimerLabel.setText(timerText);

            // Add visual feedback for low time
            if (remainingSeconds <= 10) {
                setupTimerLabel.setStyle(
                        "-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 16px;"
                );
            } else if (remainingSeconds <= 20) {
                setupTimerLabel.setStyle(
                        "-fx-text-fill: orange; -fx-font-weight: bold; -fx-font-size: 15px;"
                );
            } else {
                setupTimerLabel.setStyle(
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;"
                );
            }

        } else {
        }
    }

    public void resetToInitialState() {
        // Reset game state
        if (gameController != null) {
            gameController.stopGame();
        }

        // Reset network state
        if (currentLevel != null) {
            currentLevel.getWireConnections().clear();
        }

        // Reset UI state
        selectedSystemId = null;
        isAmmunitionPanelVisible = false;
        if (ammunitionPanel != null) {
            root.getChildren().remove(ammunitionPanel);
            ammunitionPanel = null;
        }

        // Reset wire preview state
        showWirePreview = false;
        wirePreviewStart = null;
        wirePreviewEnd = null;

        // Reset cooldown states
        systemCooldowns.clear();
        packetCooldowns.clear();
        ammunitionButtons.clear();

        // Reset opponent network visibility
        if (opponentNetworkCanvas != null) {
            opponentNetworkCanvas.setVisible(false);
        }

        // Reset temporal progress
        temporalProgressEnabled = false;
        if (temporalProgressButton != null) {
            temporalProgressButton.setDisable(false);
            temporalProgressButton.setText("Analyze Network State");
            temporalProgressButton.setStyle(
                    "-fx-background-color: rgba(0, 255, 255, 0.3);" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;"
            );
        }

        // Reset player ready states
        if (readyButton != null) {
            readyButton.setDisable(false);
            readyButton.setText("Ready to Start");
            readyButton.setStyle(
                    "-fx-background-color: lime;" +
                            "-fx-text-fill: black;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;"
            );
        }

        // Reset phase visibility
        if (setupPhasePanel != null) {
            setupPhasePanel.setVisible(true);
        }
        if (gamePhasePanel != null) {
            gamePhasePanel.setVisible(false);
        }

        // Reset status labels
        if (gamePhaseLabel != null) {
            gamePhaseLabel.setText("Phase: Setup");
        }
        if (setupInstructionsLabel != null) {
            setupInstructionsLabel.setText("Design your network! You have 60 seconds total (30s + penalty phases).");
        }

        // Reinitialize network building
        if (networkCanvas != null) {
            initializeNetworkGameController();
            renderNetworkBuilding();
        }


    }

    public void setGameController(multiplayer.MultiplayerGameController controller) {
        this.gameController = controller;

        // If currentPlayerId is not set, try to determine it from the controller
        if (currentPlayerId == null && controller != null) {
            // Try to get player ID from controller
            String controllerPlayerId = controller.getCurrentPlayerId();
            if (controllerPlayerId != null) {
                currentPlayerId = controllerPlayerId;

            } else {
                // Fallback to default
                currentPlayerId = "player1";

            }
        }
    }

    public multiplayer.MultiplayerGameController getGameController() {
        return this.gameController;
    }

    private void initializeUI() {
        // Create root container
        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        // Create background rectangle - make it responsive to screen size
        Rectangle background = new Rectangle();
        background.setFill(Color.TRANSPARENT);
        background.setStroke(Color.CYAN);
        background.setStrokeWidth(2);

        // Bind to parent size to make it responsive
        background.widthProperty().bind(root.widthProperty());
        background.heightProperty().bind(root.heightProperty());

        // Create main container
        mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(20, 30, 60, 30)); // Further increased bottom padding

        // Create title
        Text title = new Text("Multiplayer Mode - Operator vs Operator");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setFill(Color.CYAN);

        // Create subtitle
        Text subtitle = new Text("Phase 3 - Network Simulation Game");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitle.setFill(Color.LIGHTGRAY);

        // Create game panels
        createGamePanels();

        // Create control buttons
        createControlButtons();

        // Add components to main container
        mainContainer.getChildren().addAll(
                title, subtitle,
                createGamePanels(),
                createControlButtons()
        );

        // Create a scroll pane to ensure all content is accessible
        ScrollPane scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Add everything to root
        root.getChildren().addAll(background, scrollPane);
    }

    private VBox createGamePanels() {
        VBox panelsContainer = new VBox(20);
        panelsContainer.setAlignment(Pos.CENTER);
        panelsContainer.setPadding(new Insets(15));
        panelsContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");

        // Panels title
        Text panelsTitle = new Text("Game Interface");
        panelsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        panelsTitle.setFill(Color.CYAN);

        // Create horizontal layout for main game area
        HBox gameAreaBox = new HBox(20);
        gameAreaBox.setAlignment(Pos.CENTER);

        // Left side - Player's network and controls
        VBox leftPanel = new VBox(15);
        leftPanel.setAlignment(Pos.TOP_CENTER);
        leftPanel.setPrefWidth(400);
        leftPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); -fx-background-radius: 8;");
        leftPanel.setPadding(new Insets(15));

        // Setup phase panel
        setupPhasePanel = createSetupPhasePanel();
        leftPanel.getChildren().add(setupPhasePanel);

        // Game phase panel
        gamePhasePanel = createGamePhasePanel();
        gamePhasePanel.setVisible(false);
        leftPanel.getChildren().add(gamePhasePanel);

        // Player actions panel
        playerActionsPanel = createPlayerActionsPanel();
        leftPanel.getChildren().add(playerActionsPanel);

        // Center - Network building canvas (setup phase) or game view (game phase)
        VBox centerPanel = new VBox(10);
        centerPanel.setAlignment(Pos.CENTER);
        centerPanel.setPrefWidth(600);
        centerPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 8;");
        centerPanel.setPadding(new Insets(15));

        // Network building canvas for setup phase
        createNetworkBuildingCanvas();
        centerPanel.getChildren().add(networkCanvas);

        // Opponent network visibility canvas (overlay)
        createOpponentNetworkCanvas();
        centerPanel.getChildren().add(opponentNetworkCanvas);

        // Add editing mode commands panel
        VBox editingCommandsPanel = createEditingCommandsPanel();
        centerPanel.getChildren().add(editingCommandsPanel);

        // Add temporal progress panel for setup phase
        temporalProgressPanel = createTemporalProgressPanel();
        centerPanel.getChildren().add(temporalProgressPanel);

        // Right side - Opponent's network and game status
        VBox rightPanel = new VBox(15);
        rightPanel.setAlignment(Pos.TOP_CENTER);
        rightPanel.setPrefWidth(400);
        rightPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); -fx-background-radius: 8;");
        rightPanel.setPadding(new Insets(15));

        // Opponent network panel
        opponentNetworkPanel = createOpponentNetworkPanel();
        rightPanel.getChildren().add(opponentNetworkPanel);

        // Game status panel
        gameStatusPanel = createGameStatusPanel();
        rightPanel.getChildren().add(gameStatusPanel);

        gameAreaBox.getChildren().addAll(leftPanel, centerPanel, rightPanel);

        panelsContainer.getChildren().addAll(panelsTitle, gameAreaBox);
        return panelsContainer;
    }

    private VBox createSetupPhasePanel() {
        VBox setupPanel = new VBox(15);
        setupPanel.setAlignment(Pos.CENTER);
        setupPanel.setStyle("-fx-background-color: rgba(0, 100, 0, 0.3); -fx-background-radius: 8;");
        setupPanel.setPadding(new Insets(15));

        // Setup phase title
        Text setupTitle = new Text("Setup Phase");
        setupTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        setupTitle.setFill(Color.LIME);

        // Setup timer - initialized with new format for 60 seconds total (30s + 30s penalty phases)
        setupTimerLabel = new Label("Time: 01:00");
        setupTimerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        setupTimerLabel.setTextFill(Color.WHITE);

        // Setup instructions
        setupInstructionsLabel = new Label("Design your network! You have 60 seconds total (30s + penalty phases).");
        setupInstructionsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        setupInstructionsLabel.setTextFill(Color.LIGHTGRAY);
        setupInstructionsLabel.setWrapText(true);

        // Ready button
        readyButton = new Button("Ready to Start");
        readyButton.setStyle(
                "-fx-background-color: lime;" +
                        "-fx-text-fill: black;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        readyButton.setPrefWidth(150);
        readyButton.setPrefHeight(40);
        readyButton.setOnAction(e -> markPlayerReady());

        setupPanel.getChildren().addAll(setupTitle, setupTimerLabel, setupInstructionsLabel, readyButton);
        return setupPanel;
    }

    private VBox createGamePhasePanel() {
        VBox gamePanel = new VBox(15);
        gamePanel.setAlignment(Pos.CENTER);
        gamePanel.setStyle("-fx-background-color: rgba(100, 0, 0, 0.3); -fx-background-radius: 8;");
        gamePanel.setPadding(new Insets(15));

        // Game phase title
        Text gameTitle = new Text("Game Phase");
        gameTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        gameTitle.setFill(Color.RED);

        // Game timer
        gameTimerLabel = new Label("Game Time: 0:00");
        gameTimerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        gameTimerLabel.setTextFill(Color.WHITE);

        // Player scores
        HBox scoresBox = new HBox(20);
        scoresBox.setAlignment(Pos.CENTER);

        player1ScoreLabel = new Label("Player 1: 0");
        player1ScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        player1ScoreLabel.setTextFill(Color.CYAN);

        player2ScoreLabel = new Label("Player 2: 0");
        player2ScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        player2ScoreLabel.setTextFill(Color.MAGENTA);

        scoresBox.getChildren().addAll(player1ScoreLabel, player2ScoreLabel);

        gamePanel.getChildren().addAll(gameTitle, gameTimerLabel, scoresBox);
        return gamePanel;
    }


    private VBox createPlayerActionsPanel() {
        VBox actionsPanel = new VBox(15);
        actionsPanel.setAlignment(Pos.CENTER);
        actionsPanel.setStyle("-fx-background-color: rgba(0, 0, 100, 0.3); -fx-background-radius: 8;");
        actionsPanel.setPadding(new Insets(15));

        // Actions title
        Text actionsTitle = new Text("Player Actions");
        actionsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        actionsTitle.setFill(Color.CYAN);

        // Controllable systems
        controllableSystemsBox = new VBox(10);
        controllableSystemsBox.setAlignment(Pos.CENTER);

        Label systemsTitle = new Label("Controllable Systems:");
        systemsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        systemsTitle.setTextFill(Color.WHITE);

        // Add sample controllable systems
        controllableSystemsBox.getChildren().add(systemsTitle);
        controllableSystemsBox.getChildren().add(createSystemButton("System 1", "system1"));
        controllableSystemsBox.getChildren().add(createSystemButton("System 2", "system2"));
        controllableSystemsBox.getChildren().add(createSystemButton("System 3", "system3"));

        // Ammunition display
        ammunitionBox = new VBox(10);
        ammunitionBox.setAlignment(Pos.CENTER);

        Label ammoTitle = new Label("Ammunition:");
        ammoTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        ammoTitle.setTextFill(Color.WHITE);

        ammunitionBox.getChildren().add(ammoTitle);

        // Create ammunition labels with references for real-time updates
        Label smallMessengerLabel = createAmmunitionLabel("Small Messenger", 10);
        Label protectedLabel = createAmmunitionLabel("Protected", 8);
        Label bulkSmallLabel = createAmmunitionLabel("Bulk Small", 5);
        Label trojanLabel = createAmmunitionLabel("Trojan", 3);
        Label bitPacketLabel = createAmmunitionLabel("Bit Packet", 15);

        // Store references for real-time updates
        ammunitionLabels.put("SMALL_MESSENGER", smallMessengerLabel);
        ammunitionLabels.put("PROTECTED", protectedLabel);
        ammunitionLabels.put("BULK_SMALL", bulkSmallLabel);
        ammunitionLabels.put("TROJAN", trojanLabel);
        ammunitionLabels.put("BIT_PACKET", bitPacketLabel);

        ammunitionBox.getChildren().addAll(smallMessengerLabel, protectedLabel, bulkSmallLabel, trojanLabel, bitPacketLabel);

        // Cooldown timers
        cooldownTimersBox = new VBox(10);
        cooldownTimersBox.setAlignment(Pos.CENTER);

        Label cooldownTitle = new Label("Cooldowns:");
        cooldownTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        cooldownTitle.setTextFill(Color.WHITE);

        cooldownTimersBox.getChildren().add(cooldownTitle);

        // Create cooldown labels with references for real-time updates
        Label system1CooldownLabel = createCooldownLabel("System 1", 0);
        Label system2CooldownLabel = createCooldownLabel("System 2", 0);
        Label system3CooldownLabel = createCooldownLabel("System 3", 0);

        // Store references for real-time updates
        cooldownLabels.put("system1", system1CooldownLabel);
        cooldownLabels.put("system2", system2CooldownLabel);
        cooldownLabels.put("system3", system3CooldownLabel);

        cooldownTimersBox.getChildren().addAll(system1CooldownLabel, system2CooldownLabel, system3CooldownLabel);

        actionsPanel.getChildren().addAll(actionsTitle, controllableSystemsBox, ammunitionBox, cooldownTimersBox);
        return actionsPanel;
    }

    private VBox createOpponentNetworkPanel() {
        VBox opponentPanel = new VBox(15);
        opponentPanel.setAlignment(Pos.CENTER);
        opponentPanel.setStyle("-fx-background-color: rgba(100, 100, 0, 0.3); -fx-background-radius: 8;");
        opponentPanel.setPadding(new Insets(15));

        // Opponent title
        Text opponentTitle = new Text("Opponent's Network");
        opponentTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        opponentTitle.setFill(Color.YELLOW);

        // Opponent network status
        opponentNetworkStatusLabel = new Label("Network: Hidden during setup");
        opponentNetworkStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        opponentNetworkStatusLabel.setTextFill(Color.LIGHTGRAY);
        opponentNetworkStatusLabel.setWrapText(true);

        // Opponent score
        opponentScoreLabel = new Label("Score: 0");
        opponentScoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        opponentScoreLabel.setTextFill(Color.YELLOW);

        // Opponent ammunition status
        opponentAmmunitionLabel = new Label("Ammunition: Unknown");
        opponentAmmunitionLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        opponentAmmunitionLabel.setTextFill(Color.LIGHTGRAY);

        // Network health bar
        opponentNetworkHealthBar = new ProgressBar(1.0);
        opponentNetworkHealthBar.setPrefWidth(200);
        opponentNetworkHealthBar.setStyle("-fx-accent: green;");

        Label healthLabel = new Label("Network Health:");
        healthLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        healthLabel.setTextFill(Color.WHITE);

        opponentPanel.getChildren().addAll(opponentTitle, opponentNetworkStatusLabel,
                opponentScoreLabel, opponentAmmunitionLabel, healthLabel, opponentNetworkHealthBar);
        return opponentPanel;
    }


    private VBox createGameStatusPanel() {
        VBox statusPanel = new VBox(15);
        statusPanel.setAlignment(Pos.CENTER);
        statusPanel.setStyle("-fx-background-color: rgba(0, 100, 100, 0.3); -fx-background-radius: 8;");
        statusPanel.setPadding(new Insets(15));

        // Status title
        Text statusTitle = new Text("Game Status");
        statusTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        statusTitle.setFill(Color.CYAN);

        // Game phase
        gamePhaseLabel = new Label("Phase: Setup");
        gamePhaseLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        gamePhaseLabel.setTextFill(Color.WHITE);

        // Penalty status
        penaltyStatusLabel = new Label("Penalties: None");
        penaltyStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        penaltyStatusLabel.setTextFill(Color.ORANGE);

        // Network status
        networkStatusLabel = new Label("Network: Connected");
        networkStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        networkStatusLabel.setTextFill(Color.GREEN);

        statusPanel.getChildren().addAll(statusTitle, gamePhaseLabel, penaltyStatusLabel, networkStatusLabel);
        return statusPanel;
    }


    private HBox createControlButtons() {
        HBox buttonsContainer = new HBox(20);
        buttonsContainer.setAlignment(Pos.CENTER);
        buttonsContainer.setPadding(new Insets(20, 15, 20, 15)); // Increased vertical padding

        // Pause button
        pauseButton = createControlButton("Pause Game");
        pauseButton.setOnAction(e -> pauseGame());

        // Surrender button
        surrenderButton = createControlButton("Surrender");
        surrenderButton.setOnAction(e -> surrenderGame());

        // Back to menu button
        backToMenuButton = createControlButton("Back to Main Menu");
        backToMenuButton.setOnAction(e -> mainApp.showMainMenu());

        buttonsContainer.getChildren().addAll(pauseButton, surrenderButton, backToMenuButton);
        return buttonsContainer;
    }

    private Button createControlButton(String text) {
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


    private Button createSystemButton(String name, String systemId) {
        Button button = new Button(name);
        button.setPrefWidth(120);
        button.setPrefHeight(30);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        button.setStyle(
                "-fx-background-color: rgba(0, 255, 255, 0.3);" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );

        button.setOnAction(e -> selectSystem(systemId));
        return button;
    }


    private void createOpponentNetworkCanvas() {
        opponentNetworkCanvas = new Canvas(600, 400);
        opponentNetworkGC = opponentNetworkCanvas.getGraphicsContext2D();

        // Initially hidden - will be shown after setup phase
        opponentNetworkCanvas.setVisible(false);
        opponentNetworkCanvas.setOpacity(0.3); // Faint display as per requirements

        // Position over the main network canvas
        opponentNetworkCanvas.setLayoutX(0);
        opponentNetworkCanvas.setLayoutY(0);
    }


    public void showOpponentNetwork(model.GameLevel opponentLevel) {
        if (opponentLevel == null) {
            hideOpponentNetwork();
            return;
        }

        opponentNetworkCanvas.setVisible(true);

        // Draw opponent's network faintly
        drawOpponentNetwork(opponentLevel);

        // Update opponent network status
        if (opponentNetworkStatusLabel != null) {
            opponentNetworkStatusLabel.setText("Opponent Network: Visible");
            opponentNetworkStatusLabel.setTextFill(Color.LIGHTGREEN);
        }


    }

    public void hideOpponentNetwork() {
        if (opponentNetworkCanvas != null) {
            opponentNetworkCanvas.setVisible(false);
        }

        // Update opponent network status
        if (opponentNetworkStatusLabel != null) {
            opponentNetworkStatusLabel.setText("Opponent Network: Hidden");
            opponentNetworkStatusLabel.setTextFill(Color.GRAY);
        }
    }


    public void updateOpponentNetworkVisibility(boolean isSetupPhase, boolean networkVisibilityEnabled,
                                                model.GameLevel opponentNetwork) {
        if (isSetupPhase) {
            // Hide opponent network during setup phase
            hideOpponentNetwork();
        } else if (networkVisibilityEnabled && opponentNetwork != null) {
            // Show opponent network after setup phase
            showOpponentNetwork(opponentNetwork);
        } else {
            // Hide if visibility is disabled or no network data
            hideOpponentNetwork();
        }
    }

    private void drawOpponentNetwork(model.GameLevel opponentLevel) {
        if (opponentLevel == null) return;

        // Clear canvas
        opponentNetworkGC.setFill(Color.TRANSPARENT);
        opponentNetworkGC.fillRect(0, 0, opponentNetworkCanvas.getWidth(), opponentNetworkCanvas.getHeight());

        // Set faint drawing style for opponent network
        opponentNetworkGC.setGlobalAlpha(0.3); // Very faint display as per requirements

        // Draw opponent's wire connections first (behind systems)
        for (model.WireConnection connection : opponentLevel.getWireConnections()) {
            drawOpponentWireConnection(connection);
        }

        // Draw opponent's systems
        for (model.System system : opponentLevel.getSystems()) {
            drawOpponentSystem(system);
        }

        // Draw network information overlay
        drawNetworkInfoOverlay(opponentLevel);

        // Reset alpha
        opponentNetworkGC.setGlobalAlpha(1.0);
    }

    /**
     * Draws network information overlay.
     */
    private void drawNetworkInfoOverlay(model.GameLevel opponentLevel) {
        // Draw network statistics
        opponentNetworkGC.setFill(Color.LIGHTGRAY);
        opponentNetworkGC.setFont(javafx.scene.text.Font.font("Arial", 10));

        String systemCount = "Systems: " + opponentLevel.getSystems().size();
        String wireCount = "Wires: " + opponentLevel.getWireConnections().size();

        opponentNetworkGC.fillText(systemCount, 10, 20);
        opponentNetworkGC.fillText(wireCount, 10, 35);
    }

    /**
     * Draws an opponent system faintly with enhanced visual distinction.
     */
    private void drawOpponentSystem(model.System system) {
        double x = system.getPosition().getX();
        double y = system.getPosition().getY();
        double radius = 12; // Slightly smaller than player systems

        // Draw system as faint circle with dashed border
        opponentNetworkGC.setFill(Color.LIGHTGRAY);
        opponentNetworkGC.fillOval(x - radius, y - radius, radius * 2, radius * 2);

        // Draw dashed border to distinguish from player systems
        opponentNetworkGC.setStroke(Color.GRAY);
        opponentNetworkGC.setLineWidth(1.5);
        opponentNetworkGC.strokeOval(x - radius, y - radius, radius * 2, radius * 2);

        // Draw system type indicator
        opponentNetworkGC.setFill(Color.DARKGRAY);
        opponentNetworkGC.setFont(javafx.scene.text.Font.font("Arial", 7));
        String systemType = system.getSystemType().toString();
        if (systemType.length() > 8) {
            systemType = systemType.substring(0, 8) + "...";
        }
        opponentNetworkGC.fillText(systemType, x - 15, y + 20);

        // Draw small indicator dot for system status
        opponentNetworkGC.setFill(Color.ORANGE);
        opponentNetworkGC.fillOval(x + radius - 3, y - radius + 3, 3, 3);
    }

    /**
     * Draws an opponent wire connection faintly with enhanced visual distinction.
     */
    private void drawOpponentWireConnection(model.WireConnection connection) {
        if (connection.getSourcePort() == null || connection.getDestinationPort() == null) return;

        double startX = connection.getSourcePort().getPosition().getX();
        double startY = connection.getSourcePort().getPosition().getY();
        double endX = connection.getDestinationPort().getPosition().getX();
        double endY = connection.getDestinationPort().getPosition().getY();

        // Draw wire as faint dashed line to distinguish from player wires
        opponentNetworkGC.setStroke(Color.LIGHTGRAY);
        opponentNetworkGC.setLineWidth(1.5);

        // Create dashed line effect
        double dashLength = 5.0;
        double gapLength = 3.0;
        double totalLength = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        double dashCount = totalLength / (dashLength + gapLength);

        double dx = (endX - startX) / dashCount;
        double dy = (endY - startY) / dashCount;

        for (int i = 0; i < (int)dashCount; i++) {
            double dashStartX = startX + i * dx;
            double dashStartY = startY + i * dy;
            double dashEndX = startX + (i + dashLength / (dashLength + gapLength)) * dx;
            double dashEndY = startY + (i + dashLength / (dashLength + gapLength)) * dy;

            opponentNetworkGC.strokeLine(dashStartX, dashStartY, dashEndX, dashEndY);
        }

        // Draw direction arrow at the end
        drawDirectionArrow(startX, startY, endX, endY);
    }

    private void drawDirectionArrow(double startX, double startY, double endX, double endY) {
        double arrowLength = 8.0;
        double arrowAngle = Math.atan2(endY - startY, endX - startX);

        // Calculate arrow points
        double arrowX1 = endX - arrowLength * Math.cos(arrowAngle - Math.PI / 6);
        double arrowY1 = endY - arrowLength * Math.sin(arrowAngle - Math.PI / 6);
        double arrowX2 = endX - arrowLength * Math.cos(arrowAngle + Math.PI / 6);
        double arrowY2 = endY - arrowLength * Math.sin(arrowAngle + Math.PI / 6);

        // Draw arrow
        opponentNetworkGC.setStroke(Color.LIGHTGRAY);
        opponentNetworkGC.setLineWidth(1.0);
        opponentNetworkGC.strokeLine(endX, endY, arrowX1, arrowY1);
        opponentNetworkGC.strokeLine(endX, endY, arrowX2, arrowY2);
    }

    private VBox createTemporalProgressPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: rgba(0, 100, 100, 0.3); -fx-background-radius: 8;");
        panel.setPadding(new Insets(10));

        // Temporal progress title
        Text title = new Text("Temporal Analysis");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        title.setFill(Color.CYAN);

        // Temporal progress button
        temporalProgressButton = new Button("Analyze Network State");
        temporalProgressButton.setStyle(
                "-fx-background-color: rgba(0, 255, 255, 0.3);" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );
        temporalProgressButton.setPrefWidth(180);
        temporalProgressButton.setPrefHeight(30);
        temporalProgressButton.setOnAction(e -> toggleTemporalProgress());

        // Status label
        Label statusLabel = new Label("Status: Available during setup");
        statusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        statusLabel.setTextFill(Color.LIGHTGRAY);

        panel.getChildren().addAll(title, temporalProgressButton, statusLabel);
        return panel;
    }

    private void toggleTemporalProgress() {
        if (temporalProgressEnabled) {
            // Disable temporal progress
            temporalProgressEnabled = false;
            temporalProgressButton.setText("Analyze Network State");
            temporalProgressButton.setStyle(
                    "-fx-background-color: rgba(0, 255, 255, 0.3);" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;"
            );

            // Hide temporal analysis overlay
            hideTemporalAnalysis();
        } else {
            // Enable temporal progress
            temporalProgressEnabled = true;
            temporalProgressButton.setText("Exit Analysis");
            temporalProgressButton.setStyle(
                    "-fx-background-color: rgba(255, 165, 0, 0.3);" +
                            "-fx-border-color: orange;" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;"
            );

            // Show temporal analysis overlay
            showTemporalAnalysis();
        }
    }

    private void showTemporalAnalysis() {
        if (gameController == null) {

            return;
        }

        // Create temporal analysis overlay
        createTemporalAnalysisOverlay();

        // Start temporal analysis simulation
        startTemporalAnalysisSimulation();


    }

    private void hideTemporalAnalysis() {
        if (networkCanvas != null) {
            networkCanvas.setOpacity(1.0);
        }

        // Stop temporal analysis simulation
        stopTemporalAnalysisSimulation();

        // Remove temporal analysis overlay
        removeTemporalAnalysisOverlay();


    }


    private void createTemporalAnalysisOverlay() {
        // Create temporal analysis control panel
        VBox temporalControls = new VBox(10);
        temporalControls.setAlignment(Pos.CENTER);
        temporalControls.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 8;");
        temporalControls.setPadding(new Insets(15));
        temporalControls.setLayoutX(50);
        temporalControls.setLayoutY(50);

        // Title
        Text title = new Text("Temporal Analysis Mode");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setFill(Color.CYAN);

        // Time display
        Label timeLabel = new Label("Time: 0.0s");
        timeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        timeLabel.setTextFill(Color.WHITE);
        timeLabel.setId("temporalTimeLabel");

        // Control buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button playButton = new Button("▶");
        playButton.setStyle("-fx-background-color: green; -fx-text-fill: white; -fx-font-weight: bold;");
        playButton.setOnAction(e -> playTemporalAnalysis());

        Button pauseButton = new Button("⏸");
        pauseButton.setStyle("-fx-background-color: orange; -fx-text-fill: white; -fx-font-weight: bold;");
        pauseButton.setOnAction(e -> pauseTemporalAnalysis());

        Button rewindButton = new Button("⏪");
        rewindButton.setStyle("-fx-background-color: blue; -fx-text-fill: white; -fx-font-weight: bold;");
        rewindButton.setOnAction(e -> rewindTemporalAnalysis());

        Button fastForwardButton = new Button("⏩");
        fastForwardButton.setStyle("-fx-background-color: purple; -fx-text-fill: white; -fx-font-weight: bold;");
        fastForwardButton.setOnAction(e -> fastForwardTemporalAnalysis());

        buttonBox.getChildren().addAll(rewindButton, playButton, pauseButton, fastForwardButton);

        // Speed control
        Label speedLabel = new Label("Speed: 1x");
        speedLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        speedLabel.setTextFill(Color.LIGHTGRAY);
        speedLabel.setId("temporalSpeedLabel");

        // Packet count display
        Label packetLabel = new Label("Active Packets: 0");
        packetLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        packetLabel.setTextFill(Color.LIGHTGRAY);
        packetLabel.setId("temporalPacketLabel");

        // Opponent network display
        Label opponentLabel = new Label("Opponent Network: Not available");
        opponentLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        opponentLabel.setTextFill(Color.LIGHTGRAY);
        opponentLabel.setId("temporalOpponentLabel");

        temporalControls.getChildren().addAll(title, timeLabel, buttonBox, speedLabel, packetLabel, opponentLabel);

        // Store reference for later removal
        temporalAnalysisOverlay = temporalControls;

        // Add to root
        root.getChildren().add(temporalControls);
    }

    /**
     * Removes the temporal analysis overlay.
     */
    private void removeTemporalAnalysisOverlay() {
        if (temporalAnalysisOverlay != null) {
            root.getChildren().remove(temporalAnalysisOverlay);
            temporalAnalysisOverlay = null;
        }
    }

    private void startTemporalAnalysisSimulation() {
        if (gameController == null) return;

        // Start the analysis timer
        startTemporalAnalysisTimer();


    }

    private void stopTemporalAnalysisSimulation() {
        if (temporalAnalysisTimer != null) {
            temporalAnalysisTimer.stop();
            temporalAnalysisTimer = null;
        }


    }


    private void startTemporalAnalysisTimer() {
        temporalAnalysisTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(50), e -> {
                    updateTemporalAnalysis();
                })
        );
        temporalAnalysisTimer.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        temporalAnalysisTimer.play();
    }

    private void updateTemporalAnalysis() {
        if (gameController == null) return;

        // Update temporal progress (use game time as temporal progress)
        double currentTime = java.lang.System.currentTimeMillis() / 1000.0; // Simple time for now
        int activePackets = gameController.getAllActivePackets().size();

        // Get opponent network data for analysis
        String currentPlayerId = getCurrentPlayerId();
        model.GameLevel opponentNetwork = null;
        if (currentPlayerId != null) {
            opponentNetwork = gameController.getOpponentNetwork(currentPlayerId);
        }

        // Update UI labels with opponent network info
        updateTemporalAnalysisLabels(currentTime, activePackets, opponentNetwork);

        // Update packet visualization
        updatePacketVisualization();

        // Update opponent network packet states
        updateOpponentNetworkPacketStates();
    }


    private void updateTemporalAnalysisLabels(double currentTime, int activePackets, model.GameLevel opponentNetwork) {
        if (temporalAnalysisOverlay == null) return;

        // Find and update time label
        Label timeLabel = (Label) temporalAnalysisOverlay.lookup("#temporalTimeLabel");
        if (timeLabel != null) {
            timeLabel.setText(String.format("Time: %.1fs", currentTime));
        }

        // Find and update packet label
        Label packetLabel = (Label) temporalAnalysisOverlay.lookup("#temporalPacketLabel");
        if (packetLabel != null) {
            packetLabel.setText("Active Packets: " + activePackets);
        }

        // Find and update opponent network label
        Label opponentLabel = (Label) temporalAnalysisOverlay.lookup("#temporalOpponentLabel");
        if (opponentLabel != null) {
            if (opponentNetwork != null) {
                opponentLabel.setText("Opponent Network: " + opponentNetwork.getSystems().size() + " systems, " +
                        opponentNetwork.getWireConnections().size() + " connections");
                opponentLabel.setTextFill(Color.LIGHTGREEN);
            } else {
                opponentLabel.setText("Opponent Network: Not available");
                opponentLabel.setTextFill(Color.LIGHTGRAY);
            }
        }
    }

    private void updatePacketVisualization() {
        if (gameController == null || networkCanvas == null) return;

        // Get all active packets from multiplayer controller
        List<Packet> activePackets = gameController.getAllActivePackets();

        // Clear and redraw packets
        GraphicsContext gc = networkCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, networkCanvas.getWidth(), networkCanvas.getHeight());

        // Redraw the network
        redrawNetwork();

        // Draw packets
        for (Packet packet : activePackets) {
            drawPacketInAnalysis(packet);
        }
    }


    private void updateOpponentNetworkPacketStates() {
        if (opponentNetworkCanvas == null || gameController == null) return;

        GraphicsContext gc = opponentNetworkCanvas.getGraphicsContext2D();

        // Clear the canvas first
        gc.clearRect(0, 0, opponentNetworkCanvas.getWidth(), opponentNetworkCanvas.getHeight());

        // Get real opponent packets from the multiplayer controller
        String currentPlayerId = getCurrentPlayerId();
        List<Packet> opponentPackets = gameController.getOpponentPackets(currentPlayerId);

        if (opponentPackets.isEmpty()) {
            // Show indicator when no opponent packets are active
            gc.setFill(Color.GRAY);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            gc.fillText("No opponent packets", 20, 20);
        } else {
            // Draw real opponent packets
            for (Packet packet : opponentPackets) {
                drawOpponentPacketInAnalysis(packet, gc);
            }

            // Show packet count
            gc.setFill(Color.CYAN);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            gc.fillText("Opponent Packets: " + opponentPackets.size(), 10, opponentNetworkCanvas.getHeight() - 10);
        }
    }

    private void drawOpponentPacketInAnalysis(Packet packet, GraphicsContext gc) {
        if (packet == null) return;

        Point2D position = packet.getCurrentPosition();
        if (position == null) return;

        // Draw packet as a colored circle with opponent-specific styling
        Color packetColor = getOpponentPacketColor(packet.getPacketType());
        gc.setFill(packetColor);

        // Draw packet based on size
        double size = Math.max(4, packet.getSize() * 2);
        gc.fillOval(position.getX() - size/2, position.getY() - size/2, size, size);

        // Draw border to distinguish from player packets
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(position.getX() - size/2, position.getY() - size/2, size, size);

        // Draw packet type indicator
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 8));
        String typeName = packet.getPacketType().toString();
        if (typeName.length() > 8) {
            typeName = typeName.substring(0, 8);
        }
        gc.fillText(typeName, position.getX() - 15, position.getY() + size + 10);

        // Draw movement trail if packet is moving
        Vec2D velocity = packet.getMovementVector();
        if (velocity != null && velocity.magnitude() > 5) {
            gc.setStroke(packetColor.deriveColor(0, 1, 1.2, 0.5));
            gc.setLineWidth(2);
            double trailLength = 15;
            double endX = position.getX() - velocity.getX() * trailLength / velocity.magnitude();
            double endY = position.getY() - velocity.getY() * trailLength / velocity.magnitude();
            gc.strokeLine(position.getX(), position.getY(), endX, endY);
        }
    }


    private Color getOpponentPacketColor(PacketType packetType) {
        // Use different color scheme for opponent packets to distinguish them
        switch (packetType) {
            case SMALL_MESSENGER:
                return Color.LIGHTBLUE;
            case PROTECTED:
                return Color.LIGHTGREEN;
            case TRIANGLE_MESSENGER:
                return Color.LIGHTYELLOW;
            case CONFIDENTIAL:
                return Color.LIGHTCORAL;
            case SQUARE_MESSENGER:
                return Color.LIGHTSTEELBLUE;
            case BULK_SMALL:
                return Color.LIGHTSALMON;
            case BULK_LARGE:
                return Color.LIGHTSEAGREEN;
            case TROJAN:
                return Color.DARKORANGE;
            case BIT_PACKET:
                return Color.LIGHTGRAY;
            default:
                return Color.WHITE;
        }
    }

    private void drawPacketInAnalysis(Packet packet) {
        if (packet == null || networkCanvas == null) return;

        GraphicsContext gc = networkCanvas.getGraphicsContext2D();
        Point2D position = packet.getCurrentPosition();

        if (position != null) {
            // Draw packet as a colored circle
            gc.setFill(getPacketColor(packet.getPacketType()));
            gc.fillOval(position.getX() - 4, position.getY() - 4, 8, 8);

            // Draw packet trail
            gc.setStroke(getPacketColor(packet.getPacketType()));
            gc.setLineWidth(2);
            gc.strokeOval(position.getX() - 6, position.getY() - 6, 12, 12);
        }
    }


    private void drawMultiplayerPackets() {
        if (mainApp == null || mainApp.getGameController() == null) return;

        for (Packet packet : mainApp.getGameController().getGameState().getActivePackets()) {
            if (packet.isActive()) {
                drawMultiplayerPacket(packet);
            }
        }
    }

    private void drawMultiplayerPacket(Packet packet) {
        if (packet == null || networkCanvas == null) return;

        GraphicsContext gc = networkCanvas.getGraphicsContext2D();
        Point2D pos = packet.getCurrentPosition();
        Vec2D velocity = packet.getMovementVector();

        if (pos == null) return;

        // Get player-specific color
        Color packetColor = getPlayerSpecificPacketColor(packet);
        Color borderColor = Color.BLACK;
        Color trailColor = packetColor.deriveColor(0, 1, 1.2, 0.7); // Lighter version for trail

        // Calculate speed for visual effects
        double speed = velocity != null ? velocity.magnitude() : 0;
        double minSize = Math.max(2, packet.getSize());

        // Size varies based on speed (faster = slightly larger for visibility)
        double sizeVariation = Math.min(2.0, speed / 50.0);
        double displaySize = minSize + sizeVariation;

        // Draw motion trail behind packet
        if (velocity != null && speed > 5) {
            gc.setStroke(trailColor);
            gc.setLineWidth(2);
            gc.strokeOval(pos.getX() - displaySize - 2, pos.getY() - displaySize - 2,
                    displaySize * 2 + 4, displaySize * 2 + 4);
        }

        // Draw packet based on size
        if (packet.getSize() <= 3) {
            // Draw small packets as squares
            gc.setFill(packetColor);
            gc.fillRect(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);

            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(trailColor);
                gc.setLineWidth(2);
                gc.strokeRect(pos.getX() - displaySize - 1, pos.getY() - displaySize - 1,
                        displaySize * 2 + 2, displaySize * 2 + 2);
            }

            gc.setStroke(borderColor);
            gc.setLineWidth(1);
            gc.strokeRect(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
        } else {
            // Draw larger packets as circles
            gc.setFill(packetColor);
            gc.fillOval(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);

            // Add glow effect for moving packets
            if (speed > 10) {
                gc.setStroke(trailColor);
                gc.setLineWidth(2);
                gc.strokeOval(pos.getX() - displaySize - 1, pos.getY() - displaySize - 1,
                        displaySize * 2 + 2, displaySize * 2 + 2);
            }

            gc.setStroke(borderColor);
            gc.setLineWidth(1);
            gc.strokeOval(pos.getX() - displaySize, pos.getY() - displaySize, displaySize * 2, displaySize * 2);
        }

        // Add packet size indicator for larger packets
        if (packet.getSize() > 3) {
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 8));
            gc.fillText(String.valueOf(packet.getSize()), pos.getX() - 3, pos.getY() + 3);
        }

        // Add player indicator
        String packetOwner = determinePacketOwner(packet);
        if (!"unknown".equals(packetOwner)) {
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 6));
            String playerLabel = "player1".equals(packetOwner) ? "P1" : "P2";
            gc.fillText(playerLabel, pos.getX() - 6, pos.getY() - displaySize - 2);
        }
    }

    private Color getPacketColor(PacketType packetType) {
        switch (packetType) {
            case SMALL_MESSENGER:
                return Color.LIME;
            case PROTECTED:
                return Color.CYAN;
            case TRIANGLE_MESSENGER:
                return Color.ORANGE;
            case CONFIDENTIAL:
                return Color.RED;
            case SQUARE_MESSENGER:
                return Color.BLUE;
            case BULK_SMALL:
                return Color.PURPLE;
            case BULK_LARGE:
                return Color.PINK;
            case TROJAN:
                return Color.DARKRED;
            case BIT_PACKET:
                return Color.GRAY;
            default:
                return Color.WHITE;
        }
    }

    private Color getPlayerSpecificPacketColor(Packet packet) {
        if (packet == null || packet.getPacketType() == null) {
            return Color.WHITE;
        }

        // Determine packet owner using the same logic as the game controller
        String packetOwner = determinePacketOwner(packet);

        // Player 1 (type-one) packets: Blue color scheme
        if ("player1".equals(packetOwner)) {
            switch (packet.getPacketType()) {
                case SMALL_MESSENGER:
                    return Color.CYAN;
                case PROTECTED:
                    return Color.LIGHTBLUE;
                case TRIANGLE_MESSENGER:
                    return Color.DEEPSKYBLUE;
                case CONFIDENTIAL:
                    return Color.DARKBLUE;
                default:
                    return Color.BLUE;
            }
        }
        // Player 2 (type-two) packets: Red color scheme
        else if ("player2".equals(packetOwner)) {
            switch (packet.getPacketType()) {
                case BULK_SMALL:
                    return Color.ORANGE;
                case TROJAN:
                    return Color.DARKRED;
                case SQUARE_MESSENGER:
                    return Color.RED;
                case BULK_LARGE:
                    return Color.CRIMSON;
                case CONFIDENTIAL:
                    return Color.MAROON;
                default:
                    return Color.RED;
            }
        }

        // Default color for unknown ownership
        return Color.WHITE;
    }

    private String determinePacketOwner(Packet packet) {
        if (packet == null || packet.getPacketType() == null) {
            return "unknown";
        }

        PacketType packetType = packet.getPacketType();

        // Type-one packets belong to player 1
        if (packetType == PacketType.SMALL_MESSENGER ||
                packetType == PacketType.PROTECTED ||
                packetType == PacketType.TRIANGLE_MESSENGER)
                {
            return "player1";
        }

        // Type-two packets belong to player 2
        if (packetType == PacketType.BULK_SMALL ||
                packetType == PacketType.TROJAN ||
                packetType == PacketType.SQUARE_MESSENGER ||
                packetType == PacketType.BULK_LARGE) {
            return "player2";
        }

        // Confidential packets can belong to either player (released from reference systems)
        if (packetType == PacketType.CONFIDENTIAL) {
            // For now, assign based on which player's network they're in
            // This would need to be enhanced based on actual network position
            return "player1"; // Default assignment
        }

        return "unknown";
    }

    private boolean canPacketUseConnection(Packet packet, WireConnection connection) {
        if (packet == null || connection == null) {
            return false;
        }

        String packetOwner = determinePacketOwner(packet);
        String connectionOwner = determineConnectionOwner(connection);

        // Packets can only use connections owned by the same player
        return packetOwner.equals(connectionOwner);
    }

    private String determineConnectionOwner(WireConnection connection) {
        if (connection == null) {
            return "unknown";
        }

        // For now, we'll use a simple heuristic:
        // Connections in the left half of the screen belong to player 1
        // Connections in the right half belong to player 2
        // This would need to be enhanced with actual ownership tracking

        Point2D startPos = connection.getSourcePort() != null ? connection.getSourcePort().getPosition() : null;
        if (startPos != null) {
            if (startPos.getX() < networkCanvas.getWidth() / 2) {
                return "player1";
            } else {
                return "player2";
            }
        }

        return "unknown";
    }

    private boolean validatePacketConnectionAccess(Packet packet, WireConnection connection) {
        if (!canPacketUseConnection(packet, connection)) {
            // Packet cannot use this connection - it should be blocked or redirected

            return false;
        }
        return true;
    }

    private String getCurrentPlayerIdForWireOwnership() {
        // For now, we'll use a simple heuristic based on the current view state
        // In a full implementation, this would be determined by the actual player context

        if (gameController != null) {
            // Simple heuristic: if we're in setup phase, assume player 1
            // This would need to be enhanced with actual player context
            if (gameController.isSetupPhase()) {
                return "player1"; // Player 1 is building their network
            } else {
                // During game phase, we need to determine which player's turn it is
                // For now, return a default
                return "player1";
            }
        }

        return null; // Single player mode
    }


    private void playTemporalAnalysis() {
        if (temporalAnalysisTimer != null) {
            temporalAnalysisTimer.play();
        }

    }


    private void pauseTemporalAnalysis() {
        if (temporalAnalysisTimer != null) {
            temporalAnalysisTimer.pause();
        }

    }


    private void rewindTemporalAnalysis() {
        // For now, temporal analysis shows real-time data
        // In a full implementation, this would rewind packet positions

    }

    private void fastForwardTemporalAnalysis() {
        // For now, temporal analysis shows real-time data
        // In a full implementation, this would fast forward packet positions

    }

    private void redrawNetwork() {
        if (networkCanvas == null || mainApp == null || mainApp.getGameController() == null) return;

        GraphicsContext gc = networkCanvas.getGraphicsContext2D();

        // Clear canvas
        gc.clearRect(0, 0, networkCanvas.getWidth(), networkCanvas.getHeight());

        // Draw systems and connections
        // This is a simplified version - in a full implementation, you'd draw the actual network
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2);

        // Draw a simple placeholder network
        gc.strokeRect(50, 50, 500, 300);
        gc.setFill(Color.CYAN);
        gc.fillOval(100, 100, 20, 20);
        gc.fillOval(200, 150, 20, 20);
        gc.fillOval(300, 200, 20, 20);

        // Draw connections
        gc.strokeLine(110, 110, 210, 160);
        gc.strokeLine(210, 160, 310, 210);
    }

    public void disableTemporalProgress() {
        temporalProgressEnabled = false;
        temporalProgressButton.setDisable(true);
        temporalProgressButton.setText("Analysis Disabled");
        temporalProgressButton.setStyle(
                "-fx-background-color: rgba(128, 128, 128, 0.3);" +
                        "-fx-border-color: gray;" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: lightgray;" +
                        "-fx-cursor: default;"
        );

        hideTemporalAnalysis();
    }


    private Label createAmmunitionLabel(String type, int count) {
        Label label = new Label(type + ": " + count);
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        label.setTextFill(Color.WHITE);
        return label;
    }


    private Label createCooldownLabel(String system, double remaining) {
        Label label = new Label(system + ": " + String.format("%.1fs", remaining));
        label.setFont(Font.font("Arial", FontWeight.NORMAL, 10));
        label.setTextFill(Color.ORANGE);
        return label;
    }

    public void updateCooldowns(double deltaTime) {
        // Update system cooldowns
        for (String systemId : systemCooldowns.keySet()) {
            double currentCooldown = systemCooldowns.get(systemId);
            if (currentCooldown > 0) {
                double newCooldown = Math.max(0, currentCooldown - deltaTime);
                systemCooldowns.put(systemId, newCooldown);

                // Update UI if cooldown finished
                if (newCooldown == 0 && currentCooldown > 0) {
                    onCooldownFinished(systemId, "system");
                }
            }
        }

        // Update packet cooldowns
        for (String packetType : packetCooldowns.keySet()) {
            double currentCooldown = packetCooldowns.get(packetType);
            if (currentCooldown > 0) {
                double newCooldown = Math.max(0, currentCooldown - deltaTime);
                packetCooldowns.put(packetType, newCooldown);

                // Update ammunition button state
                Button button = ammunitionButtons.get(packetType);
                if (button != null) {
                    // Get current count (this would come from game controller)
                    int count = getAmmunitionCount(packetType);
                    updateAmmunitionButtonState(button, packetType, count, newCooldown);
                }

                // Update UI if cooldown finished
                if (newCooldown == 0 && currentCooldown > 0) {
                    onCooldownFinished(packetType, "packet");
                }
            }
        }
    }


    public void setSystemCooldown(String systemId, double cooldownTime) {
        systemCooldowns.put(systemId, cooldownTime);

    }


    public void setPacketCooldown(String packetType, double cooldownTime) {
        packetCooldowns.put(packetType, cooldownTime);

    }


    private int getAmmunitionCount(String packetType) {
        // This would integrate with the game controller to get actual counts
        // For now, return mock values
        switch (packetType) {
            case "SMALL_MESSENGER": return 10;
            case "PROTECTED": return 8;
            case "BULK_SMALL": return 5;
            case "TROJAN": return 3;
            case "BIT_PACKET": return 15;
            default: return 0;
        }
    }


    private void onCooldownFinished(String itemId, String type) {


        // Visual feedback - shake effect or notification
        if (type.equals("packet")) {
            Button button = ammunitionButtons.get(itemId);
            if (button != null) {
                // Add shake effect
                addShakeEffect(button);
            }
        }
    }


    private void addShakeEffect(Button button) {
        // Simple shake effect using timeline
        javafx.animation.Timeline shakeTimeline = new javafx.animation.Timeline();

        for (int i = 0; i < 5; i++) {
            double offset = (i % 2 == 0) ? 3 : -3;
            javafx.animation.KeyFrame keyFrame = new javafx.animation.KeyFrame(
                    javafx.util.Duration.millis(i * 50),
                    e -> button.setLayoutX(button.getLayoutX() + offset)
            );
            shakeTimeline.getKeyFrames().add(keyFrame);
        }

        // Reset position
        javafx.animation.KeyFrame resetFrame = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(250),
                e -> button.setLayoutX(button.getLayoutX())
        );
        shakeTimeline.getKeyFrames().add(resetFrame);

        shakeTimeline.play();
    }

    private void markPlayerReady() {
        // UI Changes (irreversible)
        readyButton.setDisable(true);
        readyButton.setText("Ready!");
        readyButton.setStyle(
                "-fx-background-color: green;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;"
        );

        // Update game status
        gamePhaseLabel.setText("Phase: Waiting for opponent");
        setupInstructionsLabel.setText("Waiting for opponent to be ready...");

        // Notify controller and server (irreversible action)
        if (gameController != null) {
            String playerId = getCurrentPlayerId();
            if (playerId != null) {
                gameController.setPlayerReady(playerId);
                gameController.sendPlayerReady();
            } else {

            }
        } else {

        }
    }


    private void selectSystem(String systemId) {
        // Show system selection feedback


        // Store selected system
        selectedSystemId = systemId;

        // Create and show ammunition panel
        showAmmunitionPanel(systemId);
    }

    private void showAmmunitionPanel(String systemId) {
        // Remove existing panel if visible
        if (isAmmunitionPanelVisible && ammunitionPanel != null) {
            root.getChildren().remove(ammunitionPanel);
        }

        // Create ammunition panel
        ammunitionPanel = new VBox(10);
        ammunitionPanel.setAlignment(Pos.CENTER);
        ammunitionPanel.setPadding(new Insets(15));
        ammunitionPanel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.6);" +
                        "-fx-border-color: cyan;" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;"
        );

        // Position the panel (top-right corner to avoid blocking opponent systems)
        ammunitionPanel.setLayoutX(1000);
        ammunitionPanel.setLayoutY(100);

        // Title
        Text title = new Text("System " + systemId.substring(systemId.length() - 1) + " - Select Ammunition");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setFill(Color.CYAN);

        // Ammunition buttons
        VBox ammoButtons = new VBox(8);
        ammoButtons.setAlignment(Pos.CENTER);

        // Create ammunition buttons based on available types
        Button smallMessengerBtn = createAmmunitionButton("Small Messenger", "SMALL_MESSENGER", 10);
        Button protectedBtn = createAmmunitionButton("Protected", "PROTECTED", 8);
        Button bulkSmallBtn = createAmmunitionButton("Bulk Small", "BULK_SMALL", 5);
        Button trojanBtn = createAmmunitionButton("Trojan", "TROJAN", 3);
        Button bitPacketBtn = createAmmunitionButton("Bit Packet", "BIT_PACKET", 15);

        ammoButtons.getChildren().addAll(
                smallMessengerBtn, protectedBtn, bulkSmallBtn, trojanBtn, bitPacketBtn
        );

        // Targeting controls
        VBox targetingControls = new VBox(5);
        targetingControls.setAlignment(Pos.CENTER);

        Label targetingLabel = new Label("Targeting:");
        targetingLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        targetingLabel.setTextFill(Color.YELLOW);

        Button targetModeBtn = new Button("Enter Target Mode");
        targetModeBtn.setStyle(
                "-fx-background-color: rgba(255, 255, 0, 0.3);" +
                        "-fx-border-color: yellow;" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );
        targetModeBtn.setPrefWidth(150);
        targetModeBtn.setPrefHeight(30);
        targetModeBtn.setOnAction(e -> enterTargetMode());

        Button clearTargetBtn = new Button("Clear Target");
        clearTargetBtn.setStyle(
                "-fx-background-color: rgba(255, 0, 0, 0.3);" +
                        "-fx-border-color: red;" +
                        "-fx-border-width: 1;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;"
        );
        clearTargetBtn.setPrefWidth(150);
        clearTargetBtn.setPrefHeight(30);
        clearTargetBtn.setOnAction(e -> clearTarget());

        targetingControls.getChildren().addAll(targetingLabel, targetModeBtn, clearTargetBtn);

        // Close button
        Button closeBtn = new Button("Close");
        closeBtn.setStyle(
                "-fx-background-color: red;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        closeBtn.setPrefWidth(100);
        closeBtn.setPrefHeight(30);
        closeBtn.setOnAction(e -> hideAmmunitionPanel());

        ammunitionPanel.getChildren().addAll(title, ammoButtons, targetingControls, closeBtn);

        // Add to root and show
        root.getChildren().add(ammunitionPanel);
        isAmmunitionPanelVisible = true;
    }


    private Button createAmmunitionButton(String displayName, String packetType, int count) {
        Button button = new Button(displayName + " (" + count + ")");
        button.setPrefWidth(150);
        button.setPrefHeight(35);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        // Store button reference for cooldown updates
        ammunitionButtons.put(packetType, button);

        // Initial state
        updateAmmunitionButtonState(button, packetType, count, 0.0);

        // Action: release packet
        button.setOnAction(e -> releasePacket(packetType));

        return button;
    }


    private void updateAmmunitionButtonState(Button button, String packetType, int count, double cooldownRemaining) {
        if (count <= 0) {
            // Out of ammunition - red display
            button.setStyle(
                    "-fx-background-color: rgba(255, 0, 0, 0.3);" +
                            "-fx-border-color: red;" +
                            "-fx-border-width: 2;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: default;"
            );
            button.setDisable(true);
        } else if (cooldownRemaining > 0) {
            // On cooldown - grayed out
            button.setStyle(
                    "-fx-background-color: rgba(128, 128, 128, 0.3);" +
                            "-fx-border-color: gray;" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: lightgray;" +
                            "-fx-cursor: default;"
            );
            button.setDisable(true);
            button.setText(button.getText().split(" \\(")[0] + " (" + count + ") - " + String.format("%.1fs", cooldownRemaining));
        } else {
            // Available - normal green state
            button.setStyle(
                    "-fx-background-color: rgba(0, 255, 0, 0.3);" +
                            "-fx-border-color: lime;" +
                            "-fx-border-width: 1;" +
                            "-fx-text-fill: white;" +
                            "-fx-cursor: hand;"
            );
            button.setDisable(false);
            button.setText(button.getText().split(" \\(")[0] + " (" + count + ")");

            // Hover effects for available buttons
            button.setOnMouseEntered(e -> {
                if (!button.isDisabled()) {
                    button.setStyle(
                            "-fx-background-color: rgba(0, 255, 0, 0.6);" +
                                    "-fx-border-color: lime;" +
                                    "-fx-border-width: 2;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-cursor: hand;"
                    );
                }
            });

            button.setOnMouseExited(e -> {
                if (!button.isDisabled()) {
                    button.setStyle(
                            "-fx-background-color: rgba(0, 255, 0, 0.3);" +
                                    "-fx-border-color: lime;" +
                                    "-fx-border-width: 1;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-cursor: hand;"
                    );
                }
            });
        }
    }


    private void releasePacket(String packetType) {
        if (selectedSystemId == null) {

            return;
        }



        // Connect to MultiplayerGameController to actually release the packet
        if (gameController != null && currentPlayerId != null) {
            // Create a player action for packet release
            multiplayer.PlayerAction action =
                    new multiplayer.PlayerAction(
                            currentPlayerId,
                            multiplayer.PlayerAction.ActionType.RELEASE_PACKET,
                            selectedSystemId,
                            packetType
                    );

            // Send the action to the game controller
            gameController.handlePlayerAction(currentPlayerId, action);

            // Show feedback
            showPacketReleaseFeedback(selectedSystemId, packetType);
        } else {

            showPacketReleaseFeedback(selectedSystemId, packetType);
        }

        // Hide ammunition panel after selection
        hideAmmunitionPanel();
    }

    private void showPacketReleaseFeedback(String systemId, String packetType) {
        // Create feedback label
        Label feedback = new Label("Released " + packetType + " from " + systemId);
        feedback.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        feedback.setTextFill(Color.LIME);
        feedback.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.7);" +
                        "-fx-padding: 10;"
        );

        // Position feedback
        feedback.setLayoutX(500);
        feedback.setLayoutY(200);

        // Add to root
        root.getChildren().add(feedback);

        // Remove after 2 seconds
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    root.getChildren().remove(feedback);
                })
        );
        timeline.play();
    }

    private void hideAmmunitionPanel() {
        if (isAmmunitionPanelVisible && ammunitionPanel != null) {
            root.getChildren().remove(ammunitionPanel);
            isAmmunitionPanelVisible = false;
            selectedSystemId = null;
        }
    }


    private void enterTargetMode() {
        isTargetingMode = true;
        createTargetingCanvas();

        // Show targeting instructions
        showTargetingInstructions();

        // Highlight opponent systems immediately
        highlightOpponentSystems();

        java.lang.System.out.println("TARGETING: Entered target mode - click on opponent systems to target them");
    }


    private void clearTarget() {
        isTargetingMode = false;
        selectedTargetSystemId = null;
        selectedTargetPosition = null;

        // Clear targeting canvas
        if (targetingCanvas != null) {
            root.getChildren().remove(targetingCanvas);
            targetingCanvas = null;
        }

        // Notify controller
        if (gameController != null) {
            gameController.clearTarget();
        }

        java.lang.System.out.println("TARGETING: Cleared target");
    }


    private void createTargetingCanvas() {
        if (targetingCanvas != null) {
            root.getChildren().remove(targetingCanvas);
        }

        targetingCanvas = new Canvas(1400, 800);
        targetingGC = targetingCanvas.getGraphicsContext2D();
        targetingCanvas.setMouseTransparent(false); // Allow clicks to be captured for targeting

        // Add mouse click handler for targeting
        targetingCanvas.setOnMouseClicked(event -> {
            if (isTargetingMode && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                handleTargetingClick(event.getX(), event.getY());
            }
        });

        root.getChildren().add(targetingCanvas);
    }

    private void showTargetingInstructions() {
        // Create instruction label
        Label instructionLabel = new Label("TARGETING MODE: Click on opponent systems to target them");
        instructionLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        instructionLabel.setTextFill(Color.YELLOW);
        instructionLabel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.7);" +
                        "-fx-padding: 10;" +
                        "-fx-background-radius: 5;"
        );

        // Position at top center
        instructionLabel.setLayoutX(500);
        instructionLabel.setLayoutY(50);

        root.getChildren().add(instructionLabel);

        // Highlight opponent systems for targeting
        highlightOpponentSystems();

        // Remove after 5 seconds
        javafx.animation.Timeline removeTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5), e -> {
                    root.getChildren().remove(instructionLabel);
                })
        );
        removeTimer.play();
    }


    private void highlightOpponentSystems() {
        java.lang.System.out.println("HIGHLIGHT_DEBUG: highlightOpponentSystems() called");

        if (targetingGC == null) {
            java.lang.System.out.println("HIGHLIGHT_DEBUG: targetingGC is null");
            return;
        }

        // Get opponent network from controller
        String currentPlayerId = getCurrentPlayerId();
        java.lang.System.out.println("HIGHLIGHT_DEBUG: currentPlayerId = " + currentPlayerId);

        if (gameController != null && currentPlayerId != null) {
            GameLevel opponentNetwork = gameController.getOpponentNetwork(currentPlayerId);
            java.lang.System.out.println("HIGHLIGHT_DEBUG: opponentNetwork = " + (opponentNetwork != null));

            if (opponentNetwork != null) {
                java.lang.System.out.println("HIGHLIGHT_DEBUG: Highlighting " + opponentNetwork.getSystems().size() + " opponent systems");

                // Clear previous highlights
                targetingGC.clearRect(0, 0, targetingCanvas.getWidth(), targetingCanvas.getHeight());

                // Draw highlighting circles around opponent systems
                targetingGC.setStroke(Color.YELLOW);
                targetingGC.setLineWidth(3);
                targetingGC.setLineDashes(5, 5); // Dashed line for highlighting

                for (model.System system : opponentNetwork.getSystems()) {
                    Point2D pos = system.getPosition();
                    // Apply offset to match the visual position of opponent network
                    Point2D offsetPos = new Point2D(pos.getX() + 200, pos.getY() + 200);
                    double radius = 30; // Highlighting circle radius

                    java.lang.System.out.println("HIGHLIGHT_DEBUG: Highlighting system " + system.getId() + " at original " + pos + " offset to " + offsetPos);

                    // Draw highlighting circle at offset position
                    targetingGC.strokeOval(offsetPos.getX() - radius, offsetPos.getY() - radius, radius * 2, radius * 2);

                    // Draw "CLICK ME" text
                    targetingGC.setFill(Color.YELLOW);
                    targetingGC.setFont(javafx.scene.text.Font.font("Arial", FontWeight.BOLD, 10));
                    targetingGC.fillText("CLICK ME", offsetPos.getX() - 25, offsetPos.getY() - radius - 5);
                }

                java.lang.System.out.println("HIGHLIGHT_DEBUG: Finished drawing highlighting circles");
            } else {
                java.lang.System.out.println("HIGHLIGHT_DEBUG: No opponent network found for player " + currentPlayerId);
            }
        } else {
            java.lang.System.out.println("HIGHLIGHT_DEBUG: gameController = " + (gameController != null) + ", currentPlayerId = " + currentPlayerId);
        }
    }

    /**
     * Handles mouse clicks in targeting mode.
     */
    private void handleTargetingClick(double x, double y) {
        java.lang.System.out.println("TARGETING_DEBUG: Click at (" + x + ", " + y + ")");

        // Find the system at the clicked position
        model.System clickedSystem = findSystemAtPosition(x, y);

        if (clickedSystem != null) {
            java.lang.System.out.println("TARGETING_DEBUG: Found system " + clickedSystem.getId() + " at " + clickedSystem.getPosition());
            java.lang.System.out.println("TARGETING_DEBUG: Is controllable: " + isSystemControllable(clickedSystem.getId()));

            // Check if it's an opponent system (not controllable by current player)
            if (!isSystemControllable(clickedSystem.getId())) {
                // Set as target
                selectedTargetSystemId = clickedSystem.getId();
                selectedTargetPosition = clickedSystem.getPosition();

                // Notify controller
                if (gameController != null) {
                    gameController.setTarget(selectedTargetSystemId, selectedTargetPosition);
                }

                // Show visual feedback
                showTargetingFeedback(clickedSystem);

                // Exit targeting mode
                isTargetingMode = false;

                java.lang.System.out.println("TARGETING_DEBUG: Successfully targeted " + selectedTargetSystemId);
            } else {
                // Show error - can't target own systems
                java.lang.System.out.println("TARGETING_DEBUG: Cannot target own system " + clickedSystem.getId());
                showTargetingError("Cannot target your own systems!");
            }
        } else {
            // Show error - no system found
            java.lang.System.out.println("TARGETING_DEBUG: No system found at position (" + x + ", " + y + ")");

            // Debug: show what networks are available
            if (gameController != null) {
                String currentPlayerId = getCurrentPlayerId();
                java.lang.System.out.println("TARGETING_DEBUG: Current player ID: " + currentPlayerId);

                GameLevel player1Network = gameController.getOpponentNetwork("Player 2");
                GameLevel player2Network = gameController.getOpponentNetwork("Player 1");
                java.lang.System.out.println("TARGETING_DEBUG: Player1 network: " + (player1Network != null));
                java.lang.System.out.println("TARGETING_DEBUG: Player2 network: " + (player2Network != null));

                if (player1Network != null) {
                    java.lang.System.out.println("TARGETING_DEBUG: Player1 has " + player1Network.getSystems().size() + " systems:");
                    for (model.System system : player1Network.getSystems()) {
                        java.lang.System.out.println("  - " + system.getId() + " at " + system.getPosition());
                    }
                }
                if (player2Network != null) {
                    java.lang.System.out.println("TARGETING_DEBUG: Player2 has " + player2Network.getSystems().size() + " systems:");
                    for (model.System system : player2Network.getSystems()) {
                        java.lang.System.out.println("  - " + system.getId() + " at " + system.getPosition());
                    }
                }
            }

            showTargetingError("No system found at this position!");
        }
    }

    /**
     * Shows visual feedback when a target is selected.
     */
    private void showTargetingFeedback(model.System targetSystem) {
        if (targetingGC == null) return;

        // Clear previous targeting visuals
        targetingGC.clearRect(0, 0, targetingCanvas.getWidth(), targetingCanvas.getHeight());

        // Draw targeting crosshair
        Point2D pos = targetSystem.getPosition();
        double size = 40;

        // Draw crosshair
        targetingGC.setStroke(Color.RED);
        targetingGC.setLineWidth(3);

        // Horizontal line
        targetingGC.strokeLine(pos.getX() - size, pos.getY(), pos.getX() + size, pos.getY());
        // Vertical line
        targetingGC.strokeLine(pos.getX(), pos.getY() - size, pos.getX(), pos.getY() + size);

        // Draw targeting circle
        targetingGC.setStroke(Color.RED);
        targetingGC.setLineWidth(2);
        targetingGC.strokeOval(pos.getX() - size/2, pos.getY() - size/2, size, size);

        // Draw target label
        targetingGC.setFill(Color.RED);
        targetingGC.setFont(javafx.scene.text.Font.font("Arial", FontWeight.BOLD, 12));
        targetingGC.fillText("TARGET", pos.getX() - 20, pos.getY() - size - 5);

        // Show success message
        showTargetingMessage("Target locked: " + targetSystem.getId(), Color.GREEN);
    }

    /**
     * Shows a targeting error message.
     */
    private void showTargetingError(String message) {
        showTargetingMessage(message, Color.RED);
    }

    /**
     * Shows a targeting message.
     */
    private void showTargetingMessage(String message, Color color) {
        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        messageLabel.setTextFill(color);
        messageLabel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.8);" +
                        "-fx-padding: 8;" +
                        "-fx-background-radius: 5;"
        );

        // Position at center
        messageLabel.setLayoutX(600);
        messageLabel.setLayoutY(400);

        root.getChildren().add(messageLabel);

        // Remove after 2 seconds
        javafx.animation.Timeline removeTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    root.getChildren().remove(messageLabel);
                })
        );
        removeTimer.play();
    }

    /**
     * Pauses the game.
     */
    private void pauseGame() {
        // Toggle pause state
        if (pauseButton.getText().equals("Pause Game")) {
            pauseButton.setText("Resume Game");
            pauseButton.setStyle(
                    "-fx-background-color: orange;" +
                            "-fx-text-fill: black;" +
                            "-fx-font-weight: bold;"
            );
        } else {
            pauseButton.setText("Pause Game");
            pauseButton.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-border-color: cyan;" +
                            "-fx-border-width: 2;" +
                            "-fx-text-fill: cyan;"
            );
        }
    }

    /**
     * Surrenders the game.
     */
    private void surrenderGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Surrender Game");
        alert.setHeaderText("Are you sure you want to surrender?");
        alert.setContentText("This will end the current game and return you to the main menu.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                mainApp.showMainMenu();
            }
        });
    }

    /**
     * Starts the setup phase countdown.
     */
    public void startSetupPhase() {
        setupPhasePanel.setVisible(true);
        gamePhasePanel.setVisible(false);

        // Initialize timer display with proper formatting - actual countdown will be handled by server
        setupTimerLabel.setText("Time: 01:00"); // Total time including penalty phases (30s + 30s)
        setupTimerLabel.setStyle(
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;"
        );


        // Note: Timer countdown is now handled by server synchronization
        // The MultiplayerGameController will update the timer via updateSetupTimer()
    }

    /**
     * Ends the setup phase and starts the game.
     */
    public void endSetupPhase() {
        java.lang.System.out.println("PHASE_DEBUG: endSetupPhase() called");
        setupPhasePanel.setVisible(false);
        gamePhasePanel.setVisible(true);

        gamePhaseLabel.setText("Phase: Game");
        setupInstructionsLabel.setText("Game started! Compete to deliver more packets!");

        // Share current network design with the multiplayer controller
        java.lang.System.out.println("PHASE_DEBUG: About to call shareNetworkWithController()");
        shareNetworkWithController();

        // Start game timer
        startGameTimer();

        // Start real-time UI updates
        startUIUpdates();
    }

    /**
     * Shares the current network design with the multiplayer controller.
     */
    private void shareNetworkWithController() {
        java.lang.System.out.println("SHARE_DEBUG: shareNetworkWithController() called");
        java.lang.System.out.println("SHARE_DEBUG: gameController = " + (gameController != null));
        java.lang.System.out.println("SHARE_DEBUG: currentLevel = " + (currentLevel != null));

        if (gameController != null) {
            // Determine the current player ID
            String currentPlayerId = getCurrentPlayerId();
            java.lang.System.out.println("SHARE_DEBUG: currentPlayerId = " + currentPlayerId);

            if (currentLevel != null) {
                java.lang.System.out.println("SHARE_DEBUG: currentLevel has " + currentLevel.getSystems().size() + " systems and " + currentLevel.getWireConnections().size() + " connections");

                // Share the current player's network design
                gameController.setPlayerNetwork(currentPlayerId, currentLevel);
                java.lang.System.out.println("SHARE_DEBUG: Called setPlayerNetwork successfully");
            } else {
                java.lang.System.out.println("SHARE_DEBUG: currentLevel is null, creating empty network");

                // Create a basic empty network if none exists
                model.GameLevel emptyNetwork = new model.GameLevel();
                emptyNetwork.setLevelId("empty_network_" + currentPlayerId);
                emptyNetwork.setName("Empty Network");

                gameController.setPlayerNetwork(currentPlayerId, emptyNetwork);
                java.lang.System.out.println("SHARE_DEBUG: Shared empty network for " + currentPlayerId);
            }
        } else {
            java.lang.System.out.println("SHARE_DEBUG: Cannot share - gameController is null");
        }
    }




    /**
     * Creates the network building canvas for the setup phase.
     */
    private void createNetworkBuildingCanvas() {
        networkCanvas = new Canvas(600, 400);
        networkGC = networkCanvas.getGraphicsContext2D();

        // Initialize network game controller and level
        initializeNetworkGameController();

        // Set up mouse event handlers for network building
        setupNetworkBuildingHandlers();

        // Set up keyboard event handlers for editing commands
        setupKeyboardHandlers();

        // Initial render
        renderNetworkBuilding();
    }

    /**
     * Initializes the network game controller for setup phase.
     */
    private void initializeNetworkGameController() {
        // Create a basic level with systems for network building
        currentLevel = new GameLevel();
        currentLevel.setLevelId("multiplayer_setup");
        currentLevel.setName("Multiplayer Setup");
        currentLevel.setInitialWireLength(1000.0); // Set wire length budget

        // Add basic systems (SRC, N nodes, DST) similar to the uploaded image
        addBasicNetworkSystems();

        // Create network game controller with proper game state
        GameState gameState = new GameState();
        gameState.setCurrentLevel(currentLevel);
        gameState.setRemainingWireLength(currentLevel.getInitialWireLength());

        networkGameController = new GameController(gameState);
        networkGameController.setMainApp(mainApp);


    }

    /**
     * Adds basic network systems for multiplayer setup.
     */
    private void addBasicNetworkSystems() {
        // SRC (Source) system - bottom left
        model.System srcSystem = new NormalSystem(new Point2D(100, 300));
        srcSystem.setId("SRC");
        currentLevel.addSystem(srcSystem);

        // N (Node) systems - top and bottom center
        model.System nSystem1 = new NormalSystem(new Point2D(300, 150));
        nSystem1.setId("N1");
        currentLevel.addSystem(nSystem1);

        model.System nSystem2 = new NormalSystem(new Point2D(300, 300));
        nSystem2.setId("N2");
        currentLevel.addSystem(nSystem2);

        // DST (Destination) system - middle right
        model.System dstSystem = new NormalSystem(new Point2D(500, 250));
        dstSystem.setId("DST");
        currentLevel.addSystem(dstSystem);

        // Add ports to systems
        addPortsToSystems();
    }

    /**
     * Adds ports to the network systems.
     */
    private void addPortsToSystems() {
        // Add ports to SRC system (right side)
        model.System srcSystem = findSystemById("SRC");
        if (srcSystem != null) {
            Port srcPort1 = new Port(PortShape.SQUARE, srcSystem, new Point2D(120, 290), false);
            Port srcPort2 = new Port(PortShape.SQUARE, srcSystem, new Point2D(120, 300), false);
            Port srcPort3 = new Port(PortShape.TRIANGLE, srcSystem, new Point2D(120, 310), false);
            srcSystem.getOutputPorts().add(srcPort1);
            srcSystem.getOutputPorts().add(srcPort2);
            srcSystem.getOutputPorts().add(srcPort3);
        }

        // Add ports to N1 system (both sides)
        model.System nSystem1 = findSystemById("N1");
        if (nSystem1 != null) {
            // Left side ports (input)
            Port n1Left1 = new Port(PortShape.SQUARE, nSystem1, new Point2D(280, 140), true);
            Port n1Left2 = new Port(PortShape.SQUARE, nSystem1, new Point2D(280, 150), true);
            Port n1Left3 = new Port(PortShape.TRIANGLE, nSystem1, new Point2D(280, 160), true);
            nSystem1.getInputPorts().add(n1Left1);
            nSystem1.getInputPorts().add(n1Left2);
            nSystem1.getInputPorts().add(n1Left3);
            // Right side ports (output)
            Port n1Right1 = new Port(PortShape.SQUARE, nSystem1, new Point2D(320, 140), false);
            Port n1Right2 = new Port(PortShape.SQUARE, nSystem1, new Point2D(320, 150), false);
            Port n1Right3 = new Port(PortShape.TRIANGLE, nSystem1, new Point2D(320, 160), false);
            nSystem1.getOutputPorts().add(n1Right1);
            nSystem1.getOutputPorts().add(n1Right2);
            nSystem1.getOutputPorts().add(n1Right3);
        }

        // Add ports to N2 system (both sides)
        model.System nSystem2 = findSystemById("N2");
        if (nSystem2 != null) {
            // Left side ports (input)
            Port n2Left1 = new Port(PortShape.SQUARE, nSystem2, new Point2D(280, 290), true);
            Port n2Left2 = new Port(PortShape.SQUARE, nSystem2, new Point2D(280, 300), true);
            Port n2Left3 = new Port(PortShape.TRIANGLE, nSystem2, new Point2D(280, 310), true);
            nSystem2.getInputPorts().add(n2Left1);
            nSystem2.getInputPorts().add(n2Left2);
            nSystem2.getInputPorts().add(n2Left3);
            // Right side ports (output)
            Port n2Right1 = new Port(PortShape.SQUARE, nSystem2, new Point2D(320, 290), false);
            Port n2Right2 = new Port(PortShape.SQUARE, nSystem2, new Point2D(320, 300), false);
            Port n2Right3 = new Port(PortShape.TRIANGLE, nSystem2, new Point2D(320, 310), false);
            nSystem2.getOutputPorts().add(n2Right1);
            nSystem2.getOutputPorts().add(n2Right2);
            nSystem2.getOutputPorts().add(n2Right3);
        }

        // Add ports to DST system (left side)
        model.System dstSystem = findSystemById("DST");
        if (dstSystem != null) {
            Port dstPort1 = new Port(PortShape.SQUARE, dstSystem, new Point2D(480, 240), true);
            Port dstPort2 = new Port(PortShape.SQUARE, dstSystem, new Point2D(480, 250), true);
            Port dstPort3 = new Port(PortShape.TRIANGLE, dstSystem, new Point2D(480, 260), true);
            dstSystem.getInputPorts().add(dstPort1);
            dstSystem.getInputPorts().add(dstPort2);
            dstSystem.getInputPorts().add(dstPort3);
        }
    }


    private model.System findSystemById(String id) {
        for (model.System system : currentLevel.getSystems()) {
            if (id.equals(system.getId())) {
                return system;
            }
        }
        return null;
    }


    private void setupNetworkBuildingHandlers() {
        networkCanvas.setOnMousePressed(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                // Left-click: Check for system selection first, then wire creation
                Point2D mousePos = new Point2D(event.getX(), event.getY());

                // Check if in targeting mode
                if (isTargetingMode) {
                    handleTargetingClick(event.getX(), event.getY());
                    return;
                }

                // Check if clicking on a controllable system
                model.System clickedSystem = findSystemAtPosition(event.getX(), event.getY());
                if (clickedSystem != null && isSystemControllable(clickedSystem.getId())) {
                    // Select the controllable system
                    selectSystem(clickedSystem.getId());

                    return;
                }

                // If not a controllable system, check for port for wire creation
                Port clickedPort = findPortAtPosition(mousePos);
                if (clickedPort != null) {
                    wirePreviewStart = clickedPort.getPosition();
                    showWirePreview = true;

                }
            } else if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                // Right-click: Remove wire
                handleRightClickWireRemoval(event);
            }
        });

        networkCanvas.setOnMouseDragged(event -> {
            if (showWirePreview) {
                wirePreviewEnd = new Point2D(event.getX(), event.getY());
                renderNetworkBuilding();
            }
        });

        networkCanvas.setOnMouseReleased(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (showWirePreview && wirePreviewEnd != null) {
                    Point2D mousePos = new Point2D(event.getX(), event.getY());
                    Port targetPort = findPortAtPosition(mousePos);

                    if (targetPort != null && wirePreviewStart != null) {
                        // Create wire connection
                        createWireConnection(wirePreviewStart, targetPort.getPosition());
                    }

                    showWirePreview = false;
                    wirePreviewStart = null;
                    wirePreviewEnd = null;
                    renderNetworkBuilding();
                }
            }
        });
    }

    private void handleRightClickWireRemoval(javafx.scene.input.MouseEvent event) {
        Point2D clickPos = new Point2D(event.getX(), event.getY());
        WireConnection wireToRemove = findWireAtPosition(clickPos);

        if (wireToRemove != null) {
            // Remove the wire using proper controller
            if (networkGameController != null) {
                boolean success = networkGameController.getWiringController().removeWireConnection(
                        wireToRemove, networkGameController.getGameState()
                );
                if (success) {

                    showWireRemovalFeedback("Wire removed successfully!");
                    renderNetworkBuilding(); // Refresh display
                } else {

                    showConnectionError("Failed to remove wire");
                }
            } else {
                // Fallback: direct removal
                currentLevel.getWireConnections().remove(wireToRemove);

                showWireRemovalFeedback("Wire removed!");
                renderNetworkBuilding(); // Refresh display
            }
        } else {

        }
    }


    private WireConnection findWireAtPosition(Point2D position) {
        for (WireConnection connection : currentLevel.getWireConnections()) {
            if (isPointNearWire(position, connection)) {
                return connection;
            }
        }
        return null;
    }

    private boolean isPointNearWire(Point2D point, WireConnection wire) {
        Point2D start = wire.getSourcePort().getPosition();
        Point2D end = wire.getDestinationPort().getPosition();

        // Calculate distance from point to line segment
        double distance = distanceToLineSegment(point, start, end);
        return distance <= 10.0; // 10 pixel tolerance
    }

    private double distanceToLineSegment(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double A = point.getX() - lineStart.getX();
        double B = point.getY() - lineStart.getY();
        double C = lineEnd.getX() - lineStart.getX();
        double D = lineEnd.getY() - lineStart.getY();

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;

        if (lenSq == 0) {
            // Line segment is actually a point
            return Math.sqrt(A * A + B * B);
        }

        double param = dot / lenSq;

        double xx, yy;
        if (param < 0) {
            xx = lineStart.getX();
            yy = lineStart.getY();
        } else if (param > 1) {
            xx = lineEnd.getX();
            yy = lineEnd.getY();
        } else {
            xx = lineStart.getX() + param * C;
            yy = lineStart.getY() + param * D;
        }

        double dx = point.getX() - xx;
        double dy = point.getY() - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void showWireRemovalFeedback(String message) {
        // Create feedback label
        Label feedback = new Label(message);
        feedback.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        feedback.setTextFill(Color.LIME);
        feedback.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.8);" +
                        "-fx-padding: 8;" +
                        "-fx-background-radius: 5;"
        );

        // Position feedback
        feedback.setLayoutX(300);
        feedback.setLayoutY(80);

        // Add to root
        root.getChildren().add(feedback);

        // Remove after 2 seconds
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> {
                    root.getChildren().remove(feedback);
                })
        );
        timeline.play();
    }

    private void setupKeyboardHandlers() {
        // Make the canvas focusable to receive keyboard events
        networkCanvas.setFocusTraversable(true);

        networkCanvas.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case R:
                    // Reset/restart the network
                    resetNetwork();
                    break;
                case C:
                    // Clear all connections
                    clearAllConnections();
                    break;
                case ESCAPE:
                    // Cancel current operation
                    cancelCurrentOperation();
                    break;
                case DELETE:
                case BACK_SPACE:
                    // Remove selected wire (if any)
                    removeSelectedWire();
                    break;
                default:
                    // Handle other keys if needed
                    break;
            }
        });
    }

    private void resetNetwork() {
        if (currentLevel != null) {
            // Clear all wire connections
            currentLevel.getWireConnections().clear();

            // Reset wire length budget
            if (networkGameController != null) {
                networkGameController.getGameState().setRemainingWireLength(currentLevel.getInitialWireLength());
            }

            // Cancel any ongoing operations
            cancelCurrentOperation();

            // Refresh display
            renderNetworkBuilding();

            showConnectionError("Network reset to initial state");

        }
    }

    private void clearAllConnections() {
        if (currentLevel != null && !currentLevel.getWireConnections().isEmpty()) {
            int connectionCount = currentLevel.getWireConnections().size();
            currentLevel.getWireConnections().clear();

            // Reset wire length budget
            if (networkGameController != null) {
                networkGameController.getGameState().setRemainingWireLength(currentLevel.getInitialWireLength());
            }

            // Cancel any ongoing operations
            cancelCurrentOperation();

            // Refresh display
            renderNetworkBuilding();

            showConnectionError("Cleared " + connectionCount + " wire connections");

        }
    }


    private void cancelCurrentOperation() {
        showWirePreview = false;
        wirePreviewStart = null;
        wirePreviewEnd = null;
        renderNetworkBuilding();
    }


    private void removeSelectedWire() {
        // For now, this is a placeholder
        // In a full implementation, you would have a wire selection system
        showConnectionError("Press R to reset network or right-click wires to remove them");
    }

    private Port findPortAtPosition(Point2D position) {
        for (model.System system : currentLevel.getSystems()) {
            // Check input ports
            for (Port port : system.getInputPorts()) {
                Point2D portPos = port.getPosition();
                double distance = Math.sqrt(
                        Math.pow(position.getX() - portPos.getX(), 2) +
                                Math.pow(position.getY() - portPos.getY(), 2)
                );
                if (distance <= 10) { // 10 pixel tolerance
                    return port;
                }
            }
            // Check output ports
            for (Port port : system.getOutputPorts()) {
                Point2D portPos = port.getPosition();
                double distance = Math.sqrt(
                        Math.pow(position.getX() - portPos.getX(), 2) +
                                Math.pow(position.getY() - portPos.getY(), 2)
                );
                if (distance <= 10) { // 10 pixel tolerance
                    return port;
                }
            }
        }
        return null;
    }

    private void createWireConnection(Point2D start, Point2D end) {
        // Find the actual ports
        Port startPort = findPortAtPosition(start);
        Port endPort = findPortAtPosition(end);

        if (startPort != null && endPort != null) {
            // Use proper validation before creating connection
            if (isValidConnection(startPort, endPort)) {
                // Create wire connection using proper controller
                if (networkGameController != null) {
                    // Determine current player ID for wire ownership
                    String currentPlayerId = getCurrentPlayerIdForWireOwnership();
                    boolean success = networkGameController.getWiringController().createWireConnection(
                            startPort, endPort, networkGameController.getGameState(), currentPlayerId
                    );
                    if (success) {

                        renderNetworkBuilding(); // Refresh display
                    } else {

                        showConnectionError("Invalid connection - check port compatibility and wire length");
                    }
                } else {
                    // Fallback: direct creation with basic validation
                    WireConnection connection = new WireConnection(startPort, endPort);
                    // Set player ownership for the connection
                    String currentPlayerId = getCurrentPlayerIdForWireOwnership();
                    if (currentPlayerId != null) {
                        connection.setOwnerPlayerId(currentPlayerId);
                    }
                    if (connection.isValid()) {
                        currentLevel.addWireConnection(connection);

                        renderNetworkBuilding(); // Refresh display
                    } else {

                        showConnectionError("Invalid connection - ports not compatible");
                    }
                }
            } else {

                showConnectionError("Invalid connection - check port rules");
            }
        }
    }

    private boolean isValidConnection(Port port1, Port port2) {
        // Check if ports are from the same system
        if (port1.getParentSystem() == port2.getParentSystem()) {
            return false;
        }

        // Check if one is input and one is output
        if (port1.isInput() == port2.isInput()) {
            return false;
        }

        // Check if connection already exists
        for (WireConnection existing : currentLevel.getWireConnections()) {
            if ((existing.getSourcePort() == port1 && existing.getDestinationPort() == port2) ||
                    (existing.getSourcePort() == port2 && existing.getDestinationPort() == port1)) {
                return false;
            }
        }

        // Check if either port is already connected
        if (port1.isConnected() || port2.isConnected()) {
            return false;
        }

        // Check port shape compatibility
        if (!port1.getShape().isCompatibleWith(port2.getShape())) {
            return false;
        }

        return true;
    }


    private void showConnectionError(String message) {
        // Create error label
        Label errorLabel = new Label(message);
        errorLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        errorLabel.setTextFill(Color.RED);
        errorLabel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.8);" +
                        "-fx-padding: 8;" +
                        "-fx-background-radius: 5;"
        );

        // Position error message
        errorLabel.setLayoutX(300);
        errorLabel.setLayoutY(50);

        // Add to root
        root.getChildren().add(errorLabel);

        // Remove after 3 seconds
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> {
                    root.getChildren().remove(errorLabel);
                })
        );
        timeline.play();
    }

    private void renderNetworkBuilding() {
        if (networkGC == null || currentLevel == null) return;

        // Clear canvas
        networkGC.clearRect(0, 0, networkCanvas.getWidth(), networkCanvas.getHeight());
        networkGC.setFill(Color.BLACK);
        networkGC.fillRect(0, 0, networkCanvas.getWidth(), networkCanvas.getHeight());

        // Draw systems
        drawNetworkSystems();

        // Draw wire connections
        drawNetworkWireConnections();

        // Draw wire preview
        if (showWirePreview && wirePreviewStart != null && wirePreviewEnd != null) {
            drawWirePreview();
        }

        // Draw packets in multiplayer mode
        if (gameController != null && !gameController.isSetupPhase()) {
            drawMultiplayerPackets();
        }

        // Draw connection status
        drawConnectionStatus();
    }


    private void drawNetworkSystems() {
        for (model.System system : currentLevel.getSystems()) {
            Point2D pos = system.getPosition();

            // Check if this system is controllable
            boolean isControllable = isSystemControllable(system.getId());

            // Draw system rectangle with different styling for controllable systems
            Color systemColor = getSystemColor(system.getId());
            networkGC.setFill(systemColor);
            networkGC.fillRect(pos.getX() - 30, pos.getY() - 15, 60, 30);

            // Draw special border for controllable systems
            if (isControllable) {
                // Draw glowing border for controllable systems
                networkGC.setStroke(Color.CYAN);
                networkGC.setLineWidth(3);
                networkGC.strokeRect(pos.getX() - 32, pos.getY() - 17, 64, 34);

                // Draw control indicator
                networkGC.setFill(Color.YELLOW);
                networkGC.setFont(javafx.scene.text.Font.font("Arial", FontWeight.BOLD, 10));
                networkGC.fillText("CTRL", pos.getX() - 15, pos.getY() - 20);
            } else {
                // Regular border for non-controllable systems
                networkGC.setStroke(Color.WHITE);
                networkGC.setLineWidth(1);
                networkGC.strokeRect(pos.getX() - 30, pos.getY() - 15, 60, 30);
            }

            // Draw system label
            networkGC.setFill(Color.WHITE);
            networkGC.setFont(javafx.scene.text.Font.font("Arial", 12));
            networkGC.fillText(system.getId(), pos.getX() - 10, pos.getY() + 5);

            // Draw input ports
            for (Port port : system.getInputPorts()) {
                drawNetworkPort(port);
            }
            // Draw output ports
            for (Port port : system.getOutputPorts()) {
                drawNetworkPort(port);
            }
        }
    }

    private Color getSystemColor(String systemId) {
        switch (systemId) {
            case "SRC": return Color.GREEN;
            case "N": return Color.BLUE;
            case "DST": return Color.RED;
            default: return Color.GRAY;
        }
    }


    private boolean isSystemControllable(String systemId) {
        if (gameController == null) return false;

        // Check if the system is in the controllable systems list
        Map<String, multiplayer.SystemInfo> controllableSystems =
                gameController.getControllableSystems();

        if (controllableSystems != null && controllableSystems.containsKey(systemId)) {
            multiplayer.SystemInfo systemInfo = controllableSystems.get(systemId);
            return systemInfo != null && systemInfo.isAvailable();
        }

        return false;
    }


    private model.System findSystemAtPosition(double x, double y) {
        Point2D clickPos = new Point2D(x, y);
        java.lang.System.out.println("FIND_SYSTEM_DEBUG: Searching at position (" + x + ", " + y + ")");

        // First, search in current player's network
        if (currentLevel != null) {
            java.lang.System.out.println("FIND_SYSTEM_DEBUG: Searching in current player network with " + currentLevel.getSystems().size() + " systems");
            for (model.System system : currentLevel.getSystems()) {
                Point2D systemPos = system.getPosition();
                if (systemPos != null) {
                    double distance = clickPos.distanceTo(systemPos);
                    java.lang.System.out.println("FIND_SYSTEM_DEBUG: Checking current system " + system.getId() + " at " + systemPos + " (distance: " + distance + ")");
                    if (distance <= 30) { // 30 pixel radius
                        java.lang.System.out.println("FIND_SYSTEM_DEBUG: Found current system " + system.getId() + " within range!");
                        return system;
                    }
                }
            }
        } else {
            java.lang.System.out.println("FIND_SYSTEM_DEBUG: No current player network");
        }

        // If in targeting mode, also search in opponent's network
        if (isTargetingMode && gameController != null) {
            java.lang.System.out.println("FIND_SYSTEM_DEBUG: In targeting mode, searching opponent networks...");
            // Try to get opponent network - we need to determine which player we are
            String currentPlayerId = getCurrentPlayerId();
            java.lang.System.out.println("FIND_SYSTEM_DEBUG: Current player ID: " + currentPlayerId);

            if (currentPlayerId != null) {
                GameLevel opponentNetwork = gameController.getOpponentNetwork(currentPlayerId);
                if (opponentNetwork != null) {
                    java.lang.System.out.println("FIND_SYSTEM_DEBUG: Found opponent network with " + opponentNetwork.getSystems().size() + " systems");
                    for (model.System system : opponentNetwork.getSystems()) {
                        Point2D systemPos = system.getPosition();
                        if (systemPos != null) {
                            // Apply offset to match the visual position of opponent network
                            // Based on the image, opponent network is offset below and to the right
                            Point2D offsetPos = new Point2D(systemPos.getX() + 200, systemPos.getY() + 200);

                            double distance = clickPos.distanceTo(offsetPos);
                            java.lang.System.out.println("FIND_SYSTEM_DEBUG: Checking opponent system " + system.getId() + " at original " + systemPos + " offset to " + offsetPos + " (distance: " + distance + ")");
                            if (distance <= 30) { // 30 pixel radius
                                java.lang.System.out.println("FIND_SYSTEM_DEBUG: Found opponent system " + system.getId() + " within range!");
                                return system;
                            }
                        }
                    }
                } else {
                    java.lang.System.out.println("FIND_SYSTEM_DEBUG: No opponent network found for player " + currentPlayerId);
                }
            } else {
                java.lang.System.out.println("FIND_SYSTEM_DEBUG: No current player ID");
            }
        } else {
            java.lang.System.out.println("FIND_SYSTEM_DEBUG: Not in targeting mode or no game controller");
        }

        java.lang.System.out.println("FIND_SYSTEM_DEBUG: No system found at position (" + x + ", " + y + ")");
        return null;
    }


    private void drawNetworkPort(Port port) {
        Point2D pos = port.getPosition();

        networkGC.setFill(Color.ORANGE);

        if (port.getShape() == PortShape.SQUARE) {
            networkGC.fillRect(pos.getX() - 5, pos.getY() - 5, 10, 10);
            networkGC.setStroke(Color.WHITE);
            networkGC.strokeRect(pos.getX() - 5, pos.getY() - 5, 10, 10);
        } else {
            double[] xPoints = {pos.getX(), pos.getX() - 5, pos.getX() + 5};
            double[] yPoints = {pos.getY() - 5, pos.getY() + 5, pos.getY() + 5};
            networkGC.fillPolygon(xPoints, yPoints, 3);
            networkGC.setStroke(Color.WHITE);
            networkGC.strokePolygon(xPoints, yPoints, 3);
        }
    }

    private void drawNetworkWireConnections() {
        for (WireConnection connection : currentLevel.getWireConnections()) {
            Point2D start = connection.getSourcePort().getPosition();
            Point2D end = connection.getDestinationPort().getPosition();

            networkGC.setStroke(Color.CYAN);
            networkGC.setLineWidth(2);
            networkGC.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
        }
    }

    private void drawWirePreview() {
        networkGC.setStroke(Color.YELLOW);
        networkGC.setLineWidth(2);
        networkGC.setLineDashes(5, 5);
        networkGC.strokeLine(wirePreviewStart.getX(), wirePreviewStart.getY(),
                wirePreviewEnd.getX(), wirePreviewEnd.getY());
        networkGC.setLineDashes(); // Reset line dashes
    }

    private void drawConnectionStatus() {
        int y = 20;

        // Connected status
        boolean connected = isNetworkConnected();
        networkGC.setFill(connected ? Color.LIME : Color.YELLOW);
        networkGC.fillText("Connected: " + (connected ? "Yes" : "No"), 10, y);

        // Reachable systems
        int reachable = getReachableSystemCount();
        int total = currentLevel.getSystems().size();
        networkGC.setFill(Color.CYAN);
        networkGC.fillText("Reachable: " + reachable + "/" + total + " systems", 10, y + 15);

        // Port connectivity
        int[] portCounts = getPortConnectivityCounts();
        networkGC.setFill(Color.ORANGE);
        networkGC.fillText("Ports: " + portCounts[0] + "/" + portCounts[1] + " connected", 10, y + 30);
    }

    private boolean isNetworkConnected() {
        // Simple connectivity check - if we have any wire connections
        return !currentLevel.getWireConnections().isEmpty();
    }

    private int getReachableSystemCount() {
        // Simple implementation - count systems with connections
        int reachable = 0;
        for (model.System system : currentLevel.getSystems()) {
            boolean hasConnection = false;
            for (WireConnection connection : currentLevel.getWireConnections()) {
                if (connection.getSourcePort().getParentSystem() == system ||
                        connection.getDestinationPort().getParentSystem() == system) {
                    hasConnection = true;
                    break;
                }
            }
            if (hasConnection) reachable++;
        }
        return reachable;
    }

    /**
     * Gets port connectivity counts.
     */
    private int[] getPortConnectivityCounts() {
        int connected = 0;
        int total = 0;

        for (model.System system : currentLevel.getSystems()) {
            // Count input ports
            for (Port port : system.getInputPorts()) {
                total++;
                // Check if this port is connected
                for (WireConnection connection : currentLevel.getWireConnections()) {
                    if (connection.getSourcePort() == port || connection.getDestinationPort() == port) {
                        connected++;
                        break;
                    }
                }
            }
            // Count output ports
            for (Port port : system.getOutputPorts()) {
                total++;
                // Check if this port is connected
                for (WireConnection connection : currentLevel.getWireConnections()) {
                    if (connection.getSourcePort() == port || connection.getDestinationPort() == port) {
                        connected++;
                        break;
                    }
                }
            }
        }

        return new int[]{connected, total};
    }

    private VBox createEditingCommandsPanel() {
        VBox commandsPanel = new VBox(10);
        commandsPanel.setAlignment(Pos.TOP_LEFT);
        commandsPanel.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-background-radius: 5;");
        commandsPanel.setPadding(new Insets(10));

        // EDITING MODE title
        Text editingTitle = new Text("EDITING MODE");
        editingTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        editingTitle.setFill(Color.LIME);

        // Commands list
        VBox commandsList = new VBox(3);
        commandsList.setAlignment(Pos.TOP_LEFT);

        String[] commands = {
                "Click & Drag Ports - Connect Wires",
                "Right-Click Wire - Remove Wire",
                "B - Bend Creation Mode",
                "M - Wire Merge Mode",
                "I - Toggle System Indicators",
                "C - Toggle Smooth Wire Curves",
                "R - Start Simulation",
                "S - Shop",
                "P - Menu (Pause/Exit)",
                "ESC - Exit Current Mode"
        };

        for (String command : commands) {
            Text commandText = new Text(command);
            commandText.setFont(Font.font("Arial", 11));
            commandText.setFill(Color.WHITE);
            commandsList.getChildren().add(commandText);
        }

        // VIEWPORT CONTROLS title
        Text viewportTitle = new Text("VIEWPORT CONTROLS");
        viewportTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        viewportTitle.setFill(Color.CYAN);

        // Viewport commands
        VBox viewportList = new VBox(3);
        viewportList.setAlignment(Pos.TOP_LEFT);

        String[] viewportCommands = {
                "Mouse Wheel - Zoom In/Out",
                "Space + Drag - Pan View",
                "R - Reset View (Show All Systems)",
                "Auto-scale: ON"
        };

        for (String command : viewportCommands) {
            Text commandText = new Text(command);
            commandText.setFont(Font.font("Arial", 11));
            commandText.setFill(Color.WHITE);
            viewportList.getChildren().add(commandText);
        }

        commandsPanel.getChildren().addAll(editingTitle, commandsList, viewportTitle, viewportList);
        return commandsPanel;
    }


    private void startGameTimer() {
        final int[] gameTime = {0};
        Timeline gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            gameTime[0]++;
            int minutes = gameTime[0] / 60;
            int seconds = gameTime[0] % 60;
            gameTimerLabel.setText(String.format("Game Time: %d:%02d", minutes, seconds));
        }));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        gameTimer.play();
    }

    private void startUIUpdates() {
        if (uiUpdateTimer != null) {
            uiUpdateTimer.stop();
        }

        uiUpdateTimer = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            updateAmmunitionDisplay();
            updateCooldownDisplay();
            updateOpponentNetworkVisibility();
        }));
        uiUpdateTimer.setCycleCount(Timeline.INDEFINITE);
        uiUpdateTimer.play();
    }


    private void updateAmmunitionDisplay() {
        if (gameController == null) return;

        try {
            // Get current ammunition counts from the game controller
            Map<String, Integer> ammunitionCounts = gameController.getAmmunitionCounts();

            for (Map.Entry<String, Label> entry : ammunitionLabels.entrySet()) {
                String packetType = entry.getKey();
                Label label = entry.getValue();

                int count = ammunitionCounts.getOrDefault(packetType, 0);

                // Update label text and color based on count
                String displayName = getDisplayNameForPacketType(packetType);
                label.setText(displayName + ": " + count);

                // Change color based on ammunition level
                if (count == 0) {
                    label.setTextFill(Color.RED);
                } else if (count <= 2) {
                    label.setTextFill(Color.ORANGE);
                } else {
                    label.setTextFill(Color.WHITE);
                }
            }

            // Update ammunition panel buttons with real-time cooldown information
            updateAmmunitionPanelCooldowns();

        } catch (Exception e) {
            // Handle any errors gracefully
            java.lang.System.err.println("Error updating ammunition display: " + e.getMessage());
        }
    }

    private void updateAmmunitionPanelCooldowns() {
        if (gameController == null || ammunitionButtons == null) return;

        try {
            // Get cooldown information from the game controller
            Map<String, multiplayer.CooldownInfo> cooldowns = gameController.getCooldowns();
            Map<String, Integer> ammunitionCounts = gameController.getAmmunitionCounts();

            for (Map.Entry<String, Button> entry : ammunitionButtons.entrySet()) {
                String packetType = entry.getKey();
                Button button = entry.getValue();

                int count = ammunitionCounts.getOrDefault(packetType, 0);
                double cooldownRemaining = 0.0;

                // Get cooldown information for this packet type
                if (cooldowns != null && cooldowns.containsKey(packetType)) {
                    multiplayer.CooldownInfo cooldownInfo = cooldowns.get(packetType);
                    if (cooldownInfo != null) {
                        cooldownRemaining = cooldownInfo.getRemainingTime();
                    }
                }

                // Update button state with cooldown information
                updateAmmunitionButtonState(button, packetType, count, cooldownRemaining);
            }
        } catch (Exception e) {
            java.lang.System.err.println("Error updating ammunition panel cooldowns: " + e.getMessage());
        }
    }

    private void updateCooldownDisplay() {
        if (gameController == null) return;

        try {
            // Get current cooldown information from the game controller
            Map<String, Double> cooldownTimes = gameController.getCooldownTimes();

            for (Map.Entry<String, Label> entry : cooldownLabels.entrySet()) {
                String systemId = entry.getKey();
                Label label = entry.getValue();

                double remainingTime = cooldownTimes.getOrDefault(systemId, 0.0);

                // Update label text and color based on cooldown
                String displayName = "System " + systemId.substring(systemId.length() - 1);
                if (remainingTime > 0) {
                    label.setText(displayName + ": " + String.format("%.1fs", remainingTime));
                    label.setTextFill(Color.RED);
                } else {
                    label.setText(displayName + ": Ready");
                    label.setTextFill(Color.GREEN);
                }
            }
        } catch (Exception e) {
            // Handle any errors gracefully
            java.lang.System.err.println("Error updating cooldown display: " + e.getMessage());
        }
    }

    private void updateOpponentNetworkVisibility() {
        if (gameController == null) return;

        try {
            // Get opponent information from the game controller
            int opponentScore = gameController.getOpponentScore();
            Map<String, Integer> opponentAmmunition = gameController.getOpponentAmmunition();
            double networkHealth = gameController.getOpponentNetworkHealth();

            // Update opponent score
            opponentScoreLabel.setText("Score: " + opponentScore);

            // Update opponent ammunition display
            if (opponentAmmunition != null && !opponentAmmunition.isEmpty()) {
                int totalAmmunition = opponentAmmunition.values().stream().mapToInt(Integer::intValue).sum();
                opponentAmmunitionLabel.setText("Ammunition: " + totalAmmunition + " total");

                // Change color based on ammunition level
                if (totalAmmunition == 0) {
                    opponentAmmunitionLabel.setTextFill(Color.RED);
                } else if (totalAmmunition <= 5) {
                    opponentAmmunitionLabel.setTextFill(Color.ORANGE);
                } else {
                    opponentAmmunitionLabel.setTextFill(Color.GREEN);
                }
            } else {
                opponentAmmunitionLabel.setText("Ammunition: Unknown");
                opponentAmmunitionLabel.setTextFill(Color.LIGHTGRAY);
            }

            // Update network health bar
            opponentNetworkHealthBar.setProgress(networkHealth);

            // Change health bar color based on health level
            if (networkHealth > 0.7) {
                opponentNetworkHealthBar.setStyle("-fx-accent: green;");
            } else if (networkHealth > 0.3) {
                opponentNetworkHealthBar.setStyle("-fx-accent: orange;");
            } else {
                opponentNetworkHealthBar.setStyle("-fx-accent: red;");
            }

            // Update network status and visibility
            if (gameController.isSetupPhase()) {
                opponentNetworkStatusLabel.setText("Network: Hidden during setup");
                opponentNetworkStatusLabel.setTextFill(Color.LIGHTGRAY);
                // Hide opponent network during setup phase
                hideOpponentNetwork();
            } else {
                // Show opponent network after setup phase if visibility is enabled
                boolean visibilityEnabled = gameController.isNetworkVisibilityEnabled();
                if (visibilityEnabled) {
                    String currentPlayerId = getCurrentPlayerId();
                    model.GameLevel opponentNetwork = gameController.getOpponentNetwork(currentPlayerId);
                    if (opponentNetwork != null) {
                        showOpponentNetwork(opponentNetwork);
                        opponentNetworkStatusLabel.setText("Network: Visible - " + String.format("%.0f%%", networkHealth * 100) + " health");
                        if (networkHealth > 0.7) {
                            opponentNetworkStatusLabel.setTextFill(Color.GREEN);
                        } else if (networkHealth > 0.3) {
                            opponentNetworkStatusLabel.setTextFill(Color.ORANGE);
                        } else {
                            opponentNetworkStatusLabel.setTextFill(Color.RED);
                        }
                    } else {
                        opponentNetworkStatusLabel.setText("Network: Not available");
                        opponentNetworkStatusLabel.setTextFill(Color.LIGHTGRAY);
                        hideOpponentNetwork();
                    }
                } else {
                    opponentNetworkStatusLabel.setText("Network: Visibility disabled");
                    opponentNetworkStatusLabel.setTextFill(Color.LIGHTGRAY);
                    hideOpponentNetwork();
                }
            }

        } catch (Exception e) {
            // Handle any errors gracefully
            java.lang.System.err.println("Error updating opponent network visibility: " + e.getMessage());
        }
    }

    private String getDisplayNameForPacketType(String packetType) {
        switch (packetType) {
            case "SMALL_MESSENGER": return "Small Messenger";
            case "PROTECTED": return "Protected";
            case "BULK_SMALL": return "Bulk Small";
            case "TROJAN": return "Trojan";
            case "BIT_PACKET": return "Bit Packet";
            default: return packetType;
        }
    }

    /**
     * Gets the root node of this view.
     */
    public StackPane getRoot() {
        return root;
    }
}



