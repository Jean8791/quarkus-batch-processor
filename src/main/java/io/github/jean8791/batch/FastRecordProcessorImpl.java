package io.github.jean8791.batch;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

final class FastRecordProcessorImpl<T> implements Processor<T> {

    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_PROGRESS_LOG_EVERY_CHUNKS = 1;

    private final EntityManager entityManager;
    private final UserTransaction userTransaction;
    private final Class<T> entityClass;

    private String hql;
    private String nativeSql;
    private RowMapper<T> rowMapper;
    private Map<String, Object> params = Collections.emptyMap();
    private int chunkSize = DEFAULT_CHUNK_SIZE;
    private TransactionMode transactionMode = TransactionMode.NONE;
    private Consumer<T> consumer;
    private ChunkListener chunkListener;
    private ErrorStrategy errorStrategy = ErrorStrategy.FAIL_FAST;
    private RecordErrorHandler<T> recordErrorHandler;
    private RetryStrategy retryStrategy = RetryStrategy.noRetry();
    private boolean loggingEnabled = true;
    private int progressLogEveryChunks = DEFAULT_PROGRESS_LOG_EVERY_CHUNKS;
    private String processorName;

    FastRecordProcessorImpl(EntityManager entityManager, UserTransaction userTransaction, Class<T> entityClass) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
        this.userTransaction = Objects.requireNonNull(userTransaction, "userTransaction must not be null");
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
    }

    @Override
    public Processor<T> query(String hql) {
        this.hql = Objects.requireNonNull(hql, "hql must not be null");
        this.nativeSql = null;
        return this;
    }

    @Override
    public Processor<T> nativeQuery(String sql) {
        this.nativeSql = Objects.requireNonNull(sql, "sql must not be null");
        this.hql = null;
        return this;
    }

    @Override
    public Processor<T> rowMapper(RowMapper<T> rowMapper) {
        this.rowMapper = Objects.requireNonNull(rowMapper, "rowMapper must not be null");
        return this;
    }

    @Override
    public Processor<T> params(Map<String, Object> params) {
        this.params = params == null ? Collections.emptyMap() : Map.copyOf(params);
        return this;
    }

    @Override
    public Processor<T> chunkSize(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than zero");
        }
        this.chunkSize = chunkSize;
        return this;
    }

    @Override
    public Processor<T> transactionMode(TransactionMode transactionMode) {
        this.transactionMode = Objects.requireNonNull(transactionMode, "transactionMode must not be null");
        return this;
    }

    @Override
    public Processor<T> onProcess(Consumer<T> consumer) {
        this.consumer = Objects.requireNonNull(consumer, "consumer must not be null");
        return this;
    }

    @Override
    public Processor<T> onChunkComplete(ChunkListener listener) {
        this.chunkListener = listener;
        return this;
    }

    @Override
    public Processor<T> onError(ErrorStrategy errorStrategy) {
        this.errorStrategy = Objects.requireNonNull(errorStrategy, "errorStrategy must not be null");
        return this;
    }

    @Override
    public Processor<T> onRecordError(RecordErrorHandler<T> errorHandler) {
        this.recordErrorHandler = errorHandler;
        return this;
    }

    @Override
    public Processor<T> retryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = Objects.requireNonNull(retryStrategy, "retryStrategy must not be null");
        return this;
    }

    @Override
    public Processor<T> loggingEnabled(boolean enabled) {
        this.loggingEnabled = enabled;
        return this;
    }

    @Override
    public Processor<T> progressLogEveryChunks(int everyChunks) {
        if (everyChunks <= 0) {
            throw new IllegalArgumentException("progressLogEveryChunks must be greater than zero");
        }
        this.progressLogEveryChunks = everyChunks;
        return this;
    }

    @Override
    public Processor<T> processorName(String processorName) {
        if (processorName == null || processorName.isBlank()) {
            throw new IllegalArgumentException("processorName must not be blank");
        }
        this.processorName = processorName;
        return this;
    }

    @Override
    public ProcessingResult run() {
        ProcessorConfig<T> config = validateAndBuildConfig();
        if (config.transactionMode() == TransactionMode.CHUNK) {
            ensureNoActiveTransaction();
        }

        if (config.loggingEnabled()) {
            ProcessingMetricsLogger.logStart(
                    config.processorName(),
                    config.entityClass().getSimpleName(),
                    config.chunkSize(),
                    config.statementDescription()
            );
        }

        SessionFactory sessionFactory = entityManager
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class);

        try (StatelessSession statelessSession = sessionFactory.openStatelessSession();
             CursorIterator<T> iterator = config.nativeMode()
                     ? buildNativeIterator(statelessSession, config)
                     : buildHqlIterator(statelessSession, config)) {

            ProcessingResult result = ProcessingLoop.execute(
                    iterator,
                    config.chunkSize(),
                    chunk -> processChunk(config, chunk),
                    config.chunkListener(),
                    this::releaseReferences,
                    new ChunkLogObserver(config)
            );

            if (config.loggingEnabled()) {
                ProcessingMetricsLogger.logFinish(
                        config.processorName(),
                        config.entityClass().getSimpleName(),
                        result
                );
            }

            return result;
        } catch (RecordProcessingException ex) {
            if (config.loggingEnabled()) {
                ProcessingMetricsLogger.logFailure(
                        config.processorName(),
                        config.entityClass().getSimpleName(),
                        ex
                );
            }
            throw ex;
        } catch (Exception ex) {
            if (config.loggingEnabled()) {
                ProcessingMetricsLogger.logFailure(
                        config.processorName(),
                        config.entityClass().getSimpleName(),
                        ex
                );
            }
            throw new RecordProcessingException("Unexpected error while processing records", ex);
        }
    }

    private CursorIterator<T> buildHqlIterator(StatelessSession statelessSession, ProcessorConfig<T> config) {
        Query<T> query = statelessSession.createQuery(config.hql(), config.entityClass());
        query.setFetchSize(config.chunkSize());
        applyParameters(query, config.params());

        return new CursorIterator<>(query.scroll(ScrollMode.FORWARD_ONLY), Function.identity());
    }

    private CursorIterator<T> buildNativeIterator(StatelessSession statelessSession, ProcessorConfig<T> config) {
        NativeQuery<?> query = statelessSession.createNativeQuery(config.nativeSql());
        query.setFetchSize(config.chunkSize());
        applyParameters(query, config.params());

        RowMapper<T> rowMapper = config.rowMapper();
        return new CursorIterator<>(query.scroll(ScrollMode.FORWARD_ONLY),
                raw -> rowMapper.map(raw instanceof Object[] row ? row : new Object[]{raw}));
    }

    private ChunkExecutionResult processChunk(ProcessorConfig<T> config, List<T> chunk) {
        return switch (config.transactionMode()) {
            case NONE -> processChunkWithoutTransaction(config, chunk);
            case CHUNK -> processChunkInTransaction(config, chunk);
        };
    }

    private ChunkExecutionResult processChunkWithoutTransaction(ProcessorConfig<T> config, List<T> chunk) {
        return processRecords(config, chunk);
    }

    private ChunkExecutionResult processChunkInTransaction(ProcessorConfig<T> config, List<T> chunk) {
        try {
            beginTransaction();
            ChunkExecutionResult result = processRecords(config, chunk);
            commitTransaction();
            return result;
        } catch (Exception ex) {
            RecordProcessingException failure = ex instanceof RecordProcessingException recordProcessingException
                    ? recordProcessingException
                    : new RecordProcessingException("Error processing transactional chunk", ex);
            rollbackTransactionQuietly(failure);
            throw failure;
        }
    }

    private ChunkExecutionResult processRecords(ProcessorConfig<T> config, List<T> chunk) {
        long processed = 0;
        long errors = 0;
        long retries = 0;

        for (T entity : chunk) {
            try {
                retries += executeWithRetry(entity, config.consumer(), config.retryStrategy());
                processed++;
            } catch (Exception ex) {
                errors++;
                notifyRecordError(config.recordErrorHandler(), entity, ex);

                if (config.errorStrategy() == ErrorStrategy.FAIL_FAST) {
                    throw new RecordProcessingException(
                            "Error processing entity: " + config.entityClass().getSimpleName(),
                            ex
                    );
                }
            }
        }

        return new ChunkExecutionResult(processed, errors, retries);
    }

    private long executeWithRetry(T entity, Consumer<T> consumer, RetryStrategy retryStrategy) {
        int attempt = 1;
        long retries = 0;

        while (true) {
            try {
                consumer.accept(entity);
                return retries;
            } catch (Exception ex) {
                if (!retryStrategy.shouldRetry(ex, attempt)) {
                    throw ex;
                }

                retries++;
                sleep(retryStrategy.delayMillis());
                attempt++;
            }
        }
    }

    private void notifyRecordError(RecordErrorHandler<T> errorHandler, T entity, Exception exception) {
        if (errorHandler != null) {
            errorHandler.onError(entity, exception);
        }
    }

    private void sleep(long delayMillis) {
        if (delayMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RecordProcessingException("Thread interrupted during retry delay", ex);
        }
    }

    private void ensureNoActiveTransaction() {
        int status;
        try {
            status = userTransaction.getStatus();
        } catch (Exception ex) {
            throw new RecordProcessingException("Could not read the transaction status", ex);
        }
        if (status != Status.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException(
                    "TransactionMode.CHUNK manages its own transactions; call run() outside of a transaction");
        }
    }

    private void beginTransaction() {
        try {
            userTransaction.begin();
        } catch (Exception ex) {
            throw new RecordProcessingException("Could not begin chunk transaction", ex);
        }
    }

    private void commitTransaction() {
        try {
            userTransaction.commit();
        } catch (Exception ex) {
            throw new RecordProcessingException("Could not commit chunk transaction", ex);
        }
    }

    private void rollbackTransactionQuietly(RecordProcessingException failure) {
        try {
            int status = userTransaction.getStatus();
            if (status != Status.STATUS_NO_TRANSACTION) {
                userTransaction.rollback();
            }
        } catch (Exception ex) {
            failure.addSuppressed(ex);
        }
    }

    private ProcessorConfig<T> validateAndBuildConfig() {
        if (consumer == null) {
            throw new IllegalStateException("Processing consumer must be configured before run()");
        }

        boolean nativeMode = nativeSql != null && !nativeSql.isBlank();
        boolean hqlMode = hql != null && !hql.isBlank();

        if (nativeMode && hqlMode) {
            throw new IllegalStateException("Configure either query(...) or nativeQuery(...), not both");
        }

        if (nativeMode && rowMapper == null) {
            throw new IllegalStateException("rowMapper(...) must be configured when using nativeQuery(...)");
        }

        String resolvedHql = !nativeMode
                ? (hqlMode ? hql : "select e from " + entityClass.getSimpleName() + " e")
                : null;

        String resolvedProcessorName = processorName != null && !processorName.isBlank()
                ? processorName
                : entityClass.getSimpleName() + "Processor";

        String resolvedStatementDescription = nativeMode
                ? nativeSql
                : resolvedHql;

        return new ProcessorConfig<>(
                entityClass,
                resolvedHql,
                nativeSql,
                nativeMode,
                rowMapper,
                params,
                chunkSize,
                transactionMode,
                consumer,
                chunkListener,
                errorStrategy,
                recordErrorHandler,
                retryStrategy,
                loggingEnabled,
                progressLogEveryChunks,
                resolvedProcessorName,
                resolvedStatementDescription
        );
    }

    private void applyParameters(Query<?> query, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
    }

    private void releaseReferences() {
        entityManager.clear();
    }

    private record ProcessorConfig<T>(
            Class<T> entityClass,
            String hql,
            String nativeSql,
            boolean nativeMode,
            RowMapper<T> rowMapper,
            Map<String, Object> params,
            int chunkSize,
            TransactionMode transactionMode,
            Consumer<T> consumer,
            ChunkListener chunkListener,
            ErrorStrategy errorStrategy,
            RecordErrorHandler<T> recordErrorHandler,
            RetryStrategy retryStrategy,
            boolean loggingEnabled,
            int progressLogEveryChunks,
            String processorName,
            String statementDescription
    ) {
    }

    private final class ChunkLogObserver implements BiConsumer<Long, Long> {

        private final ProcessorConfig<T> config;
        private long chunkCounter;

        private ChunkLogObserver(ProcessorConfig<T> config) {
            this.config = config;
        }

        @Override
        public void accept(Long processed, Long totalRead) {
            chunkCounter++;

            if (!config.loggingEnabled() || chunkCounter % config.progressLogEveryChunks() != 0) {
                return;
            }

            ProcessingMetricsLogger.logChunkProgress(
                    config.processorName(),
                    config.entityClass().getSimpleName(),
                    processed,
                    totalRead
            );
        }
    }

    private static final class CursorIterator<T> implements Iterator<T>, AutoCloseable {

        private final ScrollableResults<?> results;
        private final Function<Object, T> mapper;
        private boolean prepared;
        private boolean hasNext;
        private boolean closed;

        @SuppressWarnings("unchecked")
        private <R> CursorIterator(ScrollableResults<R> results, Function<? super R, T> mapper) {
            this.results = results;
            this.mapper = raw -> mapper.apply((R) raw);
        }

        @Override
        public boolean hasNext() {
            if (closed) {
                return false;
            }
            if (!prepared) {
                hasNext = results.next();
                prepared = true;
                if (!hasNext) {
                    close();
                }
            }
            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            prepared = false;
            return mapper.apply(results.get());
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                results.close();
            }
        }
    }
}
