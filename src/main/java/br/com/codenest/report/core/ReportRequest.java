package br.com.codenest.report.core;

import br.com.codenest.report.contract.ReportOutputTarget;

public record ReportRequest(
        String fileName,
        ReportFormat format,
        ReportDefinition definition,
        ReportOutputTarget outputTarget
) {
}