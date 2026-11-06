package io.github.jean8791.batch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryStrategyTest {

    @Test
    void noRetryNeverRetries() {
        assertFalse(RetryStrategy.noRetry().shouldRetry(new IllegalStateException(), 1));
    }

    @Test
    void retriesConfiguredTypeAndSubclassesUntilMaxAttempts() {
        RetryStrategy strategy = RetryStrategy.fixedDelay(3, 0, RuntimeException.class);

        assertTrue(strategy.shouldRetry(new IllegalStateException(), 1));
        assertTrue(strategy.shouldRetry(new UncheckedIOException(new IOException()), 2));
        assertFalse(strategy.shouldRetry(new IllegalStateException(), 3));
    }

    @Test
    void doesNotRetryOtherTypes() {
        RetryStrategy strategy = RetryStrategy.fixedDelay(3, 0, IllegalStateException.class);

        assertFalse(strategy.shouldRetry(new IllegalArgumentException(), 1));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> RetryStrategy.fixedDelay(0, 0));
        assertThrows(IllegalArgumentException.class, () -> RetryStrategy.fixedDelay(1, -1));
    }
}
