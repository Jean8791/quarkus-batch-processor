package io.github.jean8791.batch.report;

import java.io.OutputStream;

/**
 * Creates the {@link ReportWriter} for each {@link ReportFormat}.
 */
public class ReportWriterFactory {

    /**
     * Creates a writer.
     *
     * @param format       file format
     * @param outputStream destination stream
     * @return a writer that is not started yet
     */
    public ReportWriter create(ReportFormat format, OutputStream outputStream) {
        return switch (format) {
            case CSV -> new CsvReportWriter(outputStream);
            case XLSX -> new XlsxReportWriter(outputStream);
        };
    }
}
