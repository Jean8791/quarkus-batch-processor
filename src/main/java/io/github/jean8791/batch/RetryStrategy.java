package io.github.jean8791.batch;

import java.util.Objects;
import java.util.Set;

/**
 * How many times a failing record is attempted again, and for which exceptions.
 *
 * <pre>{@code
 * RetryStrategy.fixedDelay(3, 200, OptimisticLockException.class)
 * }</pre>
 *
 * @param maxAttempts total attempts per record, including the first one; at least 1
 * @param delayMillis pause between attempts, in milliseconds
 * @param retryOn     exception types that trigger a retry; subclasses also match
 */
public record RetryStrategy(
        int maxAttempts,
        long delayMillis,
        Set<Class<? extends Exception>> retryOn
) {

    /**
     * Validates the arguments.
     *
     * @throws IllegalArgumentException if {@code maxAttempts} is below 1 or {@code delayMillis} is negative
     */
    public RetryStrategy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis must not be negative");
        }
        retryOn = retryOn == null ? Set.of() : Set.copyOf(retryOn);
    }

    /**
     * @return a strategy that never retries; this is the default
     */
    public static RetryStrategy noRetry() {
        return new RetryStrategy(1, 0, Set.of());
    }

    /**
     * Retries with a fixed pause between attempts.
     *
     * @param maxAttempts total attempts per record, including the first one
     * @param delayMillis pause between attempts, in milliseconds
     * @param retryOn     exception types that trigger a retry
     * @return the strategy
     */
    @SafeVarargs
    public static RetryStrategy fixedDelay(int maxAttempts, long delayMillis, Class<? extends Exception>... retryOn) {
        Objects.requireNonNull(retryOn, "retryOn must not be null");
        return new RetryStrategy(maxAttempts, delayMillis, Set.of(retryOn));
    }

    /**
     * Decides whether a failed attempt should be repeated.
     *
     * @param exception      the exception thrown by the attempt
     * @param currentAttempt number of the attempt that failed, starting at 1
     * @return {@code true} if there are attempts left and the exception type is retryable
     */
    public boolean shouldRetry(Exception exception, int currentAttempt) {
        if (currentAttempt >= maxAttempts || retryOn.isEmpty()) {
            return false;
        }
        return retryOn.stream().anyMatch(type -> type.isInstance(exception));
    }
}
