package multiplayer;

import model.GameLevel;
import java.io.Serializable;

public class NetworkVisibilityData implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean networkVisibilityEnabled;
    private GameLevel player1Network;
    private GameLevel player2Network;

    public NetworkVisibilityData() {
        this.networkVisibilityEnabled = false;
        this.player1Network = null;
        this.player2Network = null;
    }

    public NetworkVisibilityData(boolean networkVisibilityEnabled, GameLevel player1Network, GameLevel player2Network) {
        this.networkVisibilityEnabled = networkVisibilityEnabled;
        this.player1Network = player1Network;
        this.player2Network = player2Network;
    }

    public boolean isNetworkVisibilityEnabled() {
        return networkVisibilityEnabled;
    }

    public void setNetworkVisibilityEnabled(boolean networkVisibilityEnabled) {
        this.networkVisibilityEnabled = networkVisibilityEnabled;
    }

    public GameLevel getPlayer1Network() {
        return player1Network;
    }

    public void setPlayer1Network(GameLevel player1Network) {
        this.player1Network = player1Network;
    }

    public GameLevel getPlayer2Network() {
        return player2Network;
    }

    public void setPlayer2Network(GameLevel player2Network) {
        this.player2Network = player2Network;
    }

    /**
     * Gets the opponent's network for a given player.
     */
    public GameLevel getOpponentNetwork(String playerId) {
        if (!networkVisibilityEnabled) return null;

        if ("player1".equalsIgnoreCase(playerId) || "Player 1".equalsIgnoreCase(playerId) || "Player1".equalsIgnoreCase(playerId)) {
            return player2Network;
        } else if ("player2".equalsIgnoreCase(playerId) || "Player 2".equalsIgnoreCase(playerId) || "Player2".equalsIgnoreCase(playerId)) {
            return player1Network;
        }

        return null;
    }

    @Override
    public String toString() {
        return "NetworkVisibilityData{" +
                "networkVisibilityEnabled=" + networkVisibilityEnabled +
                ", player1Network=" + (player1Network != null ? "Present" : "Null") +
                ", player2Network=" + (player2Network != null ? "Present" : "Null") +
                '}';
    }
}



