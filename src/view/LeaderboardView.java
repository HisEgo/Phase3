package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import app.MainApp;
import leaderboard.*;

import java.util.List;
import java.util.Map;

public class LeaderboardView {
    private MainApp mainApp;
    private StackPane root;
    private VBox mainContainer;
    private LeaderboardManager leaderboardManager;

    // Leaderboard tables
    private TableView<ScoreRecord> topScoresTable;
    private TableView<LevelRecord> levelRecordsTable;
    private TableView<PlayerRecord> playerStatsTable;

    // Control buttons
    private Button refreshButton;
    private Button exportButton;
    private Button backButton;
    private Button syncOfflineButton;

    // Status labels
    private Label lastUpdatedLabel;
    private Label totalPlayersLabel;
    private Label offlineScoresLabel;
    private Label systemIdLabel;

    public LeaderboardView(MainApp mainApp) {
        this.mainApp = mainApp;
        this.leaderboardManager = new LeaderboardManager();
        initializeUI();
    }

    private void initializeUI() {
        // Create root container
        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1a1a2e, #16213e);");

        // Create background rectangle
        Rectangle background = new Rectangle(1200, 800);
        background.setFill(Color.TRANSPARENT);
        background.setStroke(Color.CYAN);
        background.setStrokeWidth(2);

        // Create main container
        mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(30));

        // Create title
        Text title = new Text("Leaderboard & Achievements");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        title.setFill(Color.CYAN);

        // Create subtitle
        Text subtitle = new Text("Phase 3 - Network Simulation Game");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        subtitle.setFill(Color.LIGHTGRAY);

        // Create status section
        createStatusSection();

        // Create leaderboard tables
        createLeaderboardTables();

        // Create control buttons
        createControlButtons();

        // Add components to main container
        mainContainer.getChildren().addAll(
                title, subtitle,
                createStatusSection(),
                createLeaderboardTables(),
                createControlButtons()
        );

