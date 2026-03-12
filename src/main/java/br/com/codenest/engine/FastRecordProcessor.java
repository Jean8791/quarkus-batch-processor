package br.com.codenest.engine;

import br.com.codenest.contract.Processor;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

public class FastRecordProcessor {

    private final EntityManager entityManager;
    private final UserTransaction userTransaction;

    public FastRecordProcessor(EntityManager entityManager, UserTransaction userTransaction) {
        this.entityManager = entityManager;
        this.userTransaction = userTransaction;
    }

    public <T> Processor<T> source(Class<T> entityClass) {
        return new FastRecordProcessorImpl<>(entityManager, userTransaction, entityClass);
    }
}