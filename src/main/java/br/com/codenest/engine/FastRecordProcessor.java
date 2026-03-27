package br.com.codenest.engine;

import br.com.codenest.contract.Processor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import java.util.Objects;

@ApplicationScoped
public class FastRecordProcessor {

    private final EntityManager entityManager;
    private final UserTransaction userTransaction;

    protected FastRecordProcessor() {
        this.entityManager = null;
        this.userTransaction = null;
    }

    @Inject
    public FastRecordProcessor(EntityManager entityManager, UserTransaction userTransaction) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
        this.userTransaction = Objects.requireNonNull(userTransaction, "userTransaction must not be null");
    }

    public <T> Processor<T> source(Class<T> entityClass) {
        return new FastRecordProcessorImpl<>(entityManager, userTransaction, entityClass);
    }
}
