package io.github.jean8791.benchmark;

import io.github.jean8791.batch.FastRecordProcessor;
import io.github.jean8791.batch.TransactionMode;
import io.github.jean8791.batch.report.ColumnType;
import io.github.jean8791.batch.report.LocalFileTarget;
import io.github.jean8791.batch.report.ProcessorBackedReportService;
import io.github.jean8791.batch.report.ReportColumn;
import io.github.jean8791.batch.report.ReportDefinition;
import io.github.jean8791.batch.report.ReportFormat;
import io.github.jean8791.batch.report.ReportRequest;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * The two ways of doing each job. Both read the same rows with the same query and do the same
 * per-row work; only the reading strategy differs.
 */
@ApplicationScoped
public class Scenarios {

    private static final String QUERY = "select r from BenchRow r where r.id <= :max order by r.id";
    private static final int CHUNK_SIZE = 1000;

    @Inject
    EntityManager entityManager;

    @Inject
    FastRecordProcessor processor;

    /** Last sum computed; kept so the per-row work cannot be optimised away. */
    volatile BigDecimal checksum = BigDecimal.ZERO;

    /** Reads every row into a list, sums the amounts and returns how many rows were read. */
    public long readNaive(long rows) {
        return QuarkusTransaction.requiringNew().call(() -> {
            List<BenchRow> all = entityManager.createQuery(QUERY, BenchRow.class)
                    .setParameter("max", rows)
                    .getResultList();
            BigDecimal sum = BigDecimal.ZERO;
            for (BenchRow row : all) {
                sum = sum.add(row.amount);
            }
            checksum = sum;
            return (long) all.size();
        });
    }

    /**
     * The setup the README recommends for PostgreSQL: {@code run()} with {@code TransactionMode.NONE}
     * called from inside a transaction, so the connection is not in autocommit mode.
     */
    public long readLibraryInsideTx(long rows) {
        return QuarkusTransaction.requiringNew().call(() -> readLibrary(rows));
    }

    /** Same as {@link #readLibrary} but with one JTA transaction per chunk. */
    @ActivateRequestContext
    public long readLibraryChunkTx(long rows) {
        BigDecimal[] sum = {BigDecimal.ZERO};
        long processed = processor.source(BenchRow.class)
                .query(QUERY)
                .params(java.util.Map.of("max", rows))
                .chunkSize(CHUNK_SIZE)
                .transactionMode(TransactionMode.CHUNK)
                .loggingEnabled(false)
                .onProcess(row -> sum[0] = sum[0].add(row.amount))
                .run()
                .processedCount();
        checksum = sum[0];
        return processed;
    }

    // FastRecordProcessor clears the EntityManager after each chunk, which needs an active
    // transaction or request context. A CLI app has neither, so the benchmark opens a request context.
    /** Streams the rows in chunks, sums the amounts and returns how many rows were read. */
    @ActivateRequestContext
    public long readLibrary(long rows) {
        BigDecimal[] sum = {BigDecimal.ZERO};
        long processed = processor.source(BenchRow.class)
                .query(QUERY)
                .params(java.util.Map.of("max", rows))
                .chunkSize(CHUNK_SIZE)
                .loggingEnabled(false)
                .onProcess(row -> sum[0] = sum[0].add(row.amount))
                .run()
                .processedCount();
        checksum = sum[0];
        return processed;
    }

    /** Loads every row into a list and writes an in-memory XSSF workbook. */
    public long xlsxNaive(long rows, Path directory) throws IOException {
        List<BenchRow> all = QuarkusTransaction.requiringNew().call(() ->
                entityManager.createQuery(QUERY, BenchRow.class)
                        .setParameter("max", rows)
                        .getResultList());
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream out = Files.newOutputStream(directory.resolve("naive.xlsx"))) {
            XSSFSheet sheet = workbook.createSheet("rows");
            int index = 0;
            for (BenchRow row : all) {
                var cells = sheet.createRow(index++);
                cells.createCell(0).setCellValue(row.id);
                cells.createCell(1).setCellValue(row.label);
                cells.createCell(2).setCellValue(row.description);
                cells.createCell(3).setCellValue(row.amount.doubleValue());
                cells.createCell(4).setCellValue(row.createdAt.toString());
            }
            workbook.write(out);
        }
        return all.size();
    }

    /** Library XLSX export called from inside a transaction (see {@link #readLibraryInsideTx}). */
    public long xlsxLibraryInsideTx(long rows, Path directory) {
        return QuarkusTransaction.requiringNew().call(() -> xlsxLibrary(rows, directory));
    }

    /** Streams the rows through the library's XLSX export (SXSSF, 100 rows in memory). */
    @ActivateRequestContext
    public long xlsxLibrary(long rows, Path directory) {
        ReportDefinition definition = new ReportDefinition("bench", "rows", List.of(
                new ReportColumn("id", "id", ColumnType.LONG, null),
                new ReportColumn("label", "label", ColumnType.STRING, null),
                new ReportColumn("description", "description", ColumnType.STRING, null),
                new ReportColumn("amount", "amount", ColumnType.DECIMAL, null),
                new ReportColumn("createdAt", "createdAt", ColumnType.STRING, null)));
        ReportRequest request = new ReportRequest("library.xlsx", ReportFormat.XLSX, definition,
                new LocalFileTarget(directory.toString()));
        // The service only takes the query text, so the row limit goes into the HQL itself.
        String hql = QUERY.replace(":max", Long.toString(rows));
        return new ProcessorBackedReportService(processor).generate(BenchRow.class, hql, CHUNK_SIZE, request,
                row -> List.of(row.id, row.label, row.description, row.amount, row.createdAt.toString())).rowCount();
    }
}
