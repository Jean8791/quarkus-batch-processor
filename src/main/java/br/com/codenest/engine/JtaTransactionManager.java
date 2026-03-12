package br.com.codenest.engine;

import br.com.codenest.contract.TransactionManager;
import jakarta.transaction.UserTransaction;

public class JtaTransactionManager implements TransactionManager {

    private final UserTransaction userTransaction;

    public JtaTransactionManager(UserTransaction userTransaction) {
        this.userTransaction = userTransaction;
    }

    @Override
    public void begin() {
        try {
            userTransaction.begin();
        } catch (Exception e) {
            throw new RuntimeException("Failed to begin transaction", e);
        }
    }

    @Override
    public void commit() {
        try {
            userTransaction.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }

    @Override
    public void rollback() {
        try {
            userTransaction.rollback();
        } catch (Exception e) {
            throw new RuntimeException("Failed to rollback transaction", e);
        }
    }
}