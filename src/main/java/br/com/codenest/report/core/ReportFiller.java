package br.com.codenest.report.core;

import br.com.codenest.report.contract.ReportWriter;

@FunctionalInterface
public interface ReportFiller {
    long fill(ReportWriter writer);
}