package database.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "level_records")
public class LevelRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "level_record_id")
    private Long levelRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfileEntity userProfile;

    @Column(name = "level_id", length = 50, nullable = false)
    private String levelId;

    @Column(name = "level_name", length = 100)
    private String levelName;

    @Column(name = "best_score")
    private int bestScore;

    @Column(name = "best_completion_time")
    private double bestCompletionTime;

    @Column(name = "total_completions")
    private int totalCompletions;

    @Column(name = "total_attempts")
    private int totalAttempts;

    @Column(name = "success_rate")
    private double successRate;

    @Column(name = "average_score")
    private double averageScore;

    @Column(name = "average_completion_time")
    private double averageCompletionTime;

    @Column(name = "first_completion")
    private LocalDateTime firstCompletion;

    @Column(name = "last_completion")
    private LocalDateTime lastCompletion;

    @Column(name = "is_unlocked")
    private boolean isUnlocked;

    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Column(name = "stars_earned")
    private int starsEarned;

    // Constructors
    public LevelRecordEntity() {
        this.totalCompletions = 0;
        this.totalAttempts = 0;
        this.isUnlocked = false;
        this.starsEarned = 0;
    }

    public LevelRecordEntity(String levelId, String levelName) {
        this();
        this.levelId = levelId;
        this.levelName = levelName;
    }

    // Getters and Setters
    public Long getLevelRecordId() {
        return levelRecordId;
    }

    public void setLevelRecordId(Long levelRecordId) {
        this.levelRecordId = levelRecordId;
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

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public int getBestScore() {
        return bestScore;
    }

    public void setBestScore(int bestScore) {
        this.bestScore = bestScore;
    }

    public double getBestCompletionTime() {
        return bestCompletionTime;
    }

    public void setBestCompletionTime(double bestCompletionTime) {
        this.bestCompletionTime = bestCompletionTime;
    }

    public int getTotalCompletions() {
        return totalCompletions;
    }

    public void setTotalCompletions(int totalCompletions) {
        this.totalCompletions = totalCompletions;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public void setTotalAttempts(int totalAttempts) {
        this.totalAttempts = totalAttempts;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public double getAverageCompletionTime() {
        return averageCompletionTime;
    }

    public void setAverageCompletionTime(double averageCompletionTime) {
        this.averageCompletionTime = averageCompletionTime;
    }

    public LocalDateTime getFirstCompletion() {
        return firstCompletion;
    }

    public void setFirstCompletion(LocalDateTime firstCompletion) {
        this.firstCompletion = firstCompletion;
    }

    public LocalDateTime getLastCompletion() {
        return lastCompletion;
    }

    public void setLastCompletion(LocalDateTime lastCompletion) {
        this.lastCompletion = lastCompletion;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getStarsEarned() {
        return starsEarned;
    }

    public void setStarsEarned(int starsEarned) {
        this.starsEarned = starsEarned;
    }

    // Helper methods
    public void recordCompletion(int score, double completionTime) {
        totalCompletions++;
        totalAttempts++;

        if (score > bestScore) {
            bestScore = score;
        }

        if (bestCompletionTime == 0 || completionTime < bestCompletionTime) {
            bestCompletionTime = completionTime;
        }

        if (firstCompletion == null) {
            firstCompletion = LocalDateTime.now();
        }
        lastCompletion = LocalDateTime.now();

        // Update success rate
        successRate = (double) totalCompletions / totalAttempts * 100.0;
    }

    public void recordAttempt() {
        totalAttempts++;
        successRate = (double) totalCompletions / totalAttempts * 100.0;
    }

    @Override
    public String toString() {
        return "LevelRecordEntity{" +
                "levelRecordId=" + levelRecordId +
                ", levelId='" + levelId + '\'' +
                ", levelName='" + levelName + '\'' +
                ", bestScore=" + bestScore +
                ", totalCompletions=" + totalCompletions +
                ", successRate=" + successRate +
                ", isUnlocked=" + isUnlocked +
                '}';
    }
}


