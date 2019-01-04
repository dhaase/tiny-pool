package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.DataSource;

public abstract class DataSourceProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object,Object>, DataSource> implements ValidState {


    protected DataSourceProxy(final DataSource delegate) {
        super(delegate);
    }

}
