package database.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "mac_address", length = 17, unique = true, nullable = false)
    private String macAddress;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "total_xp")
    private int totalXP;

    @Column(name = "user_rank", length = 50)
    private String userRank;

    @Column(name = "best_score")
    private int bestScore;

    @Column(name = "average_score")
    private double averageScore;

    @Column(name = "levels_completed")
    private int levelsCompleted;

    @Column(name = "average_completion_time")
    private double averageCompletionTime;

    @ElementCollection
    @CollectionTable(name = "user_abilities", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "ability")
    private List<String> unlockedAbilities = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ScoreRecordEntity> scoreHistory = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LevelRecordEntity> levelRecords = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Constructors
    public UserProfileEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserProfileEntity(String userId, String macAddress) {
        this();
        this.userId = userId;
        this.macAddress = macAddress;
    }

    // Lifecycle callbacks
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTotalXP() {
        return totalXP;
    }

    public void setTotalXP(int totalXP) {
        this.totalXP = totalXP;
    }

    public String getUserRank() {
        return userRank;
    }

    public void setUserRank(String userRank) {
        this.userRank = userRank;
    }

    public int getBestScore() {
        return bestScore;
    }

    public void setBestScore(int bestScore) {
        this.bestScore = bestScore;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public int getLevelsCompleted() {
        return levelsCompleted;
    }

    public void setLevelsCompleted(int levelsCompleted) {
        this.levelsCompleted = levelsCompleted;
    }

    public double getAverageCompletionTime() {
        return averageCompletionTime;
    }

    public void setAverageCompletionTime(double averageCompletionTime) {
        this.averageCompletionTime = averageCompletionTime;
    }

    public List<String> getUnlockedAbilities() {
        return unlockedAbilities;
    }

    public void setUnlockedAbilities(List<String> unlockedAbilities) {
        this.unlockedAbilities = unlockedAbilities;
    }

    public List<ScoreRecordEntity> getScoreHistory() {
        return scoreHistory;
    }

    public void setScoreHistory(List<ScoreRecordEntity> scoreHistory) {
        this.scoreHistory = scoreHistory;
    }

    public List<LevelRecordEntity> getLevelRecords() {
        return levelRecords;
    }

    public void setLevelRecords(List<LevelRecordEntity> levelRecords) {
        this.levelRecords = levelRecords;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    // Helper methods
    public void addScoreRecord(ScoreRecordEntity scoreRecord) {
        scoreHistory.add(scoreRecord);
        scoreRecord.setUserProfile(this);
    }

    public void addLevelRecord(LevelRecordEntity levelRecord) {
        levelRecords.add(levelRecord);
        levelRecord.setUserProfile(this);
    }

    public void addAbility(String ability) {
        if (!unlockedAbilities.contains(ability)) {
            unlockedAbilities.add(ability);
        }
    }

    @Override
    public String toString() {
        return "UserProfileEntity{" +
                "userId='" + userId + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", username='" + username + '\'' +
                ", totalXP=" + totalXP +
                ", userRank='" + userRank + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}


