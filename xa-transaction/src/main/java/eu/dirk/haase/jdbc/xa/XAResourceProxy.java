package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractXAResourceProxy;

import javax.sql.XAConnection;
import javax.transaction.*;
import javax.transaction.xa.XAResource;
import java.sql.Connection;

public abstract class XAResourceProxy extends AbstractXAResourceProxy implements XAResource, Synchronization {

    private Connection connection;
    private Transaction transaction;
    private TransactionManager transactionManager;

    protected XAResourceProxy(XAResource delegate, XAConnection xaConnection, Object[] argumentArray) {
        super(delegate, xaConnection, argumentArray);
    }

    public void afterCompletion(int status) {

    }

    public void beforeCompletion() {

    }

    public void enlistResource() throws SystemException, RollbackException {
        this.transaction = this.transactionManager.getTransaction();
        this.transaction.registerSynchronization(this);
        this.transaction.enlistResource(this);
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

}
