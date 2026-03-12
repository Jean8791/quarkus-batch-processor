package br.com.codenest.config;

import java.util.Objects;
import java.util.Set;

public record RetryStrategy(
        int maxAttempts,
        long delayMillis,
        Set<Class<? extends Exception>> retryOn
) {

    public RetryStrategy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis must not be negative");
        }
        retryOn = retryOn == null ? Set.of() : Set.copyOf(retryOn);
    }

    public static RetryStrategy noRetry() {
        return new RetryStrategy(1, 0, Set.of());
    }

    @SafeVarargs
    public static RetryStrategy fixedDelay(int maxAttempts, long delayMillis, Class<? extends Exception>... retryOn) {
        Objects.requireNonNull(retryOn, "retryOn must not be null");
        return new RetryStrategy(maxAttempts, delayMillis, Set.of(retryOn));
    }

    public boolean shouldRetry(Exception exception, int currentAttempt) {
        if (currentAttempt >= maxAttempts) {
            return false;
        }

        if (retryOn.isEmpty()) {
            return false;
        }

        return retryOn.stream().anyMatch(type -> type.isAssignableFrom(exception.getClass()));
    }
}