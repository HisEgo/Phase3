package multiplayer;

import java.util.*;

public class MultiplayerGameState {
    private boolean isSetupPhase;
    private boolean isGameStarted;
    private long remainingSetupTime;
    private PlayerState player1State;
    private PlayerState player2State;
    private Map<String, SystemInfo> controllableSystems;
    private Map<String, CooldownInfo> cooldowns;
    private long timestamp;

    public MultiplayerGameState() {
        this.timestamp = System.currentTimeMillis();
    }

    public MultiplayerGameState(boolean isSetupPhase, boolean isGameStarted, long remainingSetupTime,
                                PlayerState player1State, PlayerState player2State,
                                Map<String, SystemInfo> controllableSystems,
                                Map<String, CooldownInfo> cooldowns) {
        this();
        this.isSetupPhase = isSetupPhase;
        this.isGameStarted = isGameStarted;
        this.remainingSetupTime = remainingSetupTime;
        this.player1State = player1State;
        this.player2State = player2State;
        this.controllableSystems = controllableSystems;
        this.cooldowns = cooldowns;
    }

    // Getters and setters
    public boolean isSetupPhase() {
        return isSetupPhase;
    }

    public void setSetupPhase(boolean setupPhase) {
        isSetupPhase = setupPhase;
    }

    public boolean isGameStarted() {
        return isGameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        isGameStarted = gameStarted;
    }

    public long getRemainingSetupTime() {
        return remainingSetupTime;
    }

    public void setRemainingSetupTime(long remainingSetupTime) {
        this.remainingSetupTime = remainingSetupTime;
    }

    public PlayerState getPlayer1State() {
        return player1State;
    }

    public void setPlayer1State(PlayerState player1State) {
        this.player1State = player1State;
    }

    public PlayerState getPlayer2State() {
        return player2State;
    }

    public void setPlayer2State(PlayerState player2State) {
        this.player2State = player2State;
    }

    public Map<String, SystemInfo> getControllableSystems() {
        return controllableSystems;
    }

    public void setControllableSystems(Map<String, SystemInfo> controllableSystems) {
        this.controllableSystems = controllableSystems;
    }

    public Map<String, CooldownInfo> getCooldowns() {
        return cooldowns;
    }

    public void setCooldowns(Map<String, CooldownInfo> cooldowns) {
        this.cooldowns = cooldowns;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}


