package io.github.jean8791.batch;

/**
 * Callback invoked when a record fails after all retry attempts.
 *
 * @param <T> record type
 */
@FunctionalInterface
public interface RecordErrorHandler<T> {

    /**
     * Called once for each failed record, before the {@link ErrorStrategy} is applied.
     *
     * @param record    the record that failed
     * @param exception the last exception thrown while processing it
     */
    void onError(T record, Exception exception);
}
