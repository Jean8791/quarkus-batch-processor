package io.github.jean8791.batch.report;

/**
 * Declared type of a report column.
 */
public enum ColumnType {
    /** Text. */
    STRING,
    /** 32-bit integer. */
    INTEGER,
    /** 64-bit integer. */
    LONG,
    /** Decimal number. */
    DECIMAL,
    /** True or false. */
    BOOLEAN,
    /** Calendar date. */
    DATE,
    /** Date and time. */
    DATETIME
}
