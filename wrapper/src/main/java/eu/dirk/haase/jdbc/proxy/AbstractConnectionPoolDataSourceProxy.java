package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;


public abstract class AbstractConnectionPoolDataSourceProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, ConnectionPoolDataSource> implements ValidState {


    protected AbstractConnectionPoolDataSourceProxy(final ConnectionPoolDataSource delegate) {
        super(delegate);
    }

    protected abstract PooledConnection wrapPooledConnection(PooledConnection delegate, Object... argumentArray);

}
