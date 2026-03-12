package br.com.codenest.engine;

record ChunkExecutionResult(
        long processedCount,
        long errorCount,
        long retryCount
) {
    static ChunkExecutionResult empty() {
        return new ChunkExecutionResult(0, 0, 0);
    }

    ChunkExecutionResult add(ChunkExecutionResult other) {
        return new ChunkExecutionResult(
                processedCount + other.processedCount,
                errorCount + other.errorCount,
                retryCount + other.retryCount
        );
    }
}