package exception;


public class GameException extends Exception {

    private final String errorCode;
    private final Object context;

    public GameException(String message) {
        super(message);
        this.errorCode = "GAME_ERROR";
        this.context = null;
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GAME_ERROR";
        this.context = null;
    }

    public GameException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.context = null;
    }

    public GameException(String message, String errorCode, Object context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public GameException(String message, String errorCode, Object context, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("GameException{errorCode='%s', message='%s', context=%s}",
                errorCode, getMessage(), context);
    }
}


