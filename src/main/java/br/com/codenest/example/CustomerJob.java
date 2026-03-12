package br.com.codenest.example;

import br.com.codenest.engine.FastRecordProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

@ApplicationScoped
public class CustomerJob {

    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction userTransaction;


    public void run() {

        FastRecordProcessor processor = new FastRecordProcessor(entityManager, userTransaction);

        processor
                .source(CustomerEntity.class)
                // .query(...)
                // .chunkSize(...)
                // .transactionMode(...)
                // .onError(...)
                // .retryStrategy(...)
                // .onProcess(...)
                .run();
    }

}