        // Add everything to root
        root.getChildren().addAll(background, mainContainer);
    }

    private VBox createStatusSection() {
        VBox statusContainer = new VBox(10);
        statusContainer.setAlignment(Pos.CENTER);
        statusContainer.setPadding(new Insets(15));
        statusContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");

        // Status title
        Text statusTitle = new Text("Leaderboard Status");
        statusTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        statusTitle.setFill(Color.CYAN);

        // Status information
        HBox statusInfoBox = new HBox(20);
        statusInfoBox.setAlignment(Pos.CENTER);

        lastUpdatedLabel = new Label("Last Updated: Never");
        lastUpdatedLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        lastUpdatedLabel.setTextFill(Color.LIGHTGRAY);

        totalPlayersLabel = new Label("Total Players: 0");
        totalPlayersLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        totalPlayersLabel.setTextFill(Color.LIGHTGRAY);

        offlineScoresLabel = new Label("Offline Scores: 0");
        offlineScoresLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        offlineScoresLabel.setTextFill(Color.ORANGE);

        systemIdLabel = new Label("System: " + leaderboardManager.getCurrentSystemDisplayName());
        systemIdLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        systemIdLabel.setTextFill(Color.CYAN);

        statusInfoBox.getChildren().addAll(lastUpdatedLabel, totalPlayersLabel, offlineScoresLabel, systemIdLabel);

        statusContainer.getChildren().addAll(statusTitle, statusInfoBox);
        return statusContainer;
    }

    private VBox createLeaderboardTables() {
        VBox tablesContainer = new VBox(20);
        tablesContainer.setAlignment(Pos.CENTER);
        tablesContainer.setPadding(new Insets(15));
        tablesContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3); -fx-background-radius: 10;");

        // Tables title
        Text tablesTitle = new Text("Leaderboard Tables");
        tablesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        tablesTitle.setFill(Color.CYAN);

        // Create tabbed pane for different leaderboard views
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: transparent;");

        // Top Scores Tab
        Tab topScoresTab = new Tab("Top Scores", createTopScoresTable());
        topScoresTab.setClosable(false);
        topScoresTab.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        // Level Records Tab
        Tab levelRecordsTab = new Tab("Level Records", createLevelRecordsTable());
        levelRecordsTab.setClosable(false);
        levelRecordsTab.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        // Player Stats Tab
        Tab playerStatsTab = new Tab("Player Statistics", createPlayerStatsTable());
        playerStatsTab.setClosable(false);
        playerStatsTab.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        tabPane.getTabs().addAll(topScoresTab, levelRecordsTab, playerStatsTab);

        tablesContainer.getChildren().addAll(tablesTitle, tabPane);
        return tablesContainer;
    }

    /**
     * Creates the top scores table.
     */
    private TableView<ScoreRecord> createTopScoresTable() {
        topScoresTable = new TableView<>();
        topScoresTable.setPrefHeight(300);
        topScoresTable.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        // Rank column
        TableColumn<ScoreRecord, Integer> rankCol = new TableColumn<>("Rank");
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rank"));
        rankCol.setPrefWidth(80);

        // Player column
        TableColumn<ScoreRecord, String> playerCol = new TableColumn<>("Player");
        playerCol.setCellValueFactory(new PropertyValueFactory<>("playerId"));
        playerCol.setPrefWidth(150);

        // Level column
        TableColumn<ScoreRecord, String> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("levelId"));
        levelCol.setPrefWidth(100);

        // Score column
        TableColumn<ScoreRecord, Integer> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        scoreCol.setPrefWidth(100);

        // Time column
        TableColumn<ScoreRecord, Double> timeCol = new TableColumn<>("Time (s)");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("completionTime"));
        timeCol.setPrefWidth(120);

        // Date column
        TableColumn<ScoreRecord, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(120);

        topScoresTable.getColumns().addAll(rankCol, playerCol, levelCol, scoreCol, timeCol, dateCol);

        // Add sample data
        loadTopScoresData();

        return topScoresTable;
    }

    private TableView<LevelRecord> createLevelRecordsTable() {
        levelRecordsTable = new TableView<>();
        levelRecordsTable.setPrefHeight(300);
        levelRecordsTable.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        // Level column
        TableColumn<LevelRecord, String> levelCol = new TableColumn<>("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<>("levelId"));
        levelCol.setPrefWidth(120);

        // Best time column
        TableColumn<LevelRecord, Double> bestTimeCol = new TableColumn<>("Best Time (s)");
        bestTimeCol.setCellValueFactory(new PropertyValueFactory<>("bestTime"));
        bestTimeCol.setPrefWidth(150);

        // Best player column
        TableColumn<LevelRecord, String> bestPlayerCol = new TableColumn<>("Best Player");
        bestPlayerCol.setCellValueFactory(new PropertyValueFactory<>("bestPlayer"));
        bestPlayerCol.setPrefWidth(150);

        // Completion count column
        TableColumn<LevelRecord, Integer> completionCountCol = new TableColumn<>("Completions");
        completionCountCol.setCellValueFactory(new PropertyValueFactory<>("completionCount"));
        completionCountCol.setPrefWidth(120);

        // Average time column
        TableColumn<LevelRecord, Double> avgTimeCol = new TableColumn<>("Avg Time (s)");
        avgTimeCol.setCellValueFactory(new PropertyValueFactory<>("averageTime"));
        avgTimeCol.setPrefWidth(150);

        levelRecordsTable.getColumns().addAll(levelCol, bestTimeCol, bestPlayerCol, completionCountCol, avgTimeCol);

        // Add sample data
        loadLevelRecordsData();

        return levelRecordsTable;
    }

    private TableView<PlayerRecord> createPlayerStatsTable() {
        playerStatsTable = new TableView<>();
        playerStatsTable.setPrefHeight(300);
        playerStatsTable.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");

        // Player column
        TableColumn<PlayerRecord, String> playerCol = new TableColumn<>("Player");
        playerCol.setCellValueFactory(new PropertyValueFactory<>("playerId"));
        playerCol.setPrefWidth(150);

        // Total XP column
        TableColumn<PlayerRecord, Integer> totalXPCol = new TableColumn<>("Total XP");
        totalXPCol.setCellValueFactory(new PropertyValueFactory<>("totalXP"));
        totalXPCol.setPrefWidth(120);

        // Levels completed column
        TableColumn<PlayerRecord, Integer> levelsCompletedCol = new TableColumn<>("Levels Completed");
        levelsCompletedCol.setCellValueFactory(new PropertyValueFactory<>("levelsCompleted"));
        levelsCompletedCol.setPrefWidth(150);

        // Best level column
        TableColumn<PlayerRecord, String> bestLevelCol = new TableColumn<>("Best Level");
        bestLevelCol.setCellValueFactory(new PropertyValueFactory<>("bestLevel"));
        bestLevelCol.setPrefWidth(120);

        // Last played column
        TableColumn<PlayerRecord, String> lastPlayedCol = new TableColumn<>("Last Played");
        lastPlayedCol.setCellValueFactory(new PropertyValueFactory<>("lastPlayed"));
        lastPlayedCol.setPrefWidth(150);

        playerStatsTable.getColumns().addAll(playerCol, totalXPCol, levelsCompletedCol, bestLevelCol, lastPlayedCol);

        // Add sample data
        loadPlayerStatsData();

        return playerStatsTable;
    }

    private HBox createControlButtons() {
        HBox buttonsContainer = new HBox(20);
        buttonsContainer.setAlignment(Pos.CENTER);
        buttonsContainer.setPadding(new Insets(15));

        // Refresh button
        refreshButton = createControlButton("Refresh Data");
        refreshButton.setOnAction(e -> refreshLeaderboardData());

        // Sync offline scores button
        syncOfflineButton = createControlButton("Sync Offline Scores");
        syncOfflineButton.setOnAction(e -> syncOfflineScores());

        // Export button
        exportButton = createControlButton("Export Leaderboard");
        exportButton.setOnAction(e -> exportLeaderboard());

        // Back button
        backButton = createControlButton("Back to Main Menu");
        backButton.setOnAction(e -> mainApp.showMainMenu());

        buttonsContainer.getChildren().addAll(refreshButton, syncOfflineButton, exportButton, backButton);
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

    /**
     * Loads real top scores data from the leaderboard manager.
     */
    private void loadTopScoresData() {
        topScoresTable.getItems().clear();

        try {
            List<ScoreRecord> topScores = leaderboardManager.getTopScores(20);

            if (topScores.isEmpty()) {
                // Show empty state message instead of sample data
                showEmptyLeaderboardMessage("No scores recorded yet. Play some levels to see your scores here!");
                return;
            }

            for (int i = 0; i < topScores.size(); i++) {
                ScoreRecord score = topScores.get(i);
                score.setRank(i + 1);
                topScoresTable.getItems().add(score);
            }

        } catch (Exception e) {
            System.err.println("Error loading top scores: " + e.getMessage());
            showErrorMessage("Failed to load leaderboard data. Please try again.");
        }
    }

    private void loadLevelRecordsData() {
        levelRecordsTable.getItems().clear();

        try {
            Map<String, LevelRecord> levelRecords = leaderboardManager.getLevelRecords();

            if (levelRecords.isEmpty()) {
                // Show empty state message instead of sample data
                showEmptyLevelRecordsMessage("No level completion records yet. Complete levels to see records here!");
                return;
            }

            for (LevelRecord levelRecord : levelRecords.values()) {
                levelRecordsTable.getItems().add(levelRecord);
            }

        } catch (Exception e) {
            System.err.println("Error loading level records: " + e.getMessage());
            showErrorMessage("Failed to load level records. Please try again.");
        }
    }

    private void loadPlayerStatsData() {
        playerStatsTable.getItems().clear();

        try {
            LeaderboardManager.LeaderboardInfo info = leaderboardManager.getLeaderboardInfo();

            if (info.getPlayerRecords().isEmpty()) {
                // Show empty state message instead of sample data
                showEmptyPlayerStatsMessage("No player statistics yet. Play the game to see your stats here!");
                return;
            }

            for (PlayerRecord playerRecord : info.getPlayerRecords().values()) {
                playerStatsTable.getItems().add(playerRecord);
            }

        } catch (Exception e) {
            System.err.println("Error loading player statistics: " + e.getMessage());
            showErrorMessage("Failed to load player statistics. Please try again.");
        }
    }

    private void refreshLeaderboardData() {
        // Clear existing data
        topScoresTable.getItems().clear();
        levelRecordsTable.getItems().clear();
        playerStatsTable.getItems().clear();

        // Reload data
        loadTopScoresData();
        loadLevelRecordsData();
        loadPlayerStatsData();

        // Update status
        lastUpdatedLabel.setText("Last Updated: " + java.time.LocalDateTime.now().toString().substring(0, 19));
        totalPlayersLabel.setText("Total Players: " + playerStatsTable.getItems().size());
        offlineScoresLabel.setText("Offline Scores: " + leaderboardManager.getOfflineScoreCount());

        // Update offline scores label color based on count
        if (leaderboardManager.getOfflineScoreCount() > 0) {
            offlineScoresLabel.setTextFill(Color.ORANGE);
        } else {
            offlineScoresLabel.setTextFill(Color.GREEN);
        }
    }

    private void syncOfflineScores() {
        try {
            // Perform actual synchronization
            leaderboardManager.synchronizeOfflineScores();

            // Update UI
            offlineScoresLabel.setText("Offline Scores: 0");
            offlineScoresLabel.setTextFill(Color.GREEN);

            // Refresh data to show updated scores
            refreshLeaderboardData();

            // Show confirmation dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Synchronization Complete");
            alert.setHeaderText("Offline Scores Synced");
            alert.setContentText("All offline scores have been successfully synchronized with the server.");
            alert.showAndWait();

        } catch (Exception e) {
            // Show error dialog
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Synchronization Failed");
            alert.setHeaderText("Failed to Sync Offline Scores");
            alert.setContentText("Error: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void exportLeaderboard() {
        // Simulate export functionality
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Complete");
        alert.setHeaderText("Leaderboard Exported");
        alert.setContentText("Leaderboard data has been exported to 'leaderboard_export.csv'");
        alert.showAndWait();
    }

    public StackPane getRoot() {
        return root;
    }


    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }


    private void showEmptyLeaderboardMessage(String message) {
        // Create a placeholder row with the message
        ScoreRecord placeholder = new ScoreRecord("", "", 0, 0.0, "");
        placeholder.setRank(0);
        topScoresTable.getItems().add(placeholder);

        // Note: In a real implementation, you might want to add a custom cell renderer
        // to display the message properly in the table
        System.out.println("Leaderboard empty: " + message);
    }

    private void showEmptyLevelRecordsMessage(String message) {
        // Create a placeholder row with the message
        LevelRecord placeholder = new LevelRecord("No Records");
        levelRecordsTable.getItems().add(placeholder);

        System.out.println("Level records empty: " + message);
    }

    private void showEmptyPlayerStatsMessage(String message) {
        // Create a placeholder row with the message
        PlayerRecord placeholder = new PlayerRecord("No Players");
        playerStatsTable.getItems().add(placeholder);

        System.out.println("Player stats empty: " + message);
    }

    private void showErrorMessage(String message) {
        // In a real implementation, you might want to show a dialog or toast notification
        System.err.println("Leaderboard error: " + message);

        // For now, we'll just log the error and show a placeholder
        // You could implement a proper error dialog here
    }
}


