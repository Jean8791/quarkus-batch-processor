package io.github.jean8791.batch.report;


import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes CSV files in UTF-8, quoting values that contain commas, quotes or line breaks.
 */
public class CsvReportWriter implements ReportWriter {

    private final BufferedWriter writer;
    private boolean started;

    /**
     * Creates the writer.
     *
     * @param outputStream destination stream
     */
    public CsvReportWriter(OutputStream outputStream) {
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    @Override
    public void start(ReportDefinition definition) {
        try {
            List<String> headers = definition.columns().stream()
                    .map(ReportColumn::header)
                    .toList();

            writeLine(headers);
            started = true;
        } catch (Exception e) {
            throw new ReportWriterException("Could not start CSV", e);
        }
    }

    @Override
    public void writeRow(List<?> values) {
        ensureStarted();

        try {
            List<String> row = values.stream()
                    .map(this::formatValue)
                    .toList();

            writeLine(row);
        } catch (Exception e) {
            throw new ReportWriterException("Could not write CSV row", e);
        }
    }

    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (Exception e) {
            throw new ReportWriterException("Could not flush CSV", e);
        }
    }

    @Override
    public void finish() {
        flush();
    }

    @Override
    public void close() {
        try {
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new ReportWriterException("Could not close CSV", e);
        }
    }

    private void writeLine(List<String> values) throws Exception {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(escape(values.get(i)));
        }

        writer.write(sb.toString());
        writer.newLine();
    }

    private String formatValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String escape(String value) {
        if (value == null) return "";

        boolean needsQuotes = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");

        String escaped = value.replace("\"", "\"\"");

        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private void ensureStarted() {
        if (!started) {
            throw new ReportWriterException("CSV writer was not started");
        }
    }
}