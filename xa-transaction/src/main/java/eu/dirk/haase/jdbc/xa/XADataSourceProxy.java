package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractXADataSourceProxy;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.SQLException;

public abstract class XADataSourceProxy extends AbstractXADataSourceProxy implements XADataSource {

    private final XADataSource delegate;

    protected XADataSourceProxy(XADataSource delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public final XAConnection getXAConnection(String user, String password) throws SQLException {
        try {
            return this.wrapXAConnection(this.delegate.getXAConnection(user, password), user, password);
        } catch (SQLException ex) {
            throw this.checkException(ex);
        }
    }

    @Override
    public final XAConnection getXAConnection() throws SQLException {
        try {
            return this.wrapXAConnection(this.delegate.getXAConnection());
        } catch (SQLException ex) {
            throw this.checkException(ex);
        }
    }
}
