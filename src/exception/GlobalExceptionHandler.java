package exception;

import java.util.logging.Logger;

public class GlobalExceptionHandler {
    private static final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());
    private static ExceptionHandlerFunction exceptionHandler;
    private static GlobalExceptionHandler instance;

    static {
        // Initialize the exception handler
        exceptionHandler = new ExceptionHandlerFunction();

        // Set up global exception handlers
        setupGlobalExceptionHandlers();
    }


    public static GlobalExceptionHandler getInstance() {
        if (instance == null) {
            instance = new GlobalExceptionHandler();
        }
        return instance;
    }


    private static void setupGlobalExceptionHandlers() {
        // Set up uncaught exception handler for threads
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            logger.severe("Uncaught exception in thread " + thread.getName() + ": " + throwable.getMessage());
            handleException(throwable, thread);
        });

        logger.info("Global exception handlers initialized");
    }

    public static boolean handleException(Throwable exception, Object context) {
        if (exception == null) return false;

        try {
            boolean handled = exceptionHandler.handleException(exception, context);

            if (!handled) {
                // Fallback to default handling
                logger.severe("Unhandled exception: " + exception.getMessage());
                exception.printStackTrace();
            }

            return handled;
        } catch (Exception e) {
            logger.severe("Error in global exception handler: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public static boolean handleException(Throwable exception) {
        return handleException(exception, null);
    }


    public static ExceptionHandlingStats getStats() {
        return exceptionHandler.getStats();
    }


    public static void resetStats() {
        exceptionHandler.resetStats();
    }


    public static ExceptionHandlerFunction getExceptionHandler() {
        return exceptionHandler;
    }

    @ExceptionHandlerAnnotation(level = "WARNING", message = "Global IO error handler")
    public static void handleIOException(java.io.IOException e, Object context) {
        logger.warning("Global IO Error: " + e.getMessage());
        // Could implement global IO error recovery logic
    }


    @ExceptionHandlerAnnotation(level = "SEVERE", message = "Global runtime error handler")
    public static void handleRuntimeException(RuntimeException e, Object context) {
        logger.severe("Global Runtime Error: " + e.getMessage());
        e.printStackTrace();
    }


    @ExceptionHandlerAnnotation(level = "WARNING", message = "Global null pointer error handler")
    public static void handleNullPointerException(NullPointerException e, Object context) {
        logger.warning("Global Null Pointer Error: " + e.getMessage());
        // Could implement null pointer recovery logic
    }

    @ExceptionHandlerAnnotation(level = "INFO", message = "Global illegal argument error handler")
    public static void handleIllegalArgumentException(IllegalArgumentException e, Object context) {
        logger.info("Global Illegal Argument Error: " + e.getMessage());
        // Could implement argument validation recovery logic
    }
}

