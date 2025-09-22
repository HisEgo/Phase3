package multiplayer;

import model.Point2D;
import java.util.*;

public class ControllableSystem {
    private String systemId;
    private Point2D position;
    private Map<String, Integer> ammunition;
    private boolean isActive;

    public ControllableSystem() {
        this.ammunition = new HashMap<>();
        this.isActive = true;
    }

    public ControllableSystem(String systemId, Point2D position) {
        this();
        this.systemId = systemId;
        this.position = position;
    }

    public void initializeAmmunition() {
        // Initialize with different packet types and quantities
        ammunition.put("SMALL_MESSENGER", 20);
        ammunition.put("MEDIUM_MESSENGER", 15);
        ammunition.put("LARGE_MESSENGER", 10);
        ammunition.put("PROTECTED", 8);
        ammunition.put("TROJAN", 5);
        ammunition.put("BULK_SMALL", 3);
        ammunition.put("BIT_PACKET", 25);
    }

    public boolean hasAmmunition(String packetType) {
        Integer ammo = ammunition.get(packetType);
        return ammo != null && ammo > 0;
    }

    public boolean consumeAmmunition(String packetType) {
        if (hasAmmunition(packetType)) {
            ammunition.put(packetType, ammunition.get(packetType) - 1);
            return true;
        }
        return false;
    }

    public int getAmmunitionCount(String packetType) {
        return ammunition.getOrDefault(packetType, 0);
    }


    public Map<String, Integer> getAmmunition() {
        return new HashMap<>(ammunition);
    }


    public void refillAmmunition(String packetType, int amount) {
        int current = ammunition.getOrDefault(packetType, 0);
        ammunition.put(packetType, current + amount);
    }


    public void addAmmunition(String packetType, int amount) {
        refillAmmunition(packetType, amount);
    }

    public int getTotalAmmunition() {
        return ammunition.values().stream().mapToInt(Integer::intValue).sum();
    }



    public boolean hasAnyAmmunition() {
        return ammunition.values().stream().anyMatch(count -> count > 0);
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setAmmunition(Map<String, Integer> ammunition) {
        this.ammunition = new HashMap<>(ammunition);
    }
}


