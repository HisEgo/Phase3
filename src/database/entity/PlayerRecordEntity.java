package database.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "player_records")
public class PlayerRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_record_id")
    private Long playerRecordId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserProfileEntity userProfile;

    @Column(name = "total_games_played")
    private int totalGamesPlayed;

    @Column(name = "total_multiplayer_games")
    private int totalMultiplayerGames;

    @Column(name = "total_singleplayer_games")
    private int totalSingleplayerGames;

    @Column(name = "total_wins")
    private int totalWins;

    @Column(name = "total_losses")
    private int totalLosses;

    @Column(name = "win_rate")
    private double winRate;

    @Column(name = "total_play_time")
    private long totalPlayTime; // in seconds

    @Column(name = "average_game_duration")
    private double averageGameDuration;

    @Column(name = "longest_win_streak")
    private int longestWinStreak;

    @Column(name = "current_win_streak")
    private int currentWinStreak;

    @Column(name = "total_packets_delivered")
    private int totalPacketsDelivered;

    @Column(name = "total_packets_failed")
    private int totalPacketsFailed;

    @Column(name = "overall_accuracy")
    private double overallAccuracy;

    @Column(name = "favorite_level", length = 50)
    private String favoriteLevel;

    @Column(name = "most_used_ability", length = 100)
    private String mostUsedAbility;

    @Column(name = "rank_position")
    private int rankPosition;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // Constructors
    public PlayerRecordEntity() {
        this.totalGamesPlayed = 0;
        this.totalMultiplayerGames = 0;
        this.totalSingleplayerGames = 0;
        this.totalWins = 0;
        this.totalLosses = 0;
        this.longestWinStreak = 0;
        this.currentWinStreak = 0;
        this.totalPacketsDelivered = 0;
        this.totalPacketsFailed = 0;
        this.lastUpdated = LocalDateTime.now();
    }

    public PlayerRecordEntity(UserProfileEntity userProfile) {
        this();
        this.userProfile = userProfile;
    }

    // Getters and Setters
    public Long getPlayerRecordId() {
        return playerRecordId;
    }

    public void setPlayerRecordId(Long playerRecordId) {
        this.playerRecordId = playerRecordId;
    }

    public UserProfileEntity getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfileEntity userProfile) {
        this.userProfile = userProfile;
    }

    public int getTotalGamesPlayed() {
        return totalGamesPlayed;
    }

    public void setTotalGamesPlayed(int totalGamesPlayed) {
        this.totalGamesPlayed = totalGamesPlayed;
    }

    public int getTotalMultiplayerGames() {
        return totalMultiplayerGames;
    }

    public void setTotalMultiplayerGames(int totalMultiplayerGames) {
        this.totalMultiplayerGames = totalMultiplayerGames;
    }

    public int getTotalSingleplayerGames() {
        return totalSingleplayerGames;
    }

    public void setTotalSingleplayerGames(int totalSingleplayerGames) {
        this.totalSingleplayerGames = totalSingleplayerGames;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void setTotalLosses(int totalLosses) {
        this.totalLosses = totalLosses;
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }

    public long getTotalPlayTime() {
        return totalPlayTime;
    }

    public void setTotalPlayTime(long totalPlayTime) {
        this.totalPlayTime = totalPlayTime;
    }

    public double getAverageGameDuration() {
        return averageGameDuration;
    }

    public void setAverageGameDuration(double averageGameDuration) {
        this.averageGameDuration = averageGameDuration;
    }

    public int getLongestWinStreak() {
        return longestWinStreak;
    }

    public void setLongestWinStreak(int longestWinStreak) {
        this.longestWinStreak = longestWinStreak;
    }

    public int getCurrentWinStreak() {
        return currentWinStreak;
    }

    public void setCurrentWinStreak(int currentWinStreak) {
        this.currentWinStreak = currentWinStreak;
    }

    public int getTotalPacketsDelivered() {
        return totalPacketsDelivered;
    }

    public void setTotalPacketsDelivered(int totalPacketsDelivered) {
        this.totalPacketsDelivered = totalPacketsDelivered;
    }

    public int getTotalPacketsFailed() {
        return totalPacketsFailed;
    }

    public void setTotalPacketsFailed(int totalPacketsFailed) {
        this.totalPacketsFailed = totalPacketsFailed;
    }

    public double getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(double overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public String getFavoriteLevel() {
        return favoriteLevel;
    }

    public void setFavoriteLevel(String favoriteLevel) {
        this.favoriteLevel = favoriteLevel;
    }

    public String getMostUsedAbility() {
        return mostUsedAbility;
    }

    public void setMostUsedAbility(String mostUsedAbility) {
        this.mostUsedAbility = mostUsedAbility;
    }

    public int getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(int rankPosition) {
        this.rankPosition = rankPosition;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Helper methods
    public void recordGameWin(boolean isMultiplayer, long gameDuration) {
        totalGamesPlayed++;
        totalWins++;
        currentWinStreak++;

        if (currentWinStreak > longestWinStreak) {
            longestWinStreak = currentWinStreak;
        }

        if (isMultiplayer) {
            totalMultiplayerGames++;
        } else {
            totalSingleplayerGames++;
        }

        totalPlayTime += gameDuration;
        updateStatistics();
    }

    public void recordGameLoss(boolean isMultiplayer, long gameDuration) {
        totalGamesPlayed++;
        totalLosses++;
        currentWinStreak = 0;

        if (isMultiplayer) {
            totalMultiplayerGames++;
        } else {
            totalSingleplayerGames++;
        }

        totalPlayTime += gameDuration;
        updateStatistics();
    }

    public void recordPacketDelivery(boolean success) {
        if (success) {
            totalPacketsDelivered++;
        } else {
            totalPacketsFailed++;
        }
        updateStatistics();
    }

    private void updateStatistics() {
        // Update win rate
        if (totalGamesPlayed > 0) {
            winRate = (double) totalWins / totalGamesPlayed * 100.0;
        }

        // Update average game duration
        if (totalGamesPlayed > 0) {
            averageGameDuration = (double) totalPlayTime / totalGamesPlayed;
        }

        // Update overall accuracy
        int totalPackets = totalPacketsDelivered + totalPacketsFailed;
        if (totalPackets > 0) {
            overallAccuracy = (double) totalPacketsDelivered / totalPackets * 100.0;
        }

        lastUpdated = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "PlayerRecordEntity{" +
                "playerRecordId=" + playerRecordId +
                ", totalGamesPlayed=" + totalGamesPlayed +
                ", totalWins=" + totalWins +
                ", totalLosses=" + totalLosses +
                ", winRate=" + winRate +
                ", currentWinStreak=" + currentWinStreak +
                ", overallAccuracy=" + overallAccuracy +
                '}';
    }
}


