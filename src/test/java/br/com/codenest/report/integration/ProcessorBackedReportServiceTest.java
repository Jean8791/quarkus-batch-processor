package br.com.codenest.report.integration;

import br.com.codenest.config.ErrorStrategy;
import br.com.codenest.config.RetryStrategy;
import br.com.codenest.config.TransactionMode;
import br.com.codenest.contract.ChunkListener;
import br.com.codenest.contract.Processor;
import br.com.codenest.contract.RecordErrorHandler;
import br.com.codenest.contract.RowMapper;
import br.com.codenest.engine.FastRecordProcessor;
import br.com.codenest.report.core.ColumnType;
import br.com.codenest.report.core.ReportColumn;
import br.com.codenest.report.core.ReportDefinition;
import br.com.codenest.report.core.ReportFormat;
import br.com.codenest.report.core.ReportRequest;
import br.com.codenest.report.result.ReportResult;
import br.com.codenest.report.targets.LocalFileTarget;
import br.com.codenest.result.ProcessingResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessorBackedReportServiceTest {

    @Test
    void shouldGenerateCsvUsingProcessorPipeline() throws Exception {
        StubFastRecordProcessor processor = new StubFastRecordProcessor(List.of(
                new TestRow("1", "alpha"),
                new TestRow("2", "beta")
        ));
        ProcessorBackedReportService reportService = new ProcessorBackedReportService(processor);

        Path tempDir = Files.createTempDirectory("processor-backed-report-");
        ReportRequest request = new ReportRequest(
                "rows",
                ReportFormat.CSV,
                new ReportDefinition(
                        "Rows",
                        null,
                        List.of(
                                new ReportColumn("id", "ID", ColumnType.STRING, 10),
                                new ReportColumn("value", "VALUE", ColumnType.STRING, 20)
                        )
                ),
                new LocalFileTarget(tempDir.toString())
        );

        ReportResult result = reportService.generate(
                TestRow.class,
                "from TestRow order by id",
                25,
                request,
                row -> List.of(row.id(), row.value())
        );

        assertEquals("from TestRow order by id", processor.lastQuery);
        assertEquals(25, processor.lastChunkSize);
        assertEquals(2, result.rowCount());
        assertNotNull(result.filePath());

        Path generatedFile = Path.of(result.filePath());
        assertTrue(Files.exists(generatedFile));
        assertEquals(
                List.of("ID,VALUE", "1,alpha", "2,beta"),
                Files.readAllLines(generatedFile)
        );
    }

    private record TestRow(String id, String value) {
    }

    private static final class StubFastRecordProcessor extends FastRecordProcessor {

        private final List<?> rows;
        private String lastQuery;
        private int lastChunkSize;

        private StubFastRecordProcessor(List<?> rows) {
            super();
            this.rows = rows;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Processor<T> source(Class<T> entityClass) {
            return new StubProcessor<>((List<T>) rows, this);
        }
    }

    private static final class StubProcessor<T> implements Processor<T> {

        private final List<T> rows;
        private final StubFastRecordProcessor owner;
        private Consumer<T> consumer;

        private StubProcessor(List<T> rows, StubFastRecordProcessor owner) {
            this.rows = rows;
            this.owner = owner;
        }

        @Override
        public Processor<T> query(String hql) {
            owner.lastQuery = hql;
            return this;
        }

        @Override
        public Processor<T> nativeQuery(String sql) {
            owner.lastQuery = sql;
            return this;
        }

        @Override
        public Processor<T> rowMapper(RowMapper<T> rowMapper) {
            return this;
        }

        @Override
        public Processor<T> params(java.util.Map<String, Object> params) {
            return this;
        }

        @Override
        public Processor<T> chunkSize(int chunkSize) {
            owner.lastChunkSize = chunkSize;
            return this;
        }

        @Override
        public Processor<T> transactionMode(TransactionMode transactionMode) {
            return this;
        }

        @Override
        public Processor<T> onProcess(Consumer<T> consumer) {
            this.consumer = consumer;
            return this;
        }

        @Override
        public Processor<T> onChunkComplete(ChunkListener listener) {
            return this;
        }

        @Override
        public Processor<T> onError(ErrorStrategy errorStrategy) {
            return this;
        }

        @Override
        public Processor<T> onRecordError(RecordErrorHandler<T> errorHandler) {
            return this;
        }

        @Override
        public Processor<T> retryStrategy(RetryStrategy retryStrategy) {
            return this;
        }

        @Override
        public Processor<T> loggingEnabled(boolean enabled) {
            return this;
        }

        @Override
        public Processor<T> progressLogEveryChunks(int everyChunks) {
            return this;
        }

        @Override
        public Processor<T> processorName(String processorName) {
            return this;
        }

        @Override
        public ProcessingResult run() {
            for (T row : rows) {
                consumer.accept(row);
            }
            return new ProcessingResult(rows.size(), 0, 0, 0, 1);
        }
    }
}
