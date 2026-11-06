package io.github.jean8791.batch.report;

/**
 * Produces the rows of a report.
 */
@FunctionalInterface
public interface ReportFiller {

    /**
     * Writes every row of the report.
     *
     * @param writer writer already started with the header
     * @return number of rows written
     */
    long fill(ReportWriter writer);
}
