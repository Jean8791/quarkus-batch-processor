package io.github.jean8791.batch.report;

import java.io.OutputStream;

/**
 * Default {@link ReportGenerator}: opens the target, writes the header, lets the filler write
 * the rows and closes everything.
 */
public class DefaultReportGenerator implements ReportGenerator {

    private final ReportWriterFactory writerFactory;

    /**
     * Creates the generator.
     *
     * @param writerFactory factory of format writers
     */
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

            return new ReportResult(normalizedFileName, filePath, request.format(), rowCount);
        } catch (Exception e) {
            throw new ReportGenerationException("Could not generate report: " + normalizedFileName, e);
        }
    }

    private String normalizeFileName(String fileName, ReportFormat format) {
        String extension = "." + format.extension();
        return fileName.endsWith(extension) ? fileName : fileName + extension;
    }
}
