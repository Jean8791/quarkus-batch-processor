package io.github.jean8791.batch.report;

import java.util.List;

/**
 * Writes report rows in a specific file format, one row at a time.
 */
public interface ReportWriter extends AutoCloseable {

    /**
     * Writes the header. Must be called before any row.
     *
     * @param definition columns and names of the report
     */
    void start(ReportDefinition definition);

    /**
     * Writes one row.
     *
     * @param values cell values in column order; {@code null} produces an empty cell
     */
    void writeRow(List<?> values);

    /**
     * Pushes buffered data to the output, when the format allows it.
     */
    void flush();

    /**
     * Completes the file. Must be called after the last row.
     */
    void finish();

    /**
     * Releases resources and closes the output stream.
     */
    @Override
    void close();
}
