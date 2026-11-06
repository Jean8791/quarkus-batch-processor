package io.github.jean8791.batch;

/**
 * Transaction boundary used while the records of each chunk are processed.
 */
public enum TransactionMode {

    /**
     * The processor does not open transactions. Use it for read-only work, such as exports,
     * or when the caller manages transactions inside the record consumer.
     */
    NONE,

    /**
     * Each chunk runs in its own JTA transaction, committed after the last record of the chunk
     * and rolled back if the chunk fails. Requires that no transaction is active when
     * {@link Processor#run()} is called.
     */
    CHUNK
}
