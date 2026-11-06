package io.github.jean8791.batch.report;

/**
 * Thrown when a report cannot be generated.
 */
public class ReportGenerationException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message description of the failure
     * @param cause   underlying error
     */
    public ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
