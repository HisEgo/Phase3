package multiplayer;

public class SystemCooldown {
    private String systemId;
    private double cooldownDuration;
    private double remainingTime;
    private boolean isActive;

    public SystemCooldown() {
        this.isActive = false;
        this.remainingTime = 0.0;
    }

    public SystemCooldown(String systemId, double cooldownDuration) {
        this();
        this.systemId = systemId;
        this.cooldownDuration = cooldownDuration;
    }

    public void activate() {
        isActive = true;
        remainingTime = cooldownDuration;
    }

    public void update() {
        if (isActive && remainingTime > 0) {
            remainingTime -= 0.016; // Assuming 60 FPS (16.67ms per frame)

            if (remainingTime <= 0) {
                remainingTime = 0;
                isActive = false;
            }
        }
    }

    public boolean isExpired() {
        return !isActive || remainingTime <= 0;
    }

    public double getRemainingTime() {
        return Math.max(0, remainingTime);
    }

    public double getProgress() {
        if (cooldownDuration <= 0) return 1.0;
        return 1.0 - (remainingTime / cooldownDuration);
    }

    public void reset() {
        isActive = false;
        remainingTime = 0.0;
    }

    public void increaseCooldown(double percentage) {
        cooldownDuration *= (1.0 + percentage);
        if (isActive) {
            remainingTime *= (1.0 + percentage);
        }
    }

    // Getters and setters
    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public double getCooldownDuration() {
        return cooldownDuration;
    }

    public void setCooldownDuration(double cooldownDuration) {
        this.cooldownDuration = cooldownDuration;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setRemainingTime(double remainingTime) {
        this.remainingTime = remainingTime;
    }
}


