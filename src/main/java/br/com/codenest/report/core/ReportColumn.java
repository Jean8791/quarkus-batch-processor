package br.com.codenest.report.core;

public record ReportColumn(
        String key,
        String header,
        ColumnType type,
        Integer width
) {
}