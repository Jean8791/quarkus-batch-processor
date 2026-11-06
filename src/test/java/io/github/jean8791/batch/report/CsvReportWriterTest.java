package io.github.jean8791.batch.report;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvReportWriterTest {

    @Test
    void quotesValuesWithSeparatorsQuotesAndLineBreaks() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (CsvReportWriter writer = new CsvReportWriter(out)) {
            writer.start(new ReportDefinition("r", null, List.of(
                    new ReportColumn("a", "A", ColumnType.STRING, null),
                    new ReportColumn("b", "B", ColumnType.STRING, null))));
            writer.writeRow(List.of("Smith, John", "say \"hi\""));
            writer.writeRow(Arrays.asList("line1\nline2", null));
            writer.finish();
        }

        String nl = System.lineSeparator();
        String expected = "A,B" + nl
                + "\"Smith, John\",\"say \"\"hi\"\"\"" + nl
                + "\"line1\nline2\"," + nl;
        assertEquals(expected, out.toString(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsRowsBeforeStart() {
        CsvReportWriter writer = new CsvReportWriter(new ByteArrayOutputStream());

        assertThrows(ReportWriterException.class, () -> writer.writeRow(List.of("x")));
    }
}
