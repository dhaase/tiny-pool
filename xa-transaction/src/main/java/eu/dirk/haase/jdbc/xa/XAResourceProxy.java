package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractXAResourceProxy;

import javax.sql.XAConnection;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

public abstract class XAResourceProxy extends AbstractXAResourceProxy implements XAResource {

    private TransactionManager transactionManager;
    private Transaction transaction;

    protected XAResourceProxy(XAResource delegate, XAConnection xaConnection, Object[] argumentArray) {
        super(delegate, xaConnection, argumentArray);
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) throws SystemException, RollbackException {
        this.transactionManager = transactionManager;
        this.transaction = this.transactionManager.getTransaction();
        this.transaction.enlistResource(this);
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

}
