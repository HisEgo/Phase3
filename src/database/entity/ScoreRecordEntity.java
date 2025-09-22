package database.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "score_records")
public class ScoreRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfileEntity userProfile;

    @Column(name = "level_id", length = 50, nullable = false)
    private String levelId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;

    @Column(name = "completion_time")
    private double completionTime;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "is_multiplayer")
    private boolean isMultiplayer;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Column(name = "packets_delivered")
    private int packetsDelivered;

    @Column(name = "packets_failed")
    private int packetsFailed;

    @Column(name = "accuracy_percentage")
    private double accuracyPercentage;

    // Constructors
    public ScoreRecordEntity() {
        this.timestamp = LocalDateTime.now();
    }

    public ScoreRecordEntity(String levelId, int score, int xpEarned, double completionTime) {
        this();
        this.levelId = levelId;
        this.score = score;
        this.xpEarned = xpEarned;
        this.completionTime = completionTime;
    }

    // Getters and Setters
    public Long getScoreId() {
        return scoreId;
    }

    public void setScoreId(Long scoreId) {
        this.scoreId = scoreId;
    }

    public UserProfileEntity getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfileEntity userProfile) {
        this.userProfile = userProfile;
    }

    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getXpEarned() {
        return xpEarned;
    }

    public void setXpEarned(int xpEarned) {
        this.xpEarned = xpEarned;
    }

    public double getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(double completionTime) {
        this.completionTime = completionTime;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isMultiplayer() {
        return isMultiplayer;
    }

    public void setMultiplayer(boolean multiplayer) {
        isMultiplayer = multiplayer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getPacketsDelivered() {
        return packetsDelivered;
    }

    public void setPacketsDelivered(int packetsDelivered) {
        this.packetsDelivered = packetsDelivered;
    }

    public int getPacketsFailed() {
        return packetsFailed;
    }

    public void setPacketsFailed(int packetsFailed) {
        this.packetsFailed = packetsFailed;
    }

    public double getAccuracyPercentage() {
        return accuracyPercentage;
    }

    public void setAccuracyPercentage(double accuracyPercentage) {
        this.accuracyPercentage = accuracyPercentage;
    }

    @Override
    public String toString() {
        return "ScoreRecordEntity{" +
                "scoreId=" + scoreId +
                ", levelId='" + levelId + '\'' +
                ", score=" + score +
                ", xpEarned=" + xpEarned +
                ", completionTime=" + completionTime +
                ", timestamp=" + timestamp +
                ", isMultiplayer=" + isMultiplayer +
                '}';
    }
}


