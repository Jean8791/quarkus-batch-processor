package br.com.codenest.engine;

import br.com.codenest.contract.ChunkListener;
import br.com.codenest.result.ProcessingResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class ProcessingLoop {

    private ProcessingLoop() {
    }

    static <T> ProcessingResult execute(
            Iterator<T> iterator,
            int chunkSize,
            Function<List<T>, ChunkExecutionResult> chunkProcessor,
            ChunkListener chunkListener,
            Runnable releaseReferences,
            BiConsumer<Long, Long> internalChunkObserver
    ) {
        Objects.requireNonNull(iterator, "iterator must not be null");
        Objects.requireNonNull(chunkProcessor, "chunkProcessor must not be null");
        Objects.requireNonNull(releaseReferences, "releaseReferences must not be null");
        Objects.requireNonNull(internalChunkObserver, "internalChunkObserver must not be null");

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than zero");
        }

        long startedAt = System.currentTimeMillis();
        long totalRead = 0;
        ChunkExecutionResult totalResult = ChunkExecutionResult.empty();
        List<T> buffer = new ArrayList<>(chunkSize);

        while (iterator.hasNext()) {
            buffer.add(iterator.next());
            totalRead++;

            if (buffer.size() == chunkSize) {
                totalResult = processChunk(
                        buffer,
                        totalResult,
                        chunkProcessor,
                        chunkListener,
                        releaseReferences,
                        internalChunkObserver,
                        totalRead
                );
                buffer = new ArrayList<>(chunkSize);
            }
        }

        if (!buffer.isEmpty()) {
            totalResult = processChunk(
                    buffer,
                    totalResult,
                    chunkProcessor,
                    chunkListener,
                    releaseReferences,
                    internalChunkObserver,
                    totalRead
            );
        } else if (totalRead == 0) {
            releaseReferences.run();
        }

        long finishedAt = System.currentTimeMillis();
        return new ProcessingResult(
                totalResult.processedCount(),
                totalResult.errorCount(),
                totalResult.retryCount(),
                startedAt,
                finishedAt
        );
    }

    private static <T> ChunkExecutionResult processChunk(
            List<T> chunk,
            ChunkExecutionResult totalResult,
            Function<List<T>, ChunkExecutionResult> chunkProcessor,
            ChunkListener chunkListener,
            Runnable releaseReferences,
            BiConsumer<Long, Long> internalChunkObserver,
            long totalRead
    ) {
        ChunkExecutionResult chunkResult = chunkProcessor.apply(chunk);
        ChunkExecutionResult updatedTotal = totalResult.add(chunkResult);

        releaseReferences.run();
        notifyChunk(chunkListener, updatedTotal.processedCount());
        internalChunkObserver.accept(updatedTotal.processedCount(), totalRead);

        return updatedTotal;
    }

    private static void notifyChunk(ChunkListener listener, long processed) {
        if (listener != null) {
            listener.onChunkComplete(processed);
        }
    }
}