package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.ConnectionPoolDataSource;


public abstract class ConnectionPoolDataSourceProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, ConnectionPoolDataSource> implements ValidState {


    protected ConnectionPoolDataSourceProxy(final ConnectionPoolDataSource delegate) {
        super(delegate);
    }

}
