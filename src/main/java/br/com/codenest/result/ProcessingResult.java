package br.com.codenest.result;

public record ProcessingResult(
        long processedCount,
        long errorCount,
        long retryCount,
        long startedAtMillis,
        long finishedAtMillis
) {
    public long durationMillis() {
        return finishedAtMillis - startedAtMillis;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }

    public long totalReadCount() {
        return processedCount + errorCount;
    }

    public double throughputPerSecond() {
        long duration = durationMillis();
        if (duration <= 0) {
            return totalReadCount();
        }
        return (totalReadCount() * 1000.0) / duration;
    }
}