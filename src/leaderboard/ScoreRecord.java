package leaderboard;

public class ScoreRecord {
    private String playerId;
    private int xpEarned;
    private long timestamp;
    private String levelId;
    private double completionTime;
    private String date;
    private int rank;
    private String hash; // For Data Integrity Validation
    private int score; // For JSON serialization compatibility

    public ScoreRecord() {}

    public ScoreRecord(String playerId, int xpEarned, long timestamp) {
        this.playerId = playerId;
        this.xpEarned = xpEarned;
        this.timestamp = timestamp;
        this.date = java.time.Instant.ofEpochMilli(timestamp).toString().substring(0, 10);
        this.score = xpEarned; // Initialize score field
    }

    public ScoreRecord(String playerId, String levelId, int xpEarned, double completionTime, String date) {
        this.playerId = playerId;
        this.levelId = levelId;
        this.xpEarned = xpEarned;
        this.completionTime = completionTime;
        this.date = date;
        this.timestamp = System.currentTimeMillis();
        this.hash = null; // Will be set by client or server
        this.score = xpEarned; // Initialize score field
    }

    // Getters and setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    // Convenience method for score display
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "ScoreRecord{playerId='" + playerId + "', xpEarned=" + xpEarned + ", timestamp=" + timestamp + "}";
    }
}


