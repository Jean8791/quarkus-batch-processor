package io.github.jean8791.batch.report;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XlsxReportWriterTest {

    @Test
    void writesTypedCells() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (XlsxReportWriter writer = new XlsxReportWriter(out)) {
            writer.start(new ReportDefinition("r", "Items", List.of(
                    new ReportColumn("sku", "SKU", ColumnType.STRING, 20),
                    new ReportColumn("price", "PRICE", ColumnType.DECIMAL, 10),
                    new ReportColumn("active", "ACTIVE", ColumnType.BOOLEAN, null),
                    new ReportColumn("since", "SINCE", ColumnType.DATE, null))));
            writer.writeRow(List.of("SKU-1", new BigDecimal("19.90"), true, LocalDate.of(2026, 3, 12)));
            writer.finish();
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            Sheet sheet = workbook.getSheet("Items");
            Row header = sheet.getRow(0);
            Row row = sheet.getRow(1);

            assertEquals("PRICE", header.getCell(1).getStringCellValue());
            assertEquals("SKU-1", row.getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC, row.getCell(1).getCellType());
            assertEquals(19.90, row.getCell(1).getNumericCellValue(), 0.0001);
            assertEquals(CellType.BOOLEAN, row.getCell(2).getCellType());
            assertEquals("2026-03-12", row.getCell(3).getStringCellValue());
        }
    }
}
