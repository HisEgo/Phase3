package server;

import network.NetworkMessage;
import model.UserData;
import leaderboard.ScoreRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private String clientId;
    private Socket clientSocket;
    private GameServer server;
    private ObjectMapper objectMapper;

    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected;

    public ClientHandler(String clientId, Socket clientSocket, GameServer server) {
        this.clientId = clientId;
        this.clientSocket = clientSocket;
        this.server = server;
        this.objectMapper = new ObjectMapper();
        this.isConnected = true;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // Send welcome message
            sendMessage(new NetworkMessage(NetworkMessage.MessageType.CONNECT, clientId, "Welcome to Network Simulation Game!"));

            // Synchronize offline data if any exists
            server.getOfflineDataHandler().synchronizeOfflineData(clientId);

            // Handle incoming messages
            String inputLine;
            while (isConnected && (inputLine = in.readLine()) != null) {
                try {
                    NetworkMessage message = objectMapper.readValue(inputLine, NetworkMessage.class);
                    handleMessage(message);
                } catch (Exception e) {
                    // Log parsing error
                    System.err.println("Error parsing message from " + clientId + ": " + e.getMessage());
                    System.err.println("Raw message: " + inputLine);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling client " + clientId + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleMessage(NetworkMessage message) {
        switch (message.getType()) {
            case DISCONNECT:
                disconnect();
                break;
            case PLAYER_READY:
                handlePlayerReady(message);
                break;
            case MULTIPLAYER_INVITE:
                handleMultiplayerInvite(message);
                break;
            case MULTIPLAYER_ACCEPT:
                handleMultiplayerAccept(message);
                break;
            case MULTIPLAYER_REJECT:
                handleMultiplayerReject(message);
                break;
            case GAME_STATE_UPDATE:
                handleGameStateUpdate(message);
                break;
            case NETWORK_DATA:
                handleNetworkData(message);
                break;
            case PLAYER_ACTION:
                handlePlayerAction(message);
                break;
            case GAME_OVER:
                handleGameOver(message);
                break;
            case SCORE_UPDATE:
                handleScoreUpdate(message);
                break;
            case LEVEL_COMPLETE:
                handleLevelComplete(message);
                break;
            default:
                // Forward other message types to appropriate handlers
                break;
        }
    }

    private void handleMultiplayerInvite(NetworkMessage message) {
        try {
            String data = (String) message.getData();

            if ("CREATE_GAME".equals(data)) {
                // Handle game creation request
                String gameId = message.getSessionId();

                if (gameId != null && !gameId.isEmpty()) {
                    // Create a new multiplayer session using the client's game ID
                    String sessionId = server.createMultiplayerSessionWithId(gameId, clientId, null);

                    // Send confirmation to the creating player
                    sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                            "SERVER",
                            clientId,
                            "GAME_CREATED"
                    ));

                } else {

                    sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                            "SERVER",
                            clientId,
                            "GAME_CREATION_FAILED"
                    ));
                }
            } else if ("GET_ACTIVE_GAMES".equals(data)) {
                // Handle request for active games list

                // Get all active sessions that are waiting for players
                java.util.List<String> activeGames = new java.util.ArrayList<>();
                for (MultiplayerSession session : server.getActiveSessions().values()) {
                    if (session.isActive() && (session.getPlayer2Id() == null || session.getPlayer2Id().isEmpty())) {
                        // This session is waiting for a second player
                        activeGames.add(session.getSessionId() + " - Waiting for player");
                    }
                }

                if (activeGames.isEmpty()) {
                    activeGames.add("No active games available");
                }

                // Send the active games list back to the client
                sendMessage(new NetworkMessage(
                        NetworkMessage.MessageType.GAME_STATE_UPDATE,
                        "SERVER",
                        clientId,
                        activeGames
                ));

            } else {
                // Handle direct player invite (legacy functionality)
                String targetPlayerId = data;

                // Check if target player is connected
                ClientHandler targetClient = server.getConnectedClients().get(targetPlayerId);
                if (targetClient != null && targetClient.isConnected()) {
                    // Send invite to target player
                    NetworkMessage inviteMessage = new NetworkMessage(
                            NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                            clientId,
                            targetPlayerId,
                            "Player " + clientId + " wants to play with you!"
                    );
                    targetClient.sendMessage(inviteMessage);

                    // Send confirmation to inviting player
                    sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                            "SERVER",
                            clientId,
                            "Invite sent to " + targetPlayerId
                    ));
                } else {
                    // Target player not available
                    sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                            "SERVER",
                            clientId,
                            "Player " + targetPlayerId + " is not available"
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling multiplayer invite: " + e.getMessage());
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.MULTIPLAYER_INVITE,
                    "SERVER",
                    clientId,
                    "GAME_CREATION_FAILED"
            ));
        }
    }

    private void handleMultiplayerAccept(NetworkMessage message) {
        try {
            String data = (String) message.getData();

            if ("JOIN_GAME".equals(data)) {
                // Handle game joining request
                String gameId = message.getSessionId();

                if (gameId != null && !gameId.isEmpty()) {
                    // Find the specific session by game ID that is waiting for a second player
                    MultiplayerSession waitingSession = server.findWaitingSessionById(gameId);

                    if (waitingSession != null) {
                        // Check if the user is trying to join their own game
                        if (waitingSession.getPlayer1Id().equals(clientId)) {
                            sendMessage(new NetworkMessage(
                                    NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                                    "SERVER",
                                    clientId,
                                    "GAME_JOIN_FAILED"
                            ));
                            return;
                        }

                        // Join the existing session
                        waitingSession.setPlayer2Id(clientId);

                        // Notify the joiner that they joined successfully
                        NetworkMessage joinSuccessMessage = new NetworkMessage(
                                NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                                "SERVER",
                                waitingSession.getSessionId(),
                                "GAME_JOINED"
                        );
                        sendMessage(joinSuccessMessage);

                        // Notify the host that a player joined
                        ClientHandler player1Client = server.getConnectedClients().get(waitingSession.getPlayer1Id());
                        if (player1Client != null) {
                            NetworkMessage playerJoinedMessage = new NetworkMessage(
                                    NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                                    "SERVER",
                                    waitingSession.getSessionId(),
                                    "PLAYER_JOINED"
                            );
                            player1Client.sendMessage(playerJoinedMessage);
                        } else {
                        }

                    } else {
                        // No waiting session found with the specified game ID
                        sendMessage(new NetworkMessage(
                                NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                                "SERVER",
                                clientId,
                                "GAME_JOIN_FAILED"
                        ));
                    }
                } else {

                    sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                            "SERVER",
                            clientId,
                            "GAME_JOIN_FAILED"
                    ));
                }
            } else {
                // Handle direct player accept (legacy functionality)
                String invitingPlayerId = data;

                // Check if inviting player is still connected
                ClientHandler invitingClient = server.getConnectedClients().get(invitingPlayerId);
                if (invitingClient != null && invitingClient.isConnected()) {
                    // Create multiplayer session
                    String sessionId = server.createMultiplayerSession(invitingPlayerId, clientId);

                    // Notify both players that the game is starting
                    NetworkMessage gameStartMessage = new NetworkMessage(
                            NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                            "SERVER",
                            sessionId,
                            "Game starting with session: " + sessionId
                    );

                    invitingClient.sendMessage(gameStartMessage);
                    sendMessage(gameStartMessage);

                    System.out.println("Multiplayer session created: " + sessionId + " between " + invitingPlayerId + " and " + clientId);
                } else {
                    // Inviting player disconnected
                    sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.MULTIPLAYER_REJECT,
                            "SERVER",
                            clientId,
                            "Inviting player is no longer available"
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling multiplayer accept: " + e.getMessage());
            sendMessage(new NetworkMessage(
                    NetworkMessage.MessageType.MULTIPLAYER_ACCEPT,
                    "SERVER",
                    clientId,
                    "GAME_JOIN_FAILED"
            ));
        }
    }

    private void handleMultiplayerReject(NetworkMessage message) {
        try {
            String invitingPlayerId = (String) message.getData();

            // Check if inviting player is still connected
            ClientHandler invitingClient = server.getConnectedClients().get(invitingPlayerId);
            if (invitingClient != null && invitingClient.isConnected()) {
                // Notify inviting player that invite was rejected
                NetworkMessage rejectMessage = new NetworkMessage(
                        NetworkMessage.MessageType.MULTIPLAYER_REJECT,
                        clientId,
                        invitingPlayerId,
                        "Player " + clientId + " declined your invitation"
                );
                invitingClient.sendMessage(rejectMessage);

                System.out.println("Player " + clientId + " rejected invite from " + invitingPlayerId);
            }
        } catch (Exception e) {
            System.err.println("Error handling multiplayer reject: " + e.getMessage());
        }
    }

    private void handleGameStateUpdate(NetworkMessage message) {
        try {

            // Extract session ID and game state data from message
            String sessionId = message.getSessionId();
            Object gameStateData = message.getData();

            if (sessionId != null) {
                // Get the multiplayer session
                MultiplayerSession session = server.getSession(sessionId);
                if (session != null && session.isActive()) {
                    // Forward the game state update to the other player
                    String otherPlayerId = session.getOtherPlayerId(clientId);
                    if (otherPlayerId != null) {
                        ClientHandler otherClient = server.getConnectedClients().get(otherPlayerId);
                        if (otherClient != null && otherClient.isConnected()) {
                            // Forward the message to the other player
                            otherClient.sendMessage(message);
                        } else {
                        }
                    } else {
                    }
                } else {
                }
            } else {

            }
        } catch (Exception e) {
            System.err.println("Error handling game state update: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleNetworkData(NetworkMessage message) {
        try {
            System.out.println("🔄 SERVER: Processing network data from " + clientId);

            // Extract session ID and network data from message
            String sessionId = message.getSessionId();
            Object networkData = message.getData();

            if (sessionId != null) {
                // Get the multiplayer session
                MultiplayerSession session = server.getSession(sessionId);
                if (session != null && session.isActive()) {
                    // Forward the network data to the other player
                    String otherPlayerId = session.getOtherPlayerId(clientId);
                    if (otherPlayerId != null) {
                        ClientHandler otherClient = server.getConnectedClients().get(otherPlayerId);
                        if (otherClient != null && otherClient.isConnected()) {
                            // Forward the network data message to the other player
                            NetworkMessage forwardMessage = new NetworkMessage(
                                    NetworkMessage.MessageType.NETWORK_DATA,
                                    message.getPlayerId(), // Keep original sender ID
                                    sessionId,
                                    networkData
                            );
                            otherClient.sendMessage(forwardMessage);
                            System.out.println("✅ SERVER: Forwarded network data " + clientId + " → " + otherPlayerId);
                        } else {
                            System.out.println("❌ SERVER: Target client " + otherPlayerId + " not connected");
                        }
                    } else {
                        System.out.println("❌ SERVER: No opponent found in session " + sessionId);
                    }
                } else {
                    System.out.println("❌ SERVER: Session " + sessionId + " not found or inactive");
                }
            } else {
                System.out.println("❌ SERVER: No session ID in network data message");
            }
        } catch (Exception e) {
            System.err.println("❌ SERVER: Error handling network data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handlePlayerReady(NetworkMessage message) {
        try {
            String sessionId = message.getSessionId();
            System.out.println("🎮 PLAYER_READY from client: " + clientId + " for session: " + sessionId);

            if (sessionId == null) {
                System.out.println("❌ PLAYER_READY: Session ID is null");
                return;
            }

            MultiplayerSession session = server.getSession(sessionId);
            if (session == null) {
                System.out.println("❌ PLAYER_READY: Session not found: " + sessionId);
                System.out.println("Available sessions: " + server.getActiveSessions().keySet());
                return;
            }

            if (!session.isActive()) {
                System.out.println("❌ PLAYER_READY: Session inactive: " + sessionId);
                return;
            }

            System.out.println("✅ PLAYER_READY: Processing for client " + clientId + " in session " + sessionId);
            session.setPlayerReady(clientId);

        } catch (Exception e) {
            System.err.println("❌ PLAYER_READY Error: " + e.getMessage());
        }
    }

    private void handlePlayerAction(NetworkMessage message) {
        try {
            // Extract session ID and action data from message
            String sessionId = message.getSessionId();
            Object actionData = message.getData();

            if (sessionId != null) {
                // Get the multiplayer session
                MultiplayerSession session = server.getSession(sessionId);
                if (session != null && session.isActive()) {
                    // Forward the action to the session for processing
                    session.handlePlayerAction(clientId, actionData);

                    // Notify the other player about the action
                    String otherPlayerId = session.getOtherPlayerId(clientId);
                    if (otherPlayerId != null) {
                        ClientHandler otherClient = server.getConnectedClients().get(otherPlayerId);
                        if (otherClient != null && otherClient.isConnected()) {
                            NetworkMessage actionNotification = new NetworkMessage(
                                    NetworkMessage.MessageType.PLAYER_ACTION,
                                    clientId,
                                    sessionId,
                                    actionData
                            );
                            otherClient.sendMessage(actionNotification);
                        }
                    }
                } else {
                    // Session not found or inactive
                    sendMessage(new NetworkMessage(
                            NetworkMessage.MessageType.PLAYER_ACTION,
                            "SERVER",
                            clientId,
                            "Session not found or inactive"
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling player action: " + e.getMessage());
        }
    }

    private void handleGameOver(NetworkMessage message) {
        try {
            String sessionId = message.getSessionId();
            String gameResult = (String) message.getData();

            if (sessionId != null) {
                // Get the multiplayer session
                MultiplayerSession session = server.getSession(sessionId);
                if (session != null) {
                    // Notify the other player about game over
                    String otherPlayerId = session.getOtherPlayerId(clientId);
                    if (otherPlayerId != null) {
                        ClientHandler otherClient = server.getConnectedClients().get(otherPlayerId);
                        if (otherClient != null && otherClient.isConnected()) {
                            NetworkMessage gameOverNotification = new NetworkMessage(
                                    NetworkMessage.MessageType.GAME_OVER,
                                    "SERVER",
                                    sessionId,
                                    gameResult
                            );
                            otherClient.sendMessage(gameOverNotification);
                        }
                    }

                    // Update leaderboard with final scores
                    if (session.getPlayer1State() != null && session.getPlayer2State() != null) {
                        server.getUserManager().updatePlayerScore(clientId, session.getPlayer1State().getScore(), session.getPlayer1State().getScore(), "multiplayer");
                        server.getUserManager().updatePlayerScore(otherPlayerId, session.getPlayer2State().getScore(), session.getPlayer2State().getScore(), "multiplayer");
                    }

                    // Clean up the session
                    server.removeSession(sessionId);

                    System.out.println("Game over in session " + sessionId + ": " + gameResult);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling game over: " + e.getMessage());
        }
    }

    private void handleScoreUpdate(NetworkMessage message) {
        try {
            if (message.getData() instanceof ScoreRecord) {
                ScoreRecord score = (ScoreRecord) message.getData();

                // Validate score integrity
                var validationResult = server.getDataValidator().validateScoreSubmission(score, clientId);

                if (validationResult.isValid()) {
                    // Update user profile with new score
                    server.getUserManager().saveUserData(clientId,
                            new UserData(null, null, score.getXpEarned(), null, score));

                    System.out.println("Valid score update from " + clientId + ": " + score.getXpEarned() + " XP");
                } else {
                    System.err.println("Invalid score submission from " + clientId + ": " + validationResult.getViolations());

                    // Send rejection message to client
                    sendMessage(new NetworkMessage(NetworkMessage.MessageType.SCORE_UPDATE, "SERVER",
                            "Score validation failed: " + validationResult.getViolations()));
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling score update: " + e.getMessage());
        }
    }

    private void handleLevelComplete(NetworkMessage message) {
        try {
            if (message.getData() instanceof ScoreRecord) {
                ScoreRecord score = (ScoreRecord) message.getData();

                // Validate score integrity
                var validationResult = server.getDataValidator().validateScoreSubmission(score, clientId);

                if (validationResult.isValid()) {
                    // Update user profile
                    server.getUserManager().saveUserData(clientId,
                            new UserData(null, null, score.getXpEarned(), null, score));

                    System.out.println("Level completed by " + clientId + ": " + score.getLevelId() + " in " + score.getCompletionTime() + "s");
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling level completion: " + e.getMessage());
        }
    }

    public void sendMessage(NetworkMessage message) {
        if (isConnected && out != null) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                out.println(jsonMessage);
            } catch (Exception e) {
                System.err.println("Error sending message to " + clientId + ": " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        isConnected = false;

        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
        } catch (Exception e) {
            // Log error
        }

        // Remove from server's client list
        server.removeClient(clientId);
    }

    // Getters
    public String getClientId() {
        return clientId;
    }

    public boolean isConnected() {
        return isConnected;
    }
}



