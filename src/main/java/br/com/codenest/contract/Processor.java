package br.com.codenest.contract;

import br.com.codenest.config.ErrorStrategy;
import br.com.codenest.config.RetryStrategy;
import br.com.codenest.config.TransactionMode;
import br.com.codenest.result.ProcessingResult;

import java.util.Map;
import java.util.function.Consumer;

public interface Processor<T> {

    Processor<T> query(String hql);

    Processor<T> nativeQuery(String sql);

    Processor<T> rowMapper(RowMapper<T> rowMapper);

    Processor<T> params(Map<String, Object> params);

    Processor<T> chunkSize(int chunkSize);

    Processor<T> transactionMode(TransactionMode transactionMode);

    Processor<T> onProcess(Consumer<T> consumer);

    Processor<T> onChunkComplete(ChunkListener listener);

    Processor<T> onError(ErrorStrategy errorStrategy);

    Processor<T> onRecordError(RecordErrorHandler<T> errorHandler);

    Processor<T> retryStrategy(RetryStrategy retryStrategy);

    Processor<T> loggingEnabled(boolean enabled);

    Processor<T> progressLogEveryChunks(int everyChunks);

    Processor<T> processorName(String processorName);

    ProcessingResult run();
}