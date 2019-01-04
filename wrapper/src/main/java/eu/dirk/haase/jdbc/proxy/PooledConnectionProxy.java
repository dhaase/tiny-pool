package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public abstract class PooledConnectionProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object,Object>, PooledConnection> implements ValidState {

    private final ConnectionPoolDataSource connectionPoolDataSource;

    protected PooledConnectionProxy(final PooledConnection delegate, final ConnectionPoolDataSource dataSource, final Object[] argumentArray) {
        super(delegate);
        this.connectionPoolDataSource = dataSource;
    }

    public final ConnectionPoolDataSource getConnectionPoolDataSource() {
        return connectionPoolDataSource;
    }

}
