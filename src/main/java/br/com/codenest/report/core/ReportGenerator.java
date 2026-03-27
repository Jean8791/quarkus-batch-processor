package br.com.codenest.report.core;

import br.com.codenest.report.result.ReportResult;

public interface ReportGenerator {
    ReportResult generate(ReportRequest request, ReportFiller filler);
}