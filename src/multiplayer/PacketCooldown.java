package multiplayer;

import java.util.*;

public class PacketCooldown {
    private String packetType;
    private double cooldownDuration;
    private Map<String, Double> playerRemainingTimes;
    private Map<String, Boolean> playerActiveStates;

    public PacketCooldown() {
        this.playerRemainingTimes = new HashMap<>();
        this.playerActiveStates = new HashMap<>();
    }

    public PacketCooldown(String packetType, double cooldownDuration) {
        this();
        this.packetType = packetType;
        this.cooldownDuration = cooldownDuration;
    }

    public void activateForPlayer(String playerId) {
        playerActiveStates.put(playerId, true);
        playerRemainingTimes.put(playerId, cooldownDuration);
    }

    public void update() {
        update(1.0);
    }

    public void update(double divisorMultiplier) {
        for (String playerId : new ArrayList<>(playerActiveStates.keySet())) {
            if (playerActiveStates.get(playerId)) {
                Double remainingTime = playerRemainingTimes.get(playerId);
                if (remainingTime != null && remainingTime > 0) {
                    double step = 0.016;
                    if (divisorMultiplier > 0) {
                        step = step / divisorMultiplier;
                    }
                    remainingTime -= step; // Assuming ~60 FPS

                    if (remainingTime <= 0) {
                        remainingTime = 0.0;
                        playerActiveStates.put(playerId, false);
                    }

                    playerRemainingTimes.put(playerId, remainingTime);
                }
            }
        }
    }

    public boolean isExpiredForPlayer(String playerId) {
        Boolean isActive = playerActiveStates.get(playerId);
        Double remainingTime = playerRemainingTimes.get(playerId);
        return isActive == null || !isActive || remainingTime == null || remainingTime <= 0;
    }

    public double getRemainingTimeForPlayer(String playerId) {
        Double remainingTime = playerRemainingTimes.get(playerId);
        return remainingTime != null ? Math.max(0, remainingTime) : 0.0;
    }

    public double getProgressForPlayer(String playerId) {
        if (cooldownDuration <= 0) return 1.0;
        double remainingTime = getRemainingTimeForPlayer(playerId);
        return 1.0 - (remainingTime / cooldownDuration);
    }

    public void resetForPlayer(String playerId) {
        playerActiveStates.put(playerId, false);
        playerRemainingTimes.put(playerId, 0.0);
    }

    public void resetForAllPlayers() {
        for (String playerId : playerActiveStates.keySet()) {
            resetForPlayer(playerId);
        }
    }

    public void increaseCooldown(double percentage) {
        cooldownDuration *= (1.0 + percentage);

        // Also increase remaining times for active cooldowns
        for (String playerId : playerActiveStates.keySet()) {
            if (playerActiveStates.get(playerId)) {
                Double remainingTime = playerRemainingTimes.get(playerId);
                if (remainingTime != null) {
                    playerRemainingTimes.put(playerId, remainingTime * (1.0 + percentage));
                }
            }
        }
    }

    public List<String> getPlayersWithActiveCooldowns() {
        List<String> activePlayers = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : playerActiveStates.entrySet()) {
            if (entry.getValue()) {
                activePlayers.add(entry.getKey());
            }
        }
        return activePlayers;
    }

    public boolean hasAnyActiveCooldown() {
        return playerActiveStates.values().stream().anyMatch(Boolean::booleanValue);
    }

    // Getters and setters
    public String getPacketType() {
        return packetType;
    }

    public void setPacketType(String packetType) {
        this.packetType = packetType;
    }

    public double getCooldownDuration() {
        return cooldownDuration;
    }

    public void setCooldownDuration(double cooldownDuration) {
        this.cooldownDuration = cooldownDuration;
    }

    public Map<String, Double> getPlayerRemainingTimes() {
        return new HashMap<>(playerRemainingTimes);
    }

    public void setPlayerRemainingTimes(Map<String, Double> playerRemainingTimes) {
        this.playerRemainingTimes = new HashMap<>(playerRemainingTimes);
    }

    public Map<String, Boolean> getPlayerActiveStates() {
        return new HashMap<>(playerActiveStates);
    }

    public void setPlayerActiveStates(Map<String, Boolean> playerActiveStates) {
        this.playerActiveStates = new HashMap<>(playerActiveStates);
    }
}


