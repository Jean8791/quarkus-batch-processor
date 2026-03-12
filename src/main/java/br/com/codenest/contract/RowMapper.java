package br.com.codenest.contract;

@FunctionalInterface
public interface RowMapper<T> {
    T map(Object[] row);
}