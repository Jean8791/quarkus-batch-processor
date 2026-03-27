package br.com.codenest.report.result;

import br.com.codenest.report.core.ReportFormat;

public record ReportResult(
        String fileName,
        String filePath,
        ReportFormat format,
        long rowCount
) {
}