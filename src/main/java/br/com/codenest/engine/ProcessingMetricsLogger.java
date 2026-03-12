package br.com.codenest.engine;

import br.com.codenest.result.ProcessingResult;
import org.jboss.logging.Logger;

final class ProcessingMetricsLogger {

    private static final Logger LOG = Logger.getLogger(ProcessingMetricsLogger.class);

    private ProcessingMetricsLogger() {
    }

    static void logStart(String processorName, String entityName, int chunkSize, String hql) {
        LOG.infof(
                "FastRecord started | processor=%s | entity=%s | chunkSize=%d | hql=%s",
                processorName,
                entityName,
                chunkSize,
                hql
        );
    }

    static void logChunkProgress(String processorName, String entityName, long processedCount, long totalReadCount) {
        LOG.infof(
                "FastRecord chunk completed | processor=%s | entity=%s | processed=%d | totalRead=%d",
                processorName,
                entityName,
                processedCount,
                totalReadCount
        );
    }

    static void logFinish(String processorName, String entityName, ProcessingResult result) {
        LOG.infof(
                "FastRecord finished | processor=%s | entity=%s | processed=%d | errors=%d | retries=%d | totalRead=%d | durationMs=%d | throughput=%.2f records/s",
                processorName,
                entityName,
                result.processedCount(),
                result.errorCount(),
                result.retryCount(),
                result.totalReadCount(),
                result.durationMillis(),
                result.throughputPerSecond()
        );
    }

    static void logFailure(String processorName, String entityName, Throwable throwable) {
        LOG.errorf(
                throwable,
                "FastRecord failed | processor=%s | entity=%s | message=%s",
                processorName,
                entityName,
                throwable.getMessage()
        );
    }
}