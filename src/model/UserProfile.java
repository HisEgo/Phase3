package model;

import leaderboard.ScoreRecord;
import java.util.*;
import java.io.Serializable;
import java.lang.System;


public class UserProfile implements Serializable {
    private String userId;
    private String macAddress;
    private String username;
    private SquadDetails squadDetails;
    private int totalXP;
    private Set<String> unlockedAbilities;
    private List<ScoreRecord> scoreHistory;
    private long lastPlayed;
    private long lastUpdated;
    private long createdAt;

    public UserProfile(String userId, String macAddress) {
        this.userId = userId;
        this.macAddress = macAddress;
        this.username = "Player_" + userId.substring(userId.lastIndexOf("_") + 1);
        this.squadDetails = new SquadDetails();
        this.totalXP = 0;
        this.unlockedAbilities = new HashSet<>();
        this.scoreHistory = new ArrayList<>();
        this.lastPlayed = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();

        // Add some default abilities
        this.unlockedAbilities.add("Basic Movement");
        this.unlockedAbilities.add("Basic Attack");
    }


    public void updateUserData(UserData data) {
        if (data.getUsername() != null) {
            this.username = data.getUsername();
        }
        if (data.getSquadDetails() != null) {
            this.squadDetails = data.getSquadDetails();
        }
        if (data.getXpEarned() > 0) {
            this.totalXP += data.getXpEarned();
        }
        if (data.getNewAbilities() != null) {
            this.unlockedAbilities.addAll(Arrays.asList(data.getNewAbilities()));
        }
        if (data.getScoreRecord() != null) {
            this.scoreHistory.add(data.getScoreRecord());
        }

        this.lastPlayed = System.currentTimeMillis();
    }


    public void addScoreRecord(ScoreRecord score) {
        this.scoreHistory.add(score);
        this.totalXP += score.getXpEarned();
        this.lastPlayed = System.currentTimeMillis();
    }

    public void unlockAbility(String abilityName) {
        this.unlockedAbilities.add(abilityName);
        this.lastUpdated = System.currentTimeMillis();
    }


    public String getUserRank() {
        if (totalXP >= 10000) return "Master";
        if (totalXP >= 5000) return "Expert";
        if (totalXP >= 2000) return "Advanced";
        if (totalXP >= 500) return "Intermediate";
        return "Beginner";
    }


    public double getAverageScore() {
        if (scoreHistory.isEmpty()) return 0.0;

        double totalScore = scoreHistory.stream()
                .mapToDouble(ScoreRecord::getXpEarned)
                .sum();
        return totalScore / scoreHistory.size();
    }

    public int getBestScore() {
        return scoreHistory.stream()
                .mapToInt(ScoreRecord::getXpEarned)
                .max()
                .orElse(0);
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public SquadDetails getSquadDetails() { return squadDetails; }
    public void setSquadDetails(SquadDetails squadDetails) { this.squadDetails = squadDetails; }

    public int getTotalXP() { return totalXP; }
    public void setTotalXP(int totalXP) { this.totalXP = totalXP; }

    public Set<String> getUnlockedAbilities() { return unlockedAbilities; }
    public void setUnlockedAbilities(Set<String> unlockedAbilities) { this.unlockedAbilities = unlockedAbilities; }

    public List<ScoreRecord> getScoreHistory() { return scoreHistory; }
    public void setScoreHistory(List<ScoreRecord> scoreHistory) { this.scoreHistory = scoreHistory; }

    public long getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(long lastPlayed) { this.lastPlayed = lastPlayed; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}


