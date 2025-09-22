package multiplayer;

public class CooldownInfo {
    private String id;
    private double remainingTime;
    private String type; // "SYSTEM" or "PACKET"

    public CooldownInfo() {}

    public CooldownInfo(String id, double remainingTime, String type) {
        this.id = id;
        this.remainingTime = remainingTime;
        this.type = type;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(double remainingTime) {
        this.remainingTime = remainingTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public boolean isExpired() {
        return remainingTime <= 0;
    }

    public double getProgress() {
        // This would need the total cooldown duration to calculate progress
        // For now, return a simple boolean-like value
        return isExpired() ? 1.0 : 0.0;
    }

    public String getFormattedTime() {
        if (remainingTime <= 0) return "Ready";

        int seconds = (int) Math.ceil(remainingTime);
        if (seconds < 60) {
            return seconds + "s";
        } else {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return String.format("%dm %ds", minutes, remainingSeconds);
        }
    }
}


