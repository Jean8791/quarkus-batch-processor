package br.com.codenest.report.core;

import java.util.List;

public record ReportDefinition(
        String reportName,
        String sheetName,
        List<ReportColumn> columns
) {
}