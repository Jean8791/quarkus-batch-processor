package br.com.codenest.report.integration;

import br.com.codenest.engine.FastRecordProcessor;
import br.com.codenest.report.core.DefaultReportGenerator;
import br.com.codenest.report.core.ReportGenerator;
import br.com.codenest.report.core.ReportRequest;
import br.com.codenest.report.core.ReportWriterFactory;
import br.com.codenest.report.result.ReportResult;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class ProcessorBackedReportService {

    private final FastRecordProcessor processor;
    private final ReportGenerator reportGenerator;

    public ProcessorBackedReportService(FastRecordProcessor processor) {
        this(processor, new DefaultReportGenerator(new ReportWriterFactory()));
    }

    public ProcessorBackedReportService(FastRecordProcessor processor, ReportGenerator reportGenerator) {
        this.processor = processor;
        this.reportGenerator = reportGenerator;
    }

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
            final long[] rowCount = {0};

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

    public <T> ReportResult generateCsv(
            Class<T> entityClass,
            String hql,
            int chunkSize,
            ReportRequest request,
            Function<T, List<?>> rowMapper
    ) {
        return generate(entityClass, hql, chunkSize, request, rowMapper);
    }
}
