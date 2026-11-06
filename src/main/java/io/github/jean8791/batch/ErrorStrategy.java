package io.github.jean8791.batch;

/**
 * What the processor does when a record still fails after all retry attempts.
 */
public enum ErrorStrategy {

    /**
     * Stops the run at the first failed record and throws {@link RecordProcessingException}.
     * With {@link TransactionMode#CHUNK}, the current chunk is rolled back.
     */
    FAIL_FAST,

    /**
     * Counts the failure, notifies the {@link RecordErrorHandler} and moves on to the next record.
     */
    CONTINUE_ON_ERROR
}
