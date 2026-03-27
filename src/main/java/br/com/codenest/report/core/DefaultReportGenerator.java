package br.com.codenest.report.core;

import br.com.codenest.report.contract.ReportWriter;
import br.com.codenest.report.exception.ReportGenerationException;
import br.com.codenest.report.result.ReportResult;
import br.com.codenest.report.targets.LocalFileTarget;

import java.io.OutputStream;

public class DefaultReportGenerator implements ReportGenerator {

    private final ReportWriterFactory writerFactory;

    public DefaultReportGenerator(ReportWriterFactory writerFactory) {
        this.writerFactory = writerFactory;
    }

    @Override
    public ReportResult generate(ReportRequest request, ReportFiller filler) {
        String normalizedFileName = normalizeFileName(request.fileName(), request.format());

        try (OutputStream os = request.outputTarget().open(normalizedFileName);
             ReportWriter writer = writerFactory.create(request.format(), os)) {

            writer.start(request.definition());
            long rowCount = filler.fill(writer);
            writer.finish();

            String filePath = null;
            if (request.outputTarget() instanceof LocalFileTarget localFileTarget) {
                filePath = localFileTarget.getLastOpenedPath();
            }

            return new ReportResult(
                    normalizedFileName,
                    filePath,
                    request.format(),
                    rowCount
            );
        } catch (Exception e) {
            throw new ReportGenerationException("Erro ao gerar relatório: " + normalizedFileName, e);
        }
    }

    private String normalizeFileName(String fileName, ReportFormat format) {
        String extension = "." + format.extension();
        return fileName.endsWith(extension) ? fileName : fileName + extension;
    }
}
