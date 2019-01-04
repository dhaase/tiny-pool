package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

public abstract class AbstractXADataSourceProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, XADataSource> implements ValidState {


    protected AbstractXADataSourceProxy(final XADataSource delegate) {
        super(delegate);
    }

    protected abstract XAConnection wrapXAConnection(XAConnection delegate, Object... argumentArray);

}
