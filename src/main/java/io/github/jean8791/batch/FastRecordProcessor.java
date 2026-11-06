package io.github.jean8791.batch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

import java.util.Objects;

/**
 * Entry point of the library: an application-scoped CDI bean that creates batch runs.
 *
 * <pre>{@code
 * @Inject
 * FastRecordProcessor processor;
 *
 * processor.source(Customer.class)
 *         .onProcess(customer -> mailer.sendNewsletter(customer))
 *         .run();
 * }</pre>
 *
 * <p>It uses the application's default {@link EntityManager} and {@link UserTransaction},
 * so no extra configuration is needed.
 */
@ApplicationScoped
public class FastRecordProcessor {

    private final EntityManager entityManager;
    private final UserTransaction userTransaction;

    /**
     * Constructor required by CDI for client proxies. Not meant to be called directly.
     */
    protected FastRecordProcessor() {
        this.entityManager = null;
        this.userTransaction = null;
    }

    /**
     * Creates the processor.
     *
     * @param entityManager   the application's entity manager
     * @param userTransaction the application's JTA transaction
     */
    @Inject
    public FastRecordProcessor(EntityManager entityManager, UserTransaction userTransaction) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
        this.userTransaction = Objects.requireNonNull(userTransaction, "userTransaction must not be null");
    }

    /**
     * Starts the definition of a new run.
     *
     * @param entityClass type of the records; an entity for HQL queries, or any class for native
     *                    queries with a {@link RowMapper}
     * @param <T>         record type
     * @return a new processor to configure and run
     */
    public <T> Processor<T> source(Class<T> entityClass) {
        return new FastRecordProcessorImpl<>(entityManager, userTransaction, entityClass);
    }
}
