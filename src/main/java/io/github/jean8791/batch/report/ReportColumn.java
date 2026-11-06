package io.github.jean8791.batch.report;

/**
 * One column of a report.
 *
 * @param key    identifier of the column
 * @param header text shown in the header row
 * @param type   declared type of the values
 * @param width  column width in characters, used by XLSX; {@code null} keeps the default
 */
public record ReportColumn(
        String key,
        String header,
        ColumnType type,
        Integer width
) {
}
