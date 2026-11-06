package io.github.jean8791.batch.report;

/**
 * Details of a generated report.
 *
 * @param fileName file name, including the extension
 * @param filePath full path of the file for {@link LocalFileTarget}; {@code null} for other targets
 * @param format   file format
 * @param rowCount number of data rows written, excluding the header
 */
public record ReportResult(
        String fileName,
        String filePath,
        ReportFormat format,
        long rowCount
) {
}
