package io.github.jean8791.batch;

/**
 * Thrown when a run is interrupted: a record failed under {@link ErrorStrategy#FAIL_FAST},
 * a chunk transaction could not be completed, or the query could not be read.
 */
public class RecordProcessingException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message description of the failure
     * @param cause   underlying error
     */
    public RecordProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
