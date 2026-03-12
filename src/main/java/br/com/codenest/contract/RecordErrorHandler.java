package br.com.codenest.contract;

@FunctionalInterface
public interface RecordErrorHandler<T> {
    void onError(T record, Exception exception);
}