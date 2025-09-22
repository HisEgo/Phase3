package exception;

import java.util.concurrent.atomic.AtomicLong;


public class ExceptionHandlingStats {
    private final AtomicLong totalExceptions;
    private final AtomicLong handledExceptions;
    private final AtomicLong unhandledExceptions;
    private final AtomicLong handlerErrors;
    private final AtomicLong lastResetTime;

    public ExceptionHandlingStats() {
        this.totalExceptions = new AtomicLong(0);
        this.handledExceptions = new AtomicLong(0);
        this.unhandledExceptions = new AtomicLong(0);
        this.handlerErrors = new AtomicLong(0);
        this.lastResetTime = new AtomicLong(System.currentTimeMillis());
    }

    public void incrementExceptionCount() {
        totalExceptions.incrementAndGet();
    }


    public void incrementHandledCount() {
        handledExceptions.incrementAndGet();
    }

    public void incrementUnhandledCount() {
        unhandledExceptions.incrementAndGet();
    }


    public void incrementHandlerErrorCount() {
        handlerErrors.incrementAndGet();
    }

    public long getTotalExceptions() {
        return totalExceptions.get();
    }

    public long getHandledExceptions() {
        return handledExceptions.get();
    }

    public long getUnhandledExceptions() {
        return unhandledExceptions.get();
    }


    public long getHandlerErrors() {
        return handlerErrors.get();
    }

    public double getSuccessRate() {
        long total = totalExceptions.get();
        if (total == 0) return 0.0;

        return (double) handledExceptions.get() / total * 100.0;
    }

    public long getLastResetTime() {
        return lastResetTime.get();
    }

    public long getUptimeMillis() {
        return System.currentTimeMillis() - lastResetTime.get();
    }

    public void reset() {
        totalExceptions.set(0);
        handledExceptions.set(0);
        unhandledExceptions.set(0);
        handlerErrors.set(0);
        lastResetTime.set(System.currentTimeMillis());
    }

    public String getSummary() {
        return String.format(
                "Exception Handling Stats - Total: %d, Handled: %d, Unhandled: %d, Errors: %d, Success Rate: %.2f%%",
                getTotalExceptions(),
                getHandledExceptions(),
                getUnhandledExceptions(),
                getHandlerErrors(),
                getSuccessRate()
        );
    }

    @Override
    public String toString() {
        return getSummary();
    }
}

