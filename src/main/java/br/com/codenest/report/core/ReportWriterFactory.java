package br.com.codenest.report.core;

import br.com.codenest.report.contract.ReportWriter;
import br.com.codenest.report.csv.CsvReportWriter;
import br.com.codenest.report.xlsx.XlsxReportWriter;

import java.io.OutputStream;

public class ReportWriterFactory {

    public ReportWriter create(ReportFormat format, OutputStream outputStream) {
        return switch (format) {
            case CSV -> new CsvReportWriter(outputStream);
            case XLSX -> new XlsxReportWriter(outputStream);
        };
    }
}