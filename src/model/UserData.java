package model;

import java.io.Serializable;
import leaderboard.ScoreRecord;
import java.lang.System;


public class UserData implements Serializable {
    private String username;
    private SquadDetails squadDetails;
    private int xpEarned;
    private String[] newAbilities;
    private ScoreRecord scoreRecord;
    private long timestamp;

    public UserData() {
        this.timestamp = System.currentTimeMillis();
    }

    public UserData(String username, int xpEarned) {
        this.username = username;
        this.xpEarned = xpEarned;
        this.timestamp = System.currentTimeMillis();
    }

    public UserData(String username, SquadDetails squadDetails, int xpEarned, String[] newAbilities, ScoreRecord scoreRecord) {
        this.username = username;
        this.squadDetails = squadDetails;
        this.xpEarned = xpEarned;
        this.newAbilities = newAbilities;
        this.scoreRecord = scoreRecord;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public SquadDetails getSquadDetails() { return squadDetails; }
    public void setSquadDetails(SquadDetails squadDetails) { this.squadDetails = squadDetails; }

    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }

    public String[] getNewAbilities() { return newAbilities; }
    public void setNewAbilities(String[] newAbilities) { this.newAbilities = newAbilities; }

    public ScoreRecord getScoreRecord() { return scoreRecord; }
    public void setScoreRecord(ScoreRecord scoreRecord) { this.scoreRecord = scoreRecord; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}


