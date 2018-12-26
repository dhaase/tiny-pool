package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public abstract class PooledConnectionProxy extends ConcurrentFactoryJdbcProxy<PooledConnection> {

    private final ConnectionPoolDataSource connectionPoolDataSource;

    protected PooledConnectionProxy(final PooledConnection delegate, final ConnectionPoolDataSource dataSource) {
        super(delegate);
        this.connectionPoolDataSource = dataSource;
    }

    public final ConnectionPoolDataSource getConnectionPoolDataSource() {
        return connectionPoolDataSource;
    }

}
