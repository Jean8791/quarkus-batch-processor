package io.github.jean8791.batch.report;

import java.util.List;

/**
 * Layout of a report.
 *
 * @param reportName name of the report
 * @param sheetName  worksheet name for XLSX; defaults to {@code Report} when blank
 * @param columns    columns in output order
 */
public record ReportDefinition(
        String reportName,
        String sheetName,
        List<ReportColumn> columns
) {
}
