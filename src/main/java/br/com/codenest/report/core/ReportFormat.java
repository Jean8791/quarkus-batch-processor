package br.com.codenest.report.core;

public enum ReportFormat {
    CSV("csv"),
    XLSX("xlsx");

    private final String extension;

    ReportFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }
}