package io.github.jean8791.batch.report;

/**
 * Supported file formats.
 */
public enum ReportFormat {

    /** Comma-separated values, UTF-8. */
    CSV("csv"),

    /** Excel workbook, written in streaming mode. Requires {@code org.apache.poi:poi-ooxml}. */
    XLSX("xlsx");

    private final String extension;

    ReportFormat(String extension) {
        this.extension = extension;
    }

    /**
     * @return file extension, without the dot
     */
    public String extension() {
        return extension;
    }
}
