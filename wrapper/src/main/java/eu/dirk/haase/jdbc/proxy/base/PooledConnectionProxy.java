package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public abstract class PooledConnectionProxy extends JdbcProxy<PooledConnection> {

    private final ConnectionPoolDataSource connectionPoolDataSource;

    protected PooledConnectionProxy(final ConnectionPoolDataSource dataSource, final PooledConnection delegate) {
        super(delegate);
        this.connectionPoolDataSource = dataSource;
    }

    public final ConnectionPoolDataSource getConnectionPoolDataSource() {
        return connectionPoolDataSource;
    }

}
