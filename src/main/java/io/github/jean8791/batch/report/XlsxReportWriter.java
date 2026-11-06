package io.github.jean8791.batch.report;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Writes Excel workbooks with Apache POI in streaming mode, keeping only 100 rows in memory.
 * Numbers and booleans become typed cells; dates are written as ISO-8601 text.
 */
public class XlsxReportWriter implements ReportWriter {

    private final OutputStream outputStream;
    private SXSSFWorkbook workbook;
    private Sheet sheet;
    private int rowIndex;
    private boolean started;

    /**
     * Creates the writer.
     *
     * @param outputStream destination stream
     */
    public XlsxReportWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void start(ReportDefinition definition) {
        try {
            workbook = new SXSSFWorkbook(100);
            workbook.setCompressTempFiles(true);

            String sheetName = definition.sheetName() == null || definition.sheetName().isBlank()
                    ? "Report"
                    : definition.sheetName();

            sheet = workbook.createSheet(sheetName);

            Row headerRow = sheet.createRow(rowIndex++);
            List<ReportColumn> columns = definition.columns();

            for (int i = 0; i < columns.size(); i++) {
                ReportColumn column = columns.get(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(column.header());

                if (column.width() != null && column.width() > 0) {
                    sheet.setColumnWidth(i, column.width() * 256);
                }
            }

            started = true;
        } catch (Exception e) {
            throw new ReportWriterException("Could not start XLSX", e);
        }
    }

    @Override
    public void writeRow(List<?> values) {
        ensureStarted();

        try {
            Row row = sheet.createRow(rowIndex++);

            for (int i = 0; i < values.size(); i++) {
                Cell cell = row.createCell(i);
                writeCell(cell, values.get(i));
            }
        } catch (Exception e) {
            throw new ReportWriterException("Could not write XLSX row", e);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void finish() {
        try {
            workbook.write(outputStream);
            outputStream.flush();
        } catch (Exception e) {
            throw new ReportWriterException("Could not finish XLSX", e);
        }
    }

    @Override
    public void close() {
        try {
            if (workbook != null) {
                workbook.dispose();
                workbook.close();
            }
            outputStream.close();
        } catch (Exception e) {
            throw new ReportWriterException("Could not close XLSX", e);
        }
    }

    private void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }

        if (value instanceof Number v) {
            cell.setCellValue(v.doubleValue());
            return;
        }

        if (value instanceof Boolean v) {
            cell.setCellValue(v);
            return;
        }

        if (value instanceof LocalDate v) {
            cell.setCellValue(v.toString());
            return;
        }

        if (value instanceof LocalDateTime v) {
            cell.setCellValue(v.toString());
            return;
        }

        cell.setCellValue(String.valueOf(value));
    }

    private void ensureStarted() {
        if (!started) {
            throw new ReportWriterException("XLSX writer was not started");
        }
    }
}