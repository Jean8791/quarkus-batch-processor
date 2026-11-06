package io.github.jean8791.batch;

/**
 * Callback invoked after every chunk, useful for progress bars or custom metrics.
 */
@FunctionalInterface
public interface ChunkListener {

    /**
     * Called after a chunk has been processed.
     *
     * @param processedCount total number of records processed successfully so far
     */
    void onChunkComplete(long processedCount);
}
