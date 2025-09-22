package exception;

public class NetworkException extends GameException {

    private final String networkAddress;
    private final int port;

    public NetworkException(String message) {
        super(message, "NETWORK_ERROR");
        this.networkAddress = null;
        this.port = -1;
    }

    public NetworkException(String message, String networkAddress, int port) {
        super(message, "NETWORK_ERROR");
        this.networkAddress = networkAddress;
        this.port = port;
    }

    public NetworkException(String message, String networkAddress, int port, Throwable cause) {
        super(message, "NETWORK_ERROR", null, cause);
        this.networkAddress = networkAddress;
        this.port = port;
    }

    public NetworkException(String message, String errorCode, String networkAddress, int port) {
        super(message, errorCode);
        this.networkAddress = networkAddress;
        this.port = port;
    }

    public String getNetworkAddress() {
        return networkAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return String.format("NetworkException{address='%s', port=%d, errorCode='%s', message='%s'}",
                networkAddress, port, getErrorCode(), getMessage());
    }
}


