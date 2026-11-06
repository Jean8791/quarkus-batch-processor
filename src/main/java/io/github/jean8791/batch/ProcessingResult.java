package io.github.jean8791.batch;

/**
 * Summary returned by {@link Processor#run()}.
 *
 * @param processedCount   records processed successfully
 * @param errorCount       records that failed after all retry attempts
 * @param retryCount       total number of retry attempts across all records
 * @param startedAtMillis  start time, in epoch milliseconds
 * @param finishedAtMillis end time, in epoch milliseconds
 */
public record ProcessingResult(
        long processedCount,
        long errorCount,
        long retryCount,
        long startedAtMillis,
        long finishedAtMillis
) {

    /**
     * @return elapsed time of the run, in milliseconds
     */
    public long durationMillis() {
        return finishedAtMillis - startedAtMillis;
    }

    /**
     * @return {@code true} if at least one record failed
     */
    public boolean hasErrors() {
        return errorCount > 0;
    }

    /**
     * @return records read from the database, successful or not
     */
    public long totalReadCount() {
        return processedCount + errorCount;
    }

    /**
     * @return records read per second; when the run took less than a millisecond,
     * the total read count
     */
    public double throughputPerSecond() {
        long duration = durationMillis();
        if (duration <= 0) {
            return totalReadCount();
        }
        return (totalReadCount() * 1000.0) / duration;
    }
}
