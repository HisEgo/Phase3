package multiplayer;

public class PlayerAction {
    public enum ActionType {
        RELEASE_PACKET,
        USE_ABILITY,
        READY_UP,
        PAUSE_GAME,
        RESUME_GAME
    }

    private String playerId;
    private ActionType type;
    private String systemId;
    private String packetType;
    private String abilityId;
    private long timestamp;

    public PlayerAction() {
        this.timestamp = System.currentTimeMillis();
    }

    public PlayerAction(String playerId, ActionType type) {
        this();
        this.playerId = playerId;
        this.type = type;
    }

    public PlayerAction(String playerId, ActionType type, String systemId, String packetType) {
        this(playerId, type);
        this.systemId = systemId;
        this.packetType = packetType;
    }

    public PlayerAction(String playerId, ActionType type, String abilityId) {
        this(playerId, type);
        this.abilityId = abilityId;
    }

    // Getters and setters
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPacketType() {
        return packetType;
    }

    public void setPacketType(String packetType) {
        this.packetType = packetType;
    }

    public String getAbilityId() {
        return abilityId;
    }

    public void setAbilityId(String abilityId) {
        this.abilityId = abilityId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PlayerAction{playerId='" + playerId + "', type=" + type +
                ", systemId='" + systemId + "', packetType='" + packetType +
                "', abilityId='" + abilityId + "', timestamp=" + timestamp + "}";
    }
}


