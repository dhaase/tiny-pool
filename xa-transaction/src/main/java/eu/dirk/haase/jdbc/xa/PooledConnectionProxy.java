package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractPooledConnectionProxy;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class PooledConnectionProxy extends AbstractPooledConnectionProxy implements PooledConnection {

    private final PooledConnection delegate;

    protected PooledConnectionProxy(PooledConnection delegate, ConnectionPoolDataSource dataSource, Object[] argumentArray) {
        super(delegate, dataSource, argumentArray);
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

}
