package network;

import api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class NetworkManager {
    private static final int DEFAULT_PORT = 8081;
    private static final String DEFAULT_HOST = "localhost";

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ObjectMapper objectMapper;
    private ExecutorService messageHandler;

    private String host;
    private int port;
    private boolean isConnected;
    private Consumer<String> connectionStatusCallback;
    private Consumer<NetworkMessage> messageCallback;

    public NetworkManager() {
        this.host = DEFAULT_HOST;
        this.port = DEFAULT_PORT;
        this.isConnected = false;
        this.objectMapper = new ObjectMapper();
        this.messageHandler = Executors.newSingleThreadExecutor();
    }

    public NetworkManager(String host, int port) {
        this();
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isConnected = true;


            // Start message listener
            startMessageListener();

            if (connectionStatusCallback != null) {
                connectionStatusCallback.accept("Connected to server");
            }

            return true;
        } catch (Exception e) {
            isConnected = false;
            if (connectionStatusCallback != null) {
                connectionStatusCallback.accept("Connection failed: " + e.getMessage());
            }
            return false;
        }
    }

    public void disconnect() {
        try {
            isConnected = false;
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();

            if (connectionStatusCallback != null) {
                connectionStatusCallback.accept("Disconnected from server");
            }
        } catch (Exception e) {
            // Log error
        }
    }

    public boolean sendMessage(NetworkMessage message) {
        if (!isConnected) {

            return false;
        }

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            out.println(jsonMessage);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void startMessageListener() {
        messageHandler.submit(() -> {
            try {
                String inputLine;
                while (isConnected && (inputLine = in.readLine()) != null) {
                    try {
                        NetworkMessage message = objectMapper.readValue(inputLine, NetworkMessage.class);
                        if (messageCallback != null) {
                            messageCallback.accept(message);
                        }
                    } catch (Exception e) {
                        // Log parsing error
                    }
                }
            } catch (Exception e) {
                // Connection lost
                isConnected = false;
                if (connectionStatusCallback != null) {
                    connectionStatusCallback.accept("Connection lost");
                }
            }
        });
    }

    public boolean reconnect() {
        disconnect();
        try {
            Thread.sleep(1000); // Wait before reconnecting
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return connect();
    }

    // Getters and setters
    public boolean isConnected() {
        return isConnected;
    }

    public void setConnectionStatusCallback(Consumer<String> callback) {
        this.connectionStatusCallback = callback;
    }

    public void setMessageCallback(Consumer<NetworkMessage> callback) {
        this.messageCallback = callback;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
}


