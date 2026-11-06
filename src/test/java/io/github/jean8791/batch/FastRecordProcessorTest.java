package io.github.jean8791.batch;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastRecordProcessorTest {

    private final EntityManager entityManager = unsupported(EntityManager.class);

    @Test
    void runRequiresConsumer() {
        FastRecordProcessor processor = new FastRecordProcessor(entityManager, transactionWithStatus(Status.STATUS_NO_TRANSACTION));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.source(String.class).run());

        assertTrue(error.getMessage().contains("consumer"));
    }

    @Test
    void nativeQueryRequiresRowMapper() {
        FastRecordProcessor processor = new FastRecordProcessor(entityManager, transactionWithStatus(Status.STATUS_NO_TRANSACTION));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.source(String.class)
                        .nativeQuery("select name from customer")
                        .onProcess(name -> {
                        })
                        .run());

        assertTrue(error.getMessage().contains("rowMapper"));
    }

    @Test
    void chunkModeRefusesToRunInsideActiveTransaction() {
        FastRecordProcessor processor = new FastRecordProcessor(entityManager, transactionWithStatus(Status.STATUS_ACTIVE));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> processor.source(String.class)
                        .transactionMode(TransactionMode.CHUNK)
                        .onProcess(name -> {
                        })
                        .run());

        assertTrue(error.getMessage().contains("outside of a transaction"));
    }

    @Test
    void rejectsInvalidSettings() {
        Processor<String> processor = new FastRecordProcessor(entityManager, transactionWithStatus(Status.STATUS_NO_TRANSACTION))
                .source(String.class);

        assertThrows(IllegalArgumentException.class, () -> processor.chunkSize(0));
        assertThrows(IllegalArgumentException.class, () -> processor.progressLogEveryChunks(0));
        assertThrows(IllegalArgumentException.class, () -> processor.processorName(" "));
    }

    private static UserTransaction transactionWithStatus(int status) {
        return (UserTransaction) Proxy.newProxyInstance(
                UserTransaction.class.getClassLoader(),
                new Class<?>[]{UserTransaction.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getStatus")) {
                        return status;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static <T> T unsupported(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }));
    }
}
