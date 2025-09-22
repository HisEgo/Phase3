package database.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "systems")
public class SystemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "system_id")
    private Long systemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_session_id", nullable = false)
    private GameSessionEntity gameSession;

    @Column(name = "system_name", length = 100, nullable = false)
    private String systemName;

    @Column(name = "system_type", length = 50, nullable = false)
    private String systemType;

    @Column(name = "position_x")
    private double positionX;

    @Column(name = "position_y")
    private double positionY;

    @Column(name = "is_controllable")
    private boolean isControllable;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "ammunition_capacity")
    private int ammunitionCapacity;

    @Column(name = "current_ammunition")
    private int currentAmmunition;

    @Column(name = "cooldown_duration")
    private double cooldownDuration;

    @Column(name = "last_used")
    private java.time.LocalDateTime lastUsed;

    @OneToMany(mappedBy = "system", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PortEntity> ports = new ArrayList<>();

    // Constructors
    public SystemEntity() {
        this.isControllable = false;
        this.isActive = true;
        this.ammunitionCapacity = 0;
        this.currentAmmunition = 0;
        this.cooldownDuration = 0.0;
    }

    public SystemEntity(String systemName, String systemType, double positionX, double positionY) {
        this();
        this.systemName = systemName;
        this.systemType = systemType;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    // Getters and Setters
    public Long getSystemId() {
        return systemId;
    }

    public void setSystemId(Long systemId) {
        this.systemId = systemId;
    }

    public GameSessionEntity getGameSession() {
        return gameSession;
    }

    public void setGameSession(GameSessionEntity gameSession) {
        this.gameSession = gameSession;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public double getPositionX() {
        return positionX;
    }

    public void setPositionX(double positionX) {
        this.positionX = positionX;
    }

    public double getPositionY() {
        return positionY;
    }

    public void setPositionY(double positionY) {
        this.positionY = positionY;
    }

    public boolean isControllable() {
        return isControllable;
    }

    public void setControllable(boolean controllable) {
        isControllable = controllable;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getAmmunitionCapacity() {
        return ammunitionCapacity;
    }

    public void setAmmunitionCapacity(int ammunitionCapacity) {
        this.ammunitionCapacity = ammunitionCapacity;
    }

    public int getCurrentAmmunition() {
        return currentAmmunition;
    }

    public void setCurrentAmmunition(int currentAmmunition) {
        this.currentAmmunition = currentAmmunition;
    }

    public double getCooldownDuration() {
        return cooldownDuration;
    }

    public void setCooldownDuration(double cooldownDuration) {
        this.cooldownDuration = cooldownDuration;
    }

    public java.time.LocalDateTime getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(java.time.LocalDateTime lastUsed) {
        this.lastUsed = lastUsed;
    }

    public List<PortEntity> getPorts() {
        return ports;
    }

    public void setPorts(List<PortEntity> ports) {
        this.ports = ports;
    }

    // Helper methods
    public void addPort(PortEntity port) {
        ports.add(port);
        port.setSystem(this);
    }

    public boolean canUse() {
        if (!isActive || currentAmmunition <= 0) {
            return false;
        }

        if (lastUsed != null && cooldownDuration > 0) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.Duration timeSinceLastUse = java.time.Duration.between(lastUsed, now);
            return timeSinceLastUse.getSeconds() >= cooldownDuration;
        }

        return true;
    }

    public void use() {
        if (canUse()) {
            currentAmmunition = Math.max(0, currentAmmunition - 1);
            lastUsed = java.time.LocalDateTime.now();
        }
    }

    public void reload(int amount) {
        currentAmmunition = Math.min(ammunitionCapacity, currentAmmunition + amount);
    }

    public double getAmmunitionPercentage() {
        if (ammunitionCapacity == 0) {
            return 0.0;
        }
        return (double) currentAmmunition / ammunitionCapacity * 100.0;
    }

    @Override
    public String toString() {
        return "SystemEntity{" +
                "systemId=" + systemId +
                ", systemName='" + systemName + '\'' +
                ", systemType='" + systemType + '\'' +
                ", positionX=" + positionX +
                ", positionY=" + positionY +
                ", isControllable=" + isControllable +
                ", currentAmmunition=" + currentAmmunition +
                '}';
    }
}


