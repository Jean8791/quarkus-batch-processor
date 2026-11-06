package io.github.jean8791.batch.report;

/**
 * Generates a report file from a {@link ReportFiller}.
 */
public interface ReportGenerator {

    /**
     * Generates the report.
     *
     * @param request where and how to write the report
     * @param filler  source of the rows
     * @return details of the generated file
     * @throws ReportGenerationException if the report cannot be written
     */
    ReportResult generate(ReportRequest request, ReportFiller filler);
}
