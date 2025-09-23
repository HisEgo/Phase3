package exception;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandlerFunction {
    private static final Logger logger = Logger.getLogger(ExceptionHandlerFunction.class.getName());

    private Map<Class<?>, List<Method>> exceptionHandlers;
    private Map<Method, ExceptionHandlerAnnotation> handlerAnnotations;
    private ExceptionHandlingStats stats;

    public ExceptionHandlerFunction() {
        this.exceptionHandlers = new ConcurrentHashMap<>();
        this.handlerAnnotations = new ConcurrentHashMap<>();
        this.stats = new ExceptionHandlingStats();

        // Discover exception handlers in the current package and subpackages
        discoverExceptionHandlers();
    }


    private void discoverExceptionHandlers() {
        try {
            // Get all classes in the current package
            String packageName = "exception";
            ClassLoader classLoader = getClass().getClassLoader();

            // Scan for classes with exception handlers
            scanPackageForExceptionHandlers(packageName, classLoader);

            logger.info("Discovered " + exceptionHandlers.size() + " exception handler classes");

        } catch (Exception e) {
            logger.severe("Error discovering exception handlers: " + e.getMessage());
        }
    }

    private void scanPackageForExceptionHandlers(String packageName, ClassLoader classLoader) {
        try {
            // Get all classes in the package
            String packagePath = packageName.replace('.', '/');
            java.net.URL resource = classLoader.getResource(packagePath);

            if (resource != null) {
                // This is a simplified approach - in production you'd use a library like Reflections
                scanKnownClassesForExceptionHandlers();
            }
        } catch (Exception e) {
            logger.warning("Error scanning package " + packageName + ": " + e.getMessage());
        }
    }

    private void scanKnownClassesForExceptionHandlers() {
        // List of known classes that might have exception handlers
        String[] knownClasses = {
                "server.GameServer",
                "server.ClientHandler",
                "server.MultiplayerSession",
                "multiplayer.MultiplayerGameController",
                "network.NetworkManager",
                "leaderboard.LeaderboardManager",
                "security.DataIntegrityValidator"
        };

        for (String className : knownClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                scanClassForExceptionHandlers(clazz);
            } catch (ClassNotFoundException e) {
                // Class not found, skip it
            } catch (Exception e) {
                logger.warning("Error scanning class " + className + ": " + e.getMessage());
            }
        }
    }

    private void scanClassForExceptionHandlers(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            ExceptionHandlerAnnotation annotation = method.getAnnotation(ExceptionHandlerAnnotation.class);
            if (annotation != null) {
                // Found an exception handler method
                method.setAccessible(true);

                // Determine what exception types this method can handle
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length > 0) {
                    Class<?> exceptionType = parameterTypes[0];
                    if (Throwable.class.isAssignableFrom(exceptionType)) {
                        exceptionHandlers.computeIfAbsent(exceptionType, k -> new ArrayList<>()).add(method);
                        handlerAnnotations.put(method, annotation);

                        logger.info("Found exception handler: " + clazz.getSimpleName() + "." + method.getName() +
                                " for " + exceptionType.getSimpleName());
                    }
                }
            }
        }
    }

    /**
     * Handles an exception using the discovered exception handlers.
     */
    public boolean handleException(Throwable exception, Object context) {
        if (exception == null) return false;

        stats.incrementExceptionCount();

        try {
            // Find handlers for this exception type
            List<Method> handlers = findHandlersForException(exception);

            if (handlers.isEmpty()) {
                logger.warning("No exception handlers found for " + exception.getClass().getSimpleName());
                stats.incrementUnhandledCount();
                return false;
            }

            // Try each handler until one succeeds
            for (Method handler : handlers) {
                if (invokeExceptionHandler(handler, exception, context)) {
                    stats.incrementHandledCount();
                    return true;
                }
            }

            stats.incrementUnhandledCount();
            return false;

        } catch (Exception e) {
            logger.severe("Error in exception handling system: " + e.getMessage());
            stats.incrementHandlerErrorCount();
            return false;
        }
    }

    private List<Method> findHandlersForException(Throwable exception) {
        List<Method> handlers = new ArrayList<>();
        Class<?> exceptionClass = exception.getClass();

        // Check for exact match
        List<Method> exactHandlers = exceptionHandlers.get(exceptionClass);
        if (exactHandlers != null) {
            handlers.addAll(exactHandlers);
        }

        // Check for superclass matches
        Class<?> superClass = exceptionClass.getSuperclass();
        while (superClass != null && !superClass.equals(Object.class)) {
            List<Method> superHandlers = exceptionHandlers.get(superClass);
            if (superHandlers != null) {
                handlers.addAll(superHandlers);
            }
            superClass = superClass.getSuperclass();
        }

        // Check for interface matches
        for (Class<?> interfaceClass : exceptionClass.getInterfaces()) {
            List<Method> interfaceHandlers = exceptionHandlers.get(interfaceClass);
            if (interfaceHandlers != null) {
                handlers.addAll(interfaceHandlers);
            }
        }

        return handlers;
    }

    private boolean invokeExceptionHandler(Method handler, Throwable exception, Object context) {
        try {
            ExceptionHandlerAnnotation annotation = handlerAnnotations.get(handler);
            if (annotation == null) return false;

            // Log the exception according to the annotation level
            logException(exception, annotation);

            // Prepare method parameters
            Object[] parameters = prepareMethodParameters(handler, exception, context);

            // Invoke the handler method
            handler.invoke(null, parameters);

            // Check if the handler wants to rethrow the exception
            if (annotation.rethrow()) {
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException) exception;
                } else {
                    throw new RuntimeException(exception);
                }
            }

            return true;

        } catch (Exception e) {
            logger.severe("Error invoking exception handler " + handler.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Logs the exception according to the annotation configuration.
     */
    private void logException(Throwable exception, ExceptionHandlerAnnotation annotation) {
        String message = annotation.message().isEmpty() ?
                "Exception handled: " + exception.getMessage() : annotation.message();

        Level level = Level.parse(annotation.level());
        logger.log(level, message, exception);
    }

    /**
     * Prepares method parameters for the exception handler.
     */
    private Object[] prepareMethodParameters(Method method, Throwable exception, Object context) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> paramType = parameterTypes[i];

            if (Throwable.class.isAssignableFrom(paramType)) {
                parameters[i] = exception;
            } else if (paramType.isAssignableFrom(context.getClass())) {
                parameters[i] = context;
            } else {
                // Provide default value for other parameter types
                parameters[i] = getDefaultValue(paramType);
            }
        }

        return parameters;
    }

    /**
     * Gets a default value for a parameter type.
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0;
        if (type == char.class) return '\0';
        return null;
    }

    /**
     * Gets exception handling statistics.
     */
    public ExceptionHandlingStats getStats() {
        return stats;
    }

    /**
     * Resets exception handling statistics.
     */
    public void resetStats() {
        stats.reset();
    }

    /**
     * Gets all discovered exception handlers.
     */
    public Map<Class<?>, List<Method>> getExceptionHandlers() {
        return new HashMap<>(exceptionHandlers);
    }
}

