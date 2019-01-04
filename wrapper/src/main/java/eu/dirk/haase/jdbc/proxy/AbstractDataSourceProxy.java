package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.DataSource;
import java.sql.Connection;

public abstract class AbstractDataSourceProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, DataSource> implements ValidState {


    protected AbstractDataSourceProxy(final DataSource delegate) {
        super(delegate);
    }

    protected abstract Connection wrapConnection(Connection delegate, Object... argumentArray);

}
