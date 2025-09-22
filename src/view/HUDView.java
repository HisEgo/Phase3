package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
// Animation imports removed - no toggle functionality
import controller.GameController;
import model.AbilityType;
import model.WireConnection;

import java.util.List;

public class HUDView {
    private GameController gameController;
    private VBox root;
    private Label coinsLabel;
    private Label wireLengthLabel;
    private Label packetLossLabel;
    private Label temporalProgressLabel;
    private VBox activeAbilitiesBox;
    // HUD is now always visible - removed toggle functionality

    public HUDView(GameController gameController) {
        this.gameController = gameController;
        initializeUI();

        // Debug: Print HUD creation status
        
    }

    private void initializeUI() {
        root = new VBox(10);
        root.setAlignment(Pos.BOTTOM_LEFT);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: transparent;");

        // Position the HUD in the bottom-left corner
        root.setLayoutX(20);
        // Bottom position will be calculated based on scene height
        root.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (root.getScene() != null) {
                double sceneHeight = root.getScene().getHeight();
                root.setLayoutY(sceneHeight - newBounds.getHeight() - 20);
            }
        });

        // Make sure HUD is visible by default
        root.setVisible(true);
        
        // Make HUD transparent to mouse events so wiring works, but allow slider interaction
        root.setMouseTransparent(true);

        // Game statistics
        coinsLabel = createStatLabel("Coins: 20");
        wireLengthLabel = createStatLabel("Wire Length: 0");
        packetLossLabel = createStatLabel("Packets (safe: 0  lost: 0)");
        temporalProgressLabel = createStatLabel("Time: 0s");

        // Mode indicator
        Label modeLabel = createStatLabel("Mode: Editing");
        modeLabel.setId("modeLabel");

        // Time slider removed - now handled in GameView
        
        // Time slider functionality moved to GameView

        // Active abilities section
        Label abilitiesTitle = new Label("Active Abilities:");
        abilitiesTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        abilitiesTitle.setTextFill(Color.CYAN);
        abilitiesTitle.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 5; -fx-padding: 5;");

        activeAbilitiesBox = new VBox(5);
        activeAbilitiesBox.setAlignment(Pos.TOP_LEFT);

        // HUD is now always visible - removed toggle button and indicator

        root.getChildren().addAll(
                coinsLabel, wireLengthLabel, packetLossLabel, temporalProgressLabel, modeLabel,
                abilitiesTitle, activeAbilitiesBox
        );
    }

    // Auto-hide timer removed - HUD is always visible

    // Hide HUD methods removed - HUD is always visible

    // Button style methods removed - no toggle button

    private Label createStatLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 5; -fx-padding: 5;");
        return label;
    }

    public void update() {
        // HUD is always visible and always updates

        // Update HUD labels

        // Update basic stats
        coinsLabel.setText("Coins: " + gameController.getGameState().getCoins());

        // Enhanced wire length display showing both remaining and used wire
        double remainingWire = gameController.getGameState().getRemainingWireLength();
        
        // Get smooth curve setting to calculate wire length correctly
        boolean useSmoothCurves = true; // Default to smooth curves
        Object setting = gameController.getGameState().getGameSettings().get("smoothWireCurves");
        if (setting instanceof Boolean) {
            useSmoothCurves = (Boolean) setting;
        }
        
        double totalUsedWire = gameController.getWiringController().getTotalWireLengthUsed(gameController.getGameState(), useSmoothCurves);

        // Show both remaining and total used wire length
        wireLengthLabel.setText(String.format("Wire: %.1f remaining, %.1f used", remainingWire, totalUsedWire));

        // Display packet counts instead of percentage
        int safePackets = gameController.getGameState().getTotalDeliveredPackets();
        int lostPackets = gameController.getGameState().getTotalLostPackets();
        packetLossLabel.setText(String.format("Packets (safe: %d  lost: %d)", safePackets, lostPackets));
        // Enhanced temporal progress display with visual feedback
        double currentTime = gameController.getGameState().getTemporalProgress();
        double maxTime = gameController.getGameState().getCurrentLevel() != null ?
                gameController.getGameState().getCurrentLevel().getLevelDuration() : 60.0;
        temporalProgressLabel.setText(String.format("Time: %.1fs / %.0fs", currentTime, maxTime));

        // Time slider functionality moved to GameView

        // Color-code based on time progress
        double timeProgress = currentTime / maxTime;
        if (timeProgress < 0.5) {
            temporalProgressLabel.setTextFill(Color.GREEN);
        } else if (timeProgress < 0.8) {
            temporalProgressLabel.setTextFill(Color.ORANGE);
        } else {
            temporalProgressLabel.setTextFill(Color.RED);
        }

        // Update mode indicator
        Label modeLabel = (Label) root.lookup("#modeLabel");
        if (modeLabel != null) {
            if (gameController.isEditingMode()) {
                modeLabel.setText("Mode: Editing (Press R to Run)");
                modeLabel.setTextFill(Color.GREEN);
            } else {
                modeLabel.setText("Mode: Simulation (Time: " + String.format("%.1f", currentTime) + "s/" + String.format("%.1f", maxTime) + "s)");
                modeLabel.setTextFill(Color.ORANGE);
            }
        }

        // Update active abilities
        updateActiveAbilities();
    }

    private void updateActiveAbilities() {
        activeAbilitiesBox.getChildren().clear();
        List<AbilityType> activeAbilities = gameController.getActiveAbilities();

        if (activeAbilities.isEmpty()) {
            Label noAbilitiesLabel = new Label("No active abilities");
            noAbilitiesLabel.setFont(Font.font("Arial", 10));
            noAbilitiesLabel.setTextFill(Color.GRAY);
            activeAbilitiesBox.getChildren().add(noAbilitiesLabel);
        } else {
            for (AbilityType ability : activeAbilities) {
                HBox abilityRow = new HBox(10);
                abilityRow.setAlignment(Pos.CENTER_LEFT);

                Label abilityLabel = new Label(ability.getDisplayName());
                abilityLabel.setFont(Font.font("Arial", 10));
                abilityLabel.setTextFill(Color.YELLOW);

                double cooldown = gameController.getAbilityCooldown(ability);
                if (cooldown > 0) {
                    Label cooldownLabel = new Label(String.format("%.1fs", cooldown));
                    cooldownLabel.setFont(Font.font("Arial", 8));
                    cooldownLabel.setTextFill(Color.RED);
                    abilityRow.getChildren().addAll(abilityLabel, cooldownLabel);
                } else {
                    abilityRow.getChildren().add(abilityLabel);
                }

                activeAbilitiesBox.getChildren().add(abilityRow);
            }
        }
    }

    // Toggle visibility methods removed - HUD is always visible

    // Show/Hide HUD methods removed - HUD is always visible

    public void forceShowAndUpdate() {
        // HUD is always visible, just ensure root is visible and force layout update
        root.setVisible(true);
        root.requestLayout();
        
    }

    public String getHUDStatus() {
        return String.format("HUD Status - shouldDisplay: %s, root.visible: %s, position: (%.1f, %.1f)",
                shouldDisplay(), root.isVisible(), root.getLayoutX(), root.getLayoutY());
    }

    public boolean isActuallyVisible() {
        return shouldDisplay() && root.getScene() != null &&
                root.getParent() != null && root.getBoundsInLocal().getWidth() > 0;
    }

    public String getComprehensiveHUDStatus() {
        return String.format(
                "HUD Debug - shouldDisplay: %s, root.visible: %s, inScene: %s, hasParent: %s, bounds: %s, position: (%.1f, %.1f)",
                shouldDisplay(), root.isVisible(),
                (root.getScene() != null), (root.getParent() != null),
                root.getBoundsInLocal(), root.getLayoutX(), root.getLayoutY()
        );
    }

    // Timer methods removed - no auto-hide timer

    public VBox getRoot() {
        return root;
    }

    // HUD indicator removed - HUD is always visible

    public boolean isVisible() {
        return true; // HUD is always visible
    }

    public boolean shouldDisplay() {
        return true; // HUD is always visible
    }

    public boolean isFunctionallyActive() {
        return true; // HUD is always functionally active
    }
}

