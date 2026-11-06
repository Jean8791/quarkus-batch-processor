package io.github.jean8791.batch;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Fluent definition of a batch run, obtained from {@link FastRecordProcessor#source(Class)}.
 *
 * <p>Records are read through a forward-only cursor on a Hibernate {@code StatelessSession},
 * grouped into chunks and handed one by one to the {@link #onProcess(Consumer) consumer}.
 * Only one chunk is kept in memory at a time.
 *
 * <pre>{@code
 * ProcessingResult result = processor.source(Invoice.class)
 *         .query("select i from Invoice i where i.status = :status order by i.id")
 *         .params(Map.of("status", Status.OPEN))
 *         .chunkSize(500)
 *         .transactionMode(TransactionMode.CHUNK)
 *         .retryStrategy(RetryStrategy.fixedDelay(3, 200, OptimisticLockException.class))
 *         .onError(ErrorStrategy.CONTINUE_ON_ERROR)
 *         .onProcess(invoiceService::recalculate)
 *         .run();
 * }</pre>
 *
 * <p>A {@code Processor} is not thread-safe and is meant to be configured and run once.
 *
 * @param <T> record type
 */
public interface Processor<T> {

    /**
     * Reads the records with an HQL/JPQL query. When neither this nor {@link #nativeQuery(String)}
     * is set, every row of the entity is read.
     *
     * @param hql query returning {@code T}
     * @return this processor
     */
    Processor<T> query(String hql);

    /**
     * Reads the records with a native SQL query. Requires a {@link #rowMapper(RowMapper)}.
     *
     * @param sql native query
     * @return this processor
     */
    Processor<T> nativeQuery(String sql);

    /**
     * Converts each native query row into {@code T}.
     *
     * @param rowMapper row converter
     * @return this processor
     */
    Processor<T> rowMapper(RowMapper<T> rowMapper);

    /**
     * Named parameters bound to the query.
     *
     * @param params parameter values by name; {@code null} clears them
     * @return this processor
     */
    Processor<T> params(Map<String, Object> params);

    /**
     * Number of records per chunk, also used as the JDBC fetch size. Defaults to 1000.
     *
     * @param chunkSize a positive number
     * @return this processor
     */
    Processor<T> chunkSize(int chunkSize);

    /**
     * Transaction boundary for each chunk. Defaults to {@link TransactionMode#NONE}.
     *
     * @param transactionMode the mode
     * @return this processor
     */
    Processor<T> transactionMode(TransactionMode transactionMode);

    /**
     * Work done for each record. Required.
     *
     * @param consumer record handler
     * @return this processor
     */
    Processor<T> onProcess(Consumer<T> consumer);

    /**
     * Callback invoked after each chunk.
     *
     * @param listener the listener, or {@code null} to remove it
     * @return this processor
     */
    Processor<T> onChunkComplete(ChunkListener listener);

    /**
     * What to do when a record fails after all retries. Defaults to {@link ErrorStrategy#FAIL_FAST}.
     *
     * @param errorStrategy the strategy
     * @return this processor
     */
    Processor<T> onError(ErrorStrategy errorStrategy);

    /**
     * Callback invoked for each record that fails after all retries.
     *
     * @param errorHandler the handler, or {@code null} to remove it
     * @return this processor
     */
    Processor<T> onRecordError(RecordErrorHandler<T> errorHandler);

    /**
     * Retry policy for failing records. Defaults to {@link RetryStrategy#noRetry()}.
     *
     * @param retryStrategy the policy
     * @return this processor
     */
    Processor<T> retryStrategy(RetryStrategy retryStrategy);

    /**
     * Turns the start, progress and finish log lines on or off. Enabled by default.
     *
     * @param enabled {@code true} to log
     * @return this processor
     */
    Processor<T> loggingEnabled(boolean enabled);

    /**
     * Logs progress every N chunks instead of after every chunk.
     *
     * @param everyChunks a positive number
     * @return this processor
     */
    Processor<T> progressLogEveryChunks(int everyChunks);

    /**
     * Name shown in the log lines. Defaults to the entity name followed by {@code Processor}.
     *
     * @param processorName a non-blank name
     * @return this processor
     */
    Processor<T> processorName(String processorName);

    /**
     * Executes the run and blocks until every record has been read.
     *
     * @return the summary of the run
     * @throws IllegalStateException     if the configuration is incomplete or, with
     *                                   {@link TransactionMode#CHUNK}, a transaction is already active
     * @throws RecordProcessingException if the run is interrupted by a failure
     */
    ProcessingResult run();
}
