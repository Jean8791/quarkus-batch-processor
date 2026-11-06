package io.github.jean8791.batch.report;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Destination of a generated report, such as a local folder or object storage.
 */
@FunctionalInterface
public interface ReportOutputTarget {

    /**
     * Opens the stream the report is written to. The generator closes it when done.
     *
     * @param fileName file name, including the extension of the format
     * @return an open output stream
     * @throws IOException if the destination cannot be opened
     */
    OutputStream open(String fileName) throws IOException;
}
