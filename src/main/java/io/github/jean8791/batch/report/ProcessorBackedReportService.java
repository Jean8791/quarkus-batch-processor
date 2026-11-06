package io.github.jean8791.batch.report;

import io.github.jean8791.batch.FastRecordProcessor;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Exports the result of an HQL query to CSV or XLSX, reading it in chunks so that
 * large tables never sit in memory.
 *
 * <pre>{@code
 * ReportResult result = reports.generate(
 *         Customer.class,
 *         "select c from Customer c order by c.id",
 *         1000,
 *         new ReportRequest("customers", ReportFormat.XLSX, definition, new LocalFileTarget("/tmp/reports")),
 *         c -> List.of(c.getId(), c.getName(), c.getEmail()));
 * }</pre>
 */
public class ProcessorBackedReportService {

    private final FastRecordProcessor processor;
    private final ReportGenerator reportGenerator;

    /**
     * Creates the service with the default generator.
     *
     * @param processor source of the records
     */
    public ProcessorBackedReportService(FastRecordProcessor processor) {
        this(processor, new DefaultReportGenerator(new ReportWriterFactory()));
    }

    /**
     * Creates the service with a custom generator.
     *
     * @param processor       source of the records
     * @param reportGenerator report generator
     */
    public ProcessorBackedReportService(FastRecordProcessor processor, ReportGenerator reportGenerator) {
        this.processor = processor;
        this.reportGenerator = reportGenerator;
    }

    /**
     * Generates the report in the format set in the request.
     *
     * @param entityClass entity read by the query
     * @param hql         query returning {@code entityClass}
     * @param chunkSize   records per chunk
     * @param request     where and how to write the report
     * @param rowMapper   converts each record into the cell values of one row
     * @param <T>         entity type
     * @return details of the generated file
     */
    public <T> ReportResult generate(
            Class<T> entityClass,
            String hql,
            int chunkSize,
            ReportRequest request,
            Function<T, List<?>> rowMapper
    ) {
        Objects.requireNonNull(entityClass, "entityClass must not be null");
        Objects.requireNonNull(hql, "hql must not be null");
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(rowMapper, "rowMapper must not be null");

        return reportGenerator.generate(request, writer -> {
            long[] rowCount = {0};

            processor.source(entityClass)
                    .query(hql)
                    .chunkSize(chunkSize)
                    .onProcess(item -> {
                        writer.writeRow(rowMapper.apply(item));
                        rowCount[0]++;
                    })
                    .run();

            return rowCount[0];
        });
    }
}
