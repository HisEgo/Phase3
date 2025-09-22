package leaderboard;

public class OfflineScore {
    private String playerId;
    private String levelId;
    private double completionTime;
    private int xpEarned;
    private long timestamp;

    public OfflineScore() {}

    public OfflineScore(String playerId, String levelId, double completionTime, int xpEarned, long timestamp) {
        this.playerId = playerId;
        this.levelId = levelId;
        this.completionTime = completionTime;
        this.xpEarned = xpEarned;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public double getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(double completionTime) {
        this.completionTime = completionTime;
    }

    public int getXpEarned() {
        return xpEarned;
    }

    public void setXpEarned(int xpEarned) {
        this.xpEarned = xpEarned;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "OfflineScore{playerId='" + playerId + "', levelId='" + levelId +
                "', completionTime=" + completionTime + ", xpEarned=" + xpEarned +
                ", timestamp=" + timestamp + "}";
    }
}


