package br.com.codenest.report.exception;

public class ReportWriterException extends RuntimeException {
    public ReportWriterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportWriterException(String message) {
        super(message);
    }
}