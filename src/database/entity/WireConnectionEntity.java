package database.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wire_connections")
public class WireConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wire_connection_id")
    private Long wireConnectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSessionEntity gameSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_port_id", nullable = false)
    private PortEntity sourcePort;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_port_id", nullable = false)
    private PortEntity targetPort;

    @Column(name = "connection_type", length = 50)
    private String connectionType;

    @Column(name = "bandwidth")
    private double bandwidth;

    @Column(name = "latency")
    private double latency;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "packets_transmitted")
    private int packetsTransmitted;

    @Column(name = "packets_delivered")
    private int packetsDelivered;

    @Column(name = "packets_dropped")
    private int packetsDropped;

    @Column(name = "transmission_efficiency")
    private double transmissionEfficiency;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    // Constructors
    public WireConnectionEntity() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.packetsTransmitted = 0;
        this.packetsDelivered = 0;
        this.packetsDropped = 0;
        this.bandwidth = 1.0;
        this.latency = 0.0;
    }

    public WireConnectionEntity(PortEntity sourcePort, PortEntity targetPort) {
        this();
        this.sourcePort = sourcePort;
        this.targetPort = targetPort;
    }

    // Getters and Setters
    public Long getWireConnectionId() {
        return wireConnectionId;
    }

    public void setWireConnectionId(Long wireConnectionId) {
        this.wireConnectionId = wireConnectionId;
    }

    public GameSessionEntity getGameSession() {
        return gameSession;
    }

    public void setGameSession(GameSessionEntity gameSession) {
        this.gameSession = gameSession;
    }

    public PortEntity getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(PortEntity sourcePort) {
        this.sourcePort = sourcePort;
    }

    public PortEntity getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(PortEntity targetPort) {
        this.targetPort = targetPort;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getPacketsTransmitted() {
        return packetsTransmitted;
    }

    public void setPacketsTransmitted(int packetsTransmitted) {
        this.packetsTransmitted = packetsTransmitted;
    }

    public int getPacketsDelivered() {
        return packetsDelivered;
    }

    public void setPacketsDelivered(int packetsDelivered) {
        this.packetsDelivered = packetsDelivered;
    }

    public int getPacketsDropped() {
        return packetsDropped;
    }

    public void setPacketsDropped(int packetsDropped) {
        this.packetsDropped = packetsDropped;
    }

    public double getTransmissionEfficiency() {
        return transmissionEfficiency;
    }

    public void setTransmissionEfficiency(double transmissionEfficiency) {
        this.transmissionEfficiency = transmissionEfficiency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    // Helper methods
    public void transmitPacket(boolean delivered) {
        packetsTransmitted++;
        lastUsed = LocalDateTime.now();

        if (delivered) {
            packetsDelivered++;
        } else {
            packetsDropped++;
        }

        // Update transmission efficiency
        if (packetsTransmitted > 0) {
            transmissionEfficiency = (double) packetsDelivered / packetsTransmitted * 100.0;
        }
    }

    public boolean canTransmit() {
        return isActive && sourcePort != null && targetPort != null;
    }

    public double getDistance() {
        if (sourcePort != null && targetPort != null) {
            double dx = targetPort.getPositionX() - sourcePort.getPositionX();
            double dy = targetPort.getPositionY() - sourcePort.getPositionY();
            return Math.sqrt(dx * dx + dy * dy);
        }
        return 0.0;
    }

    public boolean isConnectedTo(PortEntity port) {
        return sourcePort != null && targetPort != null &&
                (sourcePort.equals(port) || targetPort.equals(port));
    }

    public PortEntity getOtherPort(PortEntity port) {
        if (sourcePort != null && sourcePort.equals(port)) {
            return targetPort;
        } else if (targetPort != null && targetPort.equals(port)) {
            return sourcePort;
        }
        return null;
    }

    @Override
    public String toString() {
        return "WireConnectionEntity{" +
                "wireConnectionId=" + wireConnectionId +
                ", connectionType='" + connectionType + '\'' +
                ", bandwidth=" + bandwidth +
                ", latency=" + latency +
                ", isActive=" + isActive +
                ", packetsTransmitted=" + packetsTransmitted +
                ", packetsDelivered=" + packetsDelivered +
                ", transmissionEfficiency=" + transmissionEfficiency +
                '}';
    }
}


