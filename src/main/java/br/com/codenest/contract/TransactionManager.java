package br.com.codenest.contract;

public interface TransactionManager {
    void begin();

    void commit();

    void rollback();
}
