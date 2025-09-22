package multiplayer;

import model.Point2D;
import java.util.*;

public class SystemInfo {
    private String systemId;
    private Point2D position;
    private Map<String, Integer> ammunition;
    private double cooldownRemaining;

    public SystemInfo() {
        this.ammunition = new HashMap<>();
    }

    public SystemInfo(String systemId, Point2D position, Map<String, Integer> ammunition, double cooldownRemaining) {
        this();
        this.systemId = systemId;
        this.position = position;
        this.ammunition = new HashMap<>(ammunition);
        this.cooldownRemaining = cooldownRemaining;
    }

    // Getters and setters
    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    public Map<String, Integer> getAmmunition() {
        return new HashMap<>(ammunition);
    }

    public void setAmmunition(Map<String, Integer> ammunition) {
        this.ammunition = new HashMap<>(ammunition);
    }

    public double getCooldownRemaining() {
        return cooldownRemaining;
    }

    public void setCooldownRemaining(double cooldownRemaining) {
        this.cooldownRemaining = cooldownRemaining;
    }

    public int getTotalAmmunition() {
        return ammunition.values().stream().mapToInt(Integer::intValue).sum();
    }


    public boolean hasAnyAmmunition() {
        return getTotalAmmunition() > 0;
    }

    public boolean isAvailable() {
        return cooldownRemaining <= 0;
    }
}


