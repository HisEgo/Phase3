package multiplayer;

import java.util.*;

public class PlayerState {
    private String playerId;
    private String playerName;
    private int score;
    private int successfulDeliveries;
    private int failedDeliveries;
    private boolean isReady;
    private long lastActionTime;
    private List<String> activeAbilities;

    public PlayerState() {
        this.activeAbilities = new ArrayList<>();
        this.lastActionTime = System.currentTimeMillis();
    }

    public PlayerState(String playerId, String playerName) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
    }


    public void recordSuccessfulDelivery() {
        successfulDeliveries++;
        score += 10; // Base score for successful delivery
        lastActionTime = System.currentTimeMillis();
    }


    public void recordFailedDelivery() {
        failedDeliveries++;
        // Phase 3 requirement: Lost packets count as -1.5 against successful deliveries
        // This means we need to apply a penalty that effectively reduces the net score
        score = Math.max(0, score - 5); // Base penalty for failed delivery
        lastActionTime = System.currentTimeMillis();
    }

    public void recordPacketLoss() {
        failedDeliveries++;
        // Phase 3 requirement: Lost packets count as -1.5 against successful deliveries
        // This means we need to apply a penalty that effectively reduces the net score
        // We'll apply this as a score penalty that represents the -1.5 multiplier
        int penalty = (int) Math.round(1.5 * 10); // 1.5 * base success score (10)
        score = Math.max(0, score - penalty);
        lastActionTime = System.currentTimeMillis();
    }

    public void addBonusPoints(int points) {
        score += points;
        lastActionTime = System.currentTimeMillis();
    }

    public void activateAbility(String abilityId) {
        if (!activeAbilities.contains(abilityId)) {
            activeAbilities.add(abilityId);
        }
    }

    public void deactivateAbility(String abilityId) {
        activeAbilities.remove(abilityId);
    }

    public boolean hasActiveAbility(String abilityId) {
        return activeAbilities.contains(abilityId);
    }


    public double getSuccessRate() {
        int totalDeliveries = successfulDeliveries + failedDeliveries;
        if (totalDeliveries == 0) return 0.0;
        return (double) successfulDeliveries / totalDeliveries;
    }

    public int getNetScore() {
        return successfulDeliveries - failedDeliveries;
    }

    // Getters and setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getSuccessfulDeliveries() {
        return successfulDeliveries;
    }

    public void setSuccessfulDeliveries(int successfulDeliveries) {
        this.successfulDeliveries = successfulDeliveries;
    }

    public int getFailedDeliveries() {
        return failedDeliveries;
    }

    public void setFailedDeliveries(int failedDeliveries) {
        this.failedDeliveries = failedDeliveries;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public long getLastActionTime() {
        return lastActionTime;
    }

    public void setLastActionTime(long lastActionTime) {
        this.lastActionTime = lastActionTime;
    }

    public List<String> getActiveAbilities() {
        return new ArrayList<>(activeAbilities);
    }

    public void setActiveAbilities(List<String> activeAbilities) {
        this.activeAbilities = new ArrayList<>(activeAbilities);
    }
}


