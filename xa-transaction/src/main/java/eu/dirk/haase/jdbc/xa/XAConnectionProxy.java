package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractXAConnectionProxy;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class XAConnectionProxy extends AbstractXAConnectionProxy implements XAConnection {

    private final XAConnection delegate;

    protected XAConnectionProxy(XAConnection delegate, XADataSource xaDataSource, Object[] argumentArray) {
        super(delegate, xaDataSource, argumentArray);
        this.delegate = delegate;
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
            return this.wrapXAResource(this.delegate.getXAResource());
        } catch (SQLException var2) {
            throw this.checkException(var2);
        }
    }

}
