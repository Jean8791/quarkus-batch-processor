package io.github.jean8791.batch;

/**
 * Converts a row returned by a native SQL query into a record.
 *
 * @param <T> record type
 */
@FunctionalInterface
public interface RowMapper<T> {

    /**
     * Maps one row.
     *
     * @param row column values in the order of the {@code select} clause;
     *            a single-column query yields an array with one element
     * @return the mapped record
     */
    T map(Object[] row);
}
