package leaderboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class LevelRecord {
    private String levelId;
    private String levelName;
    private Map<String, Double> playerCompletionTimes;
    private double bestTime;
    private String bestPlayer;
    private int totalCompletions;
    private double averageCompletionTime;

    public LevelRecord() {
        this.playerCompletionTimes = new ConcurrentHashMap<>();
        this.bestTime = Double.MAX_VALUE;
        this.totalCompletions = 0;
        this.averageCompletionTime = 0.0;
    }

    public LevelRecord(String levelId) {
        this();
        this.levelId = levelId;
        this.levelName = "Level " + levelId;
    }


    public void addCompletion(String playerId, double completionTime) {
        // Store player's completion time
        playerCompletionTimes.put(playerId, completionTime);

        // Update best time if this is better
        if (completionTime < bestTime) {
            bestTime = completionTime;
            bestPlayer = playerId;
        }

        // Update total completions
        totalCompletions++;

        // Update average completion time
        updateAverageCompletionTime();
    }


    private void updateAverageCompletionTime() {
        if (playerCompletionTimes.isEmpty()) {
            averageCompletionTime = 0.0;
            return;
        }

        double totalTime = playerCompletionTimes.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        averageCompletionTime = totalTime / playerCompletionTimes.size();
    }


    public double getBestTime() {
        return bestTime == Double.MAX_VALUE ? 0.0 : bestTime;
    }

    public String getBestPlayer() {
        return bestPlayer;
    }


    public int getTotalCompletions() {
        return totalCompletions;
    }


    public double getAverageCompletionTime() {
        return averageCompletionTime;
    }


    public Double getPlayerCompletionTime(String playerId) {
        return playerCompletionTimes.get(playerId);
    }


    public Map<String, Double> getAllPlayerCompletionTimes() {
        return new HashMap<>(playerCompletionTimes);
    }

    public int getPlayerRank(String playerId) {
        Double playerTime = playerCompletionTimes.get(playerId);
        if (playerTime == null) return -1;

        List<Map.Entry<String, Double>> sortedTimes = new ArrayList<>(playerCompletionTimes.entrySet());
        sortedTimes.sort(Map.Entry.comparingByValue());

        for (int i = 0; i < sortedTimes.size(); i++) {
            if (sortedTimes.get(i).getKey().equals(playerId)) {
                return i + 1;
            }
        }
        return -1;
    }

    // Getters and setters
    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public void setPlayerCompletionTimes(Map<String, Double> playerCompletionTimes) {
        this.playerCompletionTimes = new ConcurrentHashMap<>(playerCompletionTimes);
        updateAverageCompletionTime();
    }

    public void setBestTime(double bestTime) {
        this.bestTime = bestTime;
    }

    public void setBestPlayer(String bestPlayer) {
        this.bestPlayer = bestPlayer;
    }

    public void setTotalCompletions(int totalCompletions) {
        this.totalCompletions = totalCompletions;
    }

    public void setAverageCompletionTime(double averageCompletionTime) {
        this.averageCompletionTime = averageCompletionTime;
    }
}


