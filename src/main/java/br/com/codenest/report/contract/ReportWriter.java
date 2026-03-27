package br.com.codenest.report.contract;

import br.com.codenest.report.core.ReportDefinition;

import java.util.List;

public interface ReportWriter extends AutoCloseable {

    void start(ReportDefinition definition);

    void writeRow(List<?> values);

    void flush();

    void finish();

    @Override
    void close();
}