package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractXADataSourceProxy;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import java.sql.SQLException;

public abstract class XADataSourceProxy extends AbstractXADataSourceProxy implements XADataSource {

    private final XADataSource delegate;
    private TransactionManager transactionManager;

    protected XADataSourceProxy(XADataSource delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public final XAConnection getXAConnection(String user, String password) throws SQLException {
        try {
            final XAConnectionProxy xaConnection = this.wrapXAConnection(this.delegate.getXAConnection(user, password), user, password);
            xaConnection.setTransactionManager(transactionManager);
            return xaConnection;
        } catch (SQLException ex) {
            throw this.checkException(ex);
        }
    }

    @Override
    public final XAConnection getXAConnection() throws SQLException {
        try {
            final XAConnectionProxy xaConnection = this.wrapXAConnection(this.delegate.getXAConnection());
            xaConnection.setTransactionManager(transactionManager);
            return xaConnection;
        } catch (SQLException ex) {
            throw this.checkException(ex);
        }
    }
}
