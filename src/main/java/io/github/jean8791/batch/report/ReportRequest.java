package io.github.jean8791.batch.report;

/**
 * What to generate and where to write it.
 *
 * @param fileName     file name; the extension of the format is added when missing
 * @param format       file format
 * @param definition   report layout
 * @param outputTarget destination of the file
 */
public record ReportRequest(
        String fileName,
        ReportFormat format,
        ReportDefinition definition,
        ReportOutputTarget outputTarget
) {
}
