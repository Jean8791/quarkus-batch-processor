package br.com.codenest.engine;

import br.com.codenest.result.ProcessingResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessingLoopTest {

    @Test
    void shouldProcessAllRecordsSuccessfully() {
        List<Integer> input = List.of(1, 2, 3, 4, 5);
        List<Integer> processedItems = new ArrayList<>();
        AtomicInteger releaseCalls = new AtomicInteger();
        List<Long> chunkNotifications = new ArrayList<>();

        ProcessingResult result = ProcessingLoop.execute(
                input.iterator(),
                2,
                chunk -> {
                    chunk.forEach(processedItems::add);
                    return new ChunkExecutionResult(chunk.size(), 0, 0);
                },
                chunkNotifications::add,
                releaseCalls::incrementAndGet,
                (processed, totalRead) -> {
                }
        );

        assertEquals(5, result.processedCount());
        assertEquals(0, result.errorCount());
        assertEquals(0, result.retryCount());
        assertEquals(5, result.totalReadCount());
        assertEquals(List.of(1, 2, 3, 4, 5), processedItems);
        assertEquals(List.of(2L, 4L, 5L), chunkNotifications);
        assertEquals(3, releaseCalls.get());
        assertTrue(result.durationMillis() >= 0);
        assertTrue(result.throughputPerSecond() >= 0);
    }

    @Test
    void shouldAggregateChunkExecutionResults() {
        ProcessingResult result = ProcessingLoop.execute(
                List.of(1, 2, 3, 4, 5).iterator(),
                2,
                chunk -> {
                    if (chunk.equals(List.of(1, 2))) {
                        return new ChunkExecutionResult(2, 0, 1);
                    }
                    if (chunk.equals(List.of(3, 4))) {
                        return new ChunkExecutionResult(1, 1, 2);
                    }
                    return new ChunkExecutionResult(1, 0, 0);
                },
                processed -> {
                },
                () -> {
                },
                (processed, totalRead) -> {
                }
        );

        assertEquals(4, result.processedCount());
        assertEquals(1, result.errorCount());
        assertEquals(3, result.retryCount());
        assertEquals(5, result.totalReadCount());
    }

    @Test
    void shouldRejectInvalidChunkSize() {
        try {
            ProcessingLoop.execute(
                    List.of(1, 2, 3).iterator(),
                    0,
                    chunk -> ChunkExecutionResult.empty(),
                    processed -> {
                    },
                    () -> {
                    },
                    (processed, totalRead) -> {
                    }
            );
        } catch (IllegalArgumentException exception) {
            assertEquals("chunkSize must be greater than zero", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException");
    }

    @Test
    void shouldHandleEmptyInput() {
        AtomicInteger releaseCalls = new AtomicInteger();
        List<Long> chunkNotifications = new ArrayList<>();

        ProcessingResult result = ProcessingLoop.execute(
                List.<Integer>of().iterator(),
                3,
                chunk -> ChunkExecutionResult.empty(),
                chunkNotifications::add,
                releaseCalls::incrementAndGet,
                (processed, totalRead) -> {
                }
        );

        assertEquals(0, result.processedCount());
        assertEquals(0, result.errorCount());
        assertEquals(0, result.retryCount());
        assertEquals(0, result.totalReadCount());
        assertEquals(1, releaseCalls.get());
        assertTrue(chunkNotifications.isEmpty());
    }
}