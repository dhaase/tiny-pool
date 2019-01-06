package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractXAConnectionProxy;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class XAConnectionProxy extends AbstractXAConnectionProxy implements XAConnection {

    private final XAConnection delegate;
    private TransactionManager transactionManager;

    protected XAConnectionProxy(XAConnection delegate, XADataSource xaDataSource, Object[] argumentArray) {
        super(delegate, xaDataSource, argumentArray);
        this.delegate = delegate;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return this.wrapConnection(this.delegate.getConnection());
        } catch (SQLException var2) {
            throw this.checkException(var2);
        }
    }

    @Override
    public XAResource getXAResource() throws SQLException {
        try {
            final XAResourceProxy xaResource = this.wrapXAResource(this.delegate.getXAResource());
            xaResource.setTransactionManager(transactionManager);
            xaResource.enlistResource();
            return xaResource;
        } catch (RollbackException | SystemException | SQLException ex) {
            throw this.checkException(ex);
        }
    }

}
