package leaderboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRecord {
    private String playerId;
    private String playerName;
    private int totalXP;
    private Map<String, Double> levelCompletionTimes;
    private Map<String, Integer> levelXPEarned;
    private List<String> unlockedAbilities;
    private long firstPlayed;
    private long lastPlayed;
    private int levelsCompleted;
    private double averageCompletionTime;

    public PlayerRecord() {
        this.levelCompletionTimes = new ConcurrentHashMap<>();
        this.levelXPEarned = new ConcurrentHashMap<>();
        this.unlockedAbilities = new ArrayList<>();
        this.firstPlayed = System.currentTimeMillis();
        this.lastPlayed = System.currentTimeMillis();
        this.levelsCompleted = 0;
        this.averageCompletionTime = 0.0;
    }

    public PlayerRecord(String playerId) {
        this();
        this.playerId = playerId;
        this.playerName = "Player " + playerId;
    }


    public void addLevelCompletion(String levelId, double completionTime, int xpEarned) {
        // Update completion time if it's better than previous
        Double previousTime = levelCompletionTimes.get(levelId);
        boolean isNewLevel = (previousTime == null);
        if (isNewLevel || completionTime < previousTime) {
            levelCompletionTimes.put(levelId, completionTime);
        }

        // Update XP earned for this level
        levelXPEarned.put(levelId, xpEarned);

        // Update total XP
        totalXP += xpEarned;

        // Update levels completed count if this is a new level
        if (isNewLevel) {
            levelsCompleted++;
        }

        // Update average completion time
        updateAverageCompletionTime();

        // Update last played time
        lastPlayed = System.currentTimeMillis();
    }


    public Double getBestLevelTime(String levelId) {
        return levelCompletionTimes.get(levelId);
    }


    public Integer getLevelXP(String levelId) {
        return levelXPEarned.get(levelId);
    }


    public int getTotalXP() {
        return totalXP;
    }


    public int getLevelsCompleted() {
        return levelsCompleted;
    }


    public double getAverageCompletionTime() {
        return averageCompletionTime;
    }

    private void updateAverageCompletionTime() {
        if (levelCompletionTimes.isEmpty()) {
            averageCompletionTime = 0.0;
            return;
        }

        double totalTime = levelCompletionTimes.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        averageCompletionTime = totalTime / levelCompletionTimes.size();
    }


    public int getRank(List<PlayerRecord> allPlayers) {
        List<PlayerRecord> sortedPlayers = new ArrayList<>(allPlayers);
        sortedPlayers.sort((p1, p2) -> Integer.compare(p2.getTotalXP(), p1.getTotalXP()));

        for (int i = 0; i < sortedPlayers.size(); i++) {
            if (sortedPlayers.get(i).getPlayerId().equals(this.playerId)) {
                return i + 1;
            }
        }
        return -1; // Player not found
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

    public void setTotalXP(int totalXP) {
        this.totalXP = totalXP;
    }

    public Map<String, Double> getLevelCompletionTimes() {
        return new HashMap<>(levelCompletionTimes);
    }

    public void setLevelCompletionTimes(Map<String, Double> levelCompletionTimes) {
        this.levelCompletionTimes = new ConcurrentHashMap<>(levelCompletionTimes);
    }

    public Map<String, Integer> getLevelXPEarned() {
        return new HashMap<>(levelXPEarned);
    }

    public void setLevelXPEarned(Map<String, Integer> levelXPEarned) {
        this.levelXPEarned = new ConcurrentHashMap<>(levelXPEarned);
    }

    public List<String> getUnlockedAbilities() {
        return new ArrayList<>(unlockedAbilities);
    }

    public void setUnlockedAbilities(List<String> unlockedAbilities) {
        this.unlockedAbilities = new ArrayList<>(unlockedAbilities);
    }

    public long getFirstPlayed() {
        return firstPlayed;
    }

    public void setFirstPlayed(long firstPlayed) {
        this.firstPlayed = firstPlayed;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(long lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public void setLevelsCompleted(int levelsCompleted) {
        this.levelsCompleted = levelsCompleted;
    }

    public void setAverageCompletionTime(double averageCompletionTime) {
        this.averageCompletionTime = averageCompletionTime;
    }
}


