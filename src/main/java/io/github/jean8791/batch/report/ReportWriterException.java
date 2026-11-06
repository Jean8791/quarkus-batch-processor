package io.github.jean8791.batch.report;

/**
 * Thrown when a {@link ReportWriter} fails or is used out of order.
 */
public class ReportWriterException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message description of the failure
     * @param cause   underlying error
     */
    public ReportWriterException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception.
     *
     * @param message description of the failure
     */
    public ReportWriterException(String message) {
        super(message);
    }
}
