package database.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "multiplayer_sessions")
public class MultiplayerSessionEntity {

    @Id
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", nullable = false)
    private UserProfileEntity player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id", nullable = false)
    private UserProfileEntity player2;

    @Column(name = "session_status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionStatus sessionStatus;

    @Column(name = "game_mode", length = 50)
    private String gameMode;

    @Column(name = "level_id", length = 50)
    private String levelId;

    @Column(name = "player1_score")
    private int player1Score;

    @Column(name = "player2_score")
    private int player2Score;

    @Column(name = "winner_id", length = 50)
    private String winnerId;

    @Column(name = "game_duration")
    private long gameDuration; // in seconds

    @Column(name = "setup_phase_duration")
    private long setupPhaseDuration; // in seconds

    @Column(name = "game_phase_duration")
    private long gamePhaseDuration; // in seconds

    @Column(name = "total_packets_sent")
    private int totalPacketsSent;

    @Column(name = "total_packets_delivered")
    private int totalPacketsDelivered;

    @Column(name = "network_efficiency")
    private double networkEfficiency;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "multiplayerSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GameSessionEntity> gameSessions = new ArrayList<>();

    // Constructors
    public MultiplayerSessionEntity() {
        this.createdAt = LocalDateTime.now();
        this.sessionStatus = SessionStatus.CREATED;
        this.player1Score = 0;
        this.player2Score = 0;
        this.totalPacketsSent = 0;
        this.totalPacketsDelivered = 0;
    }

    public MultiplayerSessionEntity(String sessionId, UserProfileEntity player1, UserProfileEntity player2) {
        this();
        this.sessionId = sessionId;
        this.player1 = player1;
        this.player2 = player2;
    }

    // Enums
    public enum SessionStatus {
        CREATED, WAITING, IN_PROGRESS, COMPLETED, ABANDONED, ERROR
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public UserProfileEntity getPlayer1() {
        return player1;
    }

    public void setPlayer1(UserProfileEntity player1) {
        this.player1 = player1;
    }

    public UserProfileEntity getPlayer2() {
        return player2;
    }

    public void setPlayer2(UserProfileEntity player2) {
        this.player2 = player2;
    }

    public SessionStatus getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(SessionStatus sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public String getLevelId() {
        return levelId;
    }

    public void setLevelId(String levelId) {
        this.levelId = levelId;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(int player1Score) {
        this.player1Score = player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(int player2Score) {
        this.player2Score = player2Score;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public long getGameDuration() {
        return gameDuration;
    }

    public void setGameDuration(long gameDuration) {
        this.gameDuration = gameDuration;
    }

    public long getSetupPhaseDuration() {
        return setupPhaseDuration;
    }

    public void setSetupPhaseDuration(long setupPhaseDuration) {
        this.setupPhaseDuration = setupPhaseDuration;
    }

    public long getGamePhaseDuration() {
        return gamePhaseDuration;
    }

    public void setGamePhaseDuration(long gamePhaseDuration) {
        this.gamePhaseDuration = gamePhaseDuration;
    }

    public int getTotalPacketsSent() {
        return totalPacketsSent;
    }

    public void setTotalPacketsSent(int totalPacketsSent) {
        this.totalPacketsSent = totalPacketsSent;
    }

    public int getTotalPacketsDelivered() {
        return totalPacketsDelivered;
    }

    public void setTotalPacketsDelivered(int totalPacketsDelivered) {
        this.totalPacketsDelivered = totalPacketsDelivered;
    }

    public double getNetworkEfficiency() {
        return networkEfficiency;
    }

    public void setNetworkEfficiency(double networkEfficiency) {
        this.networkEfficiency = networkEfficiency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public List<GameSessionEntity> getGameSessions() {
        return gameSessions;
    }

    public void setGameSessions(List<GameSessionEntity> gameSessions) {
        this.gameSessions = gameSessions;
    }

    // Helper methods
    public void startSession() {
        this.sessionStatus = SessionStatus.IN_PROGRESS;
        this.startedAt = LocalDateTime.now();
    }

    public void endSession(String winnerId) {
        this.sessionStatus = SessionStatus.COMPLETED;
        this.endedAt = LocalDateTime.now();
        this.winnerId = winnerId;

        if (startedAt != null) {
            this.gameDuration = java.time.Duration.between(startedAt, endedAt).getSeconds();
        }

        // Calculate network efficiency
        if (totalPacketsSent > 0) {
            this.networkEfficiency = (double) totalPacketsDelivered / totalPacketsSent * 100.0;
        }
    }

    public void abandonSession() {
        this.sessionStatus = SessionStatus.ABANDONED;
        this.endedAt = LocalDateTime.now();

        if (startedAt != null) {
            this.gameDuration = java.time.Duration.between(startedAt, endedAt).getSeconds();
        }
    }

    public void addGameSession(GameSessionEntity gameSession) {
        gameSessions.add(gameSession);
        gameSession.setMultiplayerSession(this);
    }

    public UserProfileEntity getWinner() {
        if (winnerId != null) {
            if (winnerId.equals(player1.getUserId())) {
                return player1;
            } else if (winnerId.equals(player2.getUserId())) {
                return player2;
            }
        }
        return null;
    }

    public UserProfileEntity getLoser() {
        if (winnerId != null) {
            if (winnerId.equals(player1.getUserId())) {
                return player2;
            } else if (winnerId.equals(player2.getUserId())) {
                return player1;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "MultiplayerSessionEntity{" +
                "sessionId='" + sessionId + '\'' +
                ", sessionStatus=" + sessionStatus +
                ", player1Score=" + player1Score +
                ", player2Score=" + player2Score +
                ", winnerId='" + winnerId + '\'' +
                ", gameDuration=" + gameDuration +
                ", createdAt=" + createdAt +
                '}';
    }
}


