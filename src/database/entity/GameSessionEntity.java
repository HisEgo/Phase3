package database.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "game_sessions")
public class GameSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_session_id")
    private Long gameSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "multiplayer_session_id", nullable = false)
    private MultiplayerSessionEntity multiplayerSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserProfileEntity user;

    @Column(name = "session_type", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SessionType sessionType;

    @Column(name = "player_position")
    private int playerPosition; // 1 or 2

    @Column(name = "final_score")
    private int finalScore;

    @Column(name = "packets_sent")
    private int packetsSent;

    @Column(name = "packets_delivered")
    private int packetsDelivered;

    @Column(name = "packets_failed")
    private int packetsFailed;

    @Column(name = "accuracy_percentage")
    private double accuracyPercentage;

    @Column(name = "ammunition_used")
    private int ammunitionUsed;

    @Column(name = "systems_controlled")
    private int systemsControlled;

    @Column(name = "cooldown_violations")
    private int cooldownViolations;

    @Column(name = "network_efficiency")
    private double networkEfficiency;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration")
    private long duration; // in seconds

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SystemEntity> systems = new ArrayList<>();

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WireConnectionEntity> wireConnections = new ArrayList<>();

    // Constructors
    public GameSessionEntity() {
        this.startedAt = LocalDateTime.now();
        this.finalScore = 0;
        this.packetsSent = 0;
        this.packetsDelivered = 0;
        this.packetsFailed = 0;
        this.ammunitionUsed = 0;
        this.systemsControlled = 0;
        this.cooldownViolations = 0;
    }

    public GameSessionEntity(MultiplayerSessionEntity multiplayerSession, UserProfileEntity user, SessionType sessionType, int playerPosition) {
        this();
        this.multiplayerSession = multiplayerSession;
        this.user = user;
        this.sessionType = sessionType;
        this.playerPosition = playerPosition;
    }

    // Enums
    public enum SessionType {
        SETUP, GAME, COMPLETED
    }

    // Getters and Setters
    public Long getGameSessionId() {
        return gameSessionId;
    }

    public void setGameSessionId(Long gameSessionId) {
        this.gameSessionId = gameSessionId;
    }

    public MultiplayerSessionEntity getMultiplayerSession() {
        return multiplayerSession;
    }

    public void setMultiplayerSession(MultiplayerSessionEntity multiplayerSession) {
        this.multiplayerSession = multiplayerSession;
    }

    public UserProfileEntity getUser() {
        return user;
    }

    public void setUser(UserProfileEntity user) {
        this.user = user;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public int getPlayerPosition() {
        return playerPosition;
    }

    public void setPlayerPosition(int playerPosition) {
        this.playerPosition = playerPosition;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }

    public int getPacketsSent() {
        return packetsSent;
    }

    public void setPacketsSent(int packetsSent) {
        this.packetsSent = packetsSent;
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

    public int getAmmunitionUsed() {
        return ammunitionUsed;
    }

    public void setAmmunitionUsed(int ammunitionUsed) {
        this.ammunitionUsed = ammunitionUsed;
    }

    public int getSystemsControlled() {
        return systemsControlled;
    }

    public void setSystemsControlled(int systemsControlled) {
        this.systemsControlled = systemsControlled;
    }

    public int getCooldownViolations() {
        return cooldownViolations;
    }

    public void setCooldownViolations(int cooldownViolations) {
        this.cooldownViolations = cooldownViolations;
    }

    public double getNetworkEfficiency() {
        return networkEfficiency;
    }

    public void setNetworkEfficiency(double networkEfficiency) {
        this.networkEfficiency = networkEfficiency;
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

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public List<SystemEntity> getSystems() {
        return systems;
    }

    public void setSystems(List<SystemEntity> systems) {
        this.systems = systems;
    }

    public List<WireConnectionEntity> getWireConnections() {
        return wireConnections;
    }

    public void setWireConnections(List<WireConnectionEntity> wireConnections) {
        this.wireConnections = wireConnections;
    }

    // Helper methods
    public void endSession() {
        this.endedAt = LocalDateTime.now();
        this.sessionType = SessionType.COMPLETED;

        if (startedAt != null) {
            this.duration = java.time.Duration.between(startedAt, endedAt).getSeconds();
        }

        // Calculate accuracy percentage
        int totalPackets = packetsSent;
        if (totalPackets > 0) {
            this.accuracyPercentage = (double) packetsDelivered / totalPackets * 100.0;
        }

        // Calculate network efficiency
        if (packetsSent > 0) {
            this.networkEfficiency = (double) packetsDelivered / packetsSent * 100.0;
        }
    }

    public void recordPacketSent(boolean delivered) {
        packetsSent++;
        if (delivered) {
            packetsDelivered++;
        } else {
            packetsFailed++;
        }
    }

    public void recordAmmunitionUsed() {
        ammunitionUsed++;
    }

    public void recordCooldownViolation() {
        cooldownViolations++;
    }

    public void addSystem(SystemEntity system) {
        systems.add(system);
        system.setGameSession(this);
    }

    public void addWireConnection(WireConnectionEntity wireConnection) {
        wireConnections.add(wireConnection);
        wireConnection.setGameSession(this);
    }

    @Override
    public String toString() {
        return "GameSessionEntity{" +
                "gameSessionId=" + gameSessionId +
                ", sessionType=" + sessionType +
                ", playerPosition=" + playerPosition +
                ", finalScore=" + finalScore +
                ", packetsDelivered=" + packetsDelivered +
                ", accuracyPercentage=" + accuracyPercentage +
                ", duration=" + duration +
                '}';
    }
}


