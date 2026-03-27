package br.com.codenest.report.core;

import br.com.codenest.report.result.ReportResult;
import br.com.codenest.report.targets.LocalFileTarget;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultReportGeneratorTest {

    @Test
    void shouldGenerateCsvAndExposeFilePath() throws Exception {
        Path tempDir = Files.createTempDirectory("default-report-generator-csv-");
        LocalFileTarget outputTarget = new LocalFileTarget(tempDir.toString());
        DefaultReportGenerator generator = new DefaultReportGenerator(new ReportWriterFactory());

        ReportRequest request = new ReportRequest(
                "customers",
                ReportFormat.CSV,
                new ReportDefinition(
                        "Customers",
                        null,
                        List.of(
                                new ReportColumn("id", "ID", ColumnType.LONG, 10),
                                new ReportColumn("name", "NAME", ColumnType.STRING, 30)
                        )
                ),
                outputTarget
        );

        ReportResult result = generator.generate(request, writer -> {
            writer.writeRow(List.of(1L, "Alice"));
            writer.writeRow(List.of(2L, "Bob"));
            return 2;
        });

        assertEquals("customers.csv", result.fileName());
        assertNotNull(result.filePath());
        assertEquals(ReportFormat.CSV, result.format());
        assertEquals(2, result.rowCount());

        Path generatedFile = Path.of(result.filePath());
        assertTrue(Files.exists(generatedFile));
        assertEquals(
                List.of("ID,NAME", "1,Alice", "2,Bob"),
                Files.readAllLines(generatedFile)
        );
    }

    @Test
    void shouldGenerateXlsxFile() throws Exception {
        Path tempDir = Files.createTempDirectory("default-report-generator-xlsx-");
        LocalFileTarget outputTarget = new LocalFileTarget(tempDir.toString());
        DefaultReportGenerator generator = new DefaultReportGenerator(new ReportWriterFactory());

        ReportRequest request = new ReportRequest(
                "inventory",
                ReportFormat.XLSX,
                new ReportDefinition(
                        "Inventory",
                        "Items",
                        List.of(
                                new ReportColumn("sku", "SKU", ColumnType.STRING, 20),
                                new ReportColumn("qty", "QTY", ColumnType.INTEGER, 10)
                        )
                ),
                outputTarget
        );

        ReportResult result = generator.generate(request, writer -> {
            writer.writeRow(List.of("SKU-1", 10));
            return 1;
        });

        assertEquals("inventory.xlsx", result.fileName());
        assertNotNull(result.filePath());
        assertEquals(ReportFormat.XLSX, result.format());
        assertEquals(1, result.rowCount());

        Path generatedFile = Path.of(result.filePath());
        assertTrue(Files.exists(generatedFile));
        assertTrue(Files.size(generatedFile) > 0);
    }
}
