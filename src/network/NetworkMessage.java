package network;


public class NetworkMessage {
    public enum MessageType {
        CONNECT, DISCONNECT, GAME_STATE_UPDATE, PLAYER_ACTION,
        LEVEL_COMPLETE, SCORE_UPDATE, MULTIPLAYER_INVITE,
        MULTIPLAYER_ACCEPT, MULTIPLAYER_REJECT, GAME_OVER, PLAYER_READY,
        NETWORK_DATA, NETWORK_VISIBILITY_UPDATE
    }

    private MessageType type;
    private String playerId;
    private String sessionId;
    private Object data;

    public NetworkMessage() {}

    public NetworkMessage(MessageType type, String playerId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
    }

    public NetworkMessage(MessageType type, String playerId, String sessionId, Object data) {
        this(type, playerId, data);
        this.sessionId = sessionId;
    }

    // Getters and setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}


