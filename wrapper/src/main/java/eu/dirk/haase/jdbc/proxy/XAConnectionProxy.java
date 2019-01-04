package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

public abstract class XAConnectionProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object,Object>, XAConnection> implements ValidState {

    private final XADataSource xaDataSource;

    protected XAConnectionProxy(final XAConnection delegate, final XADataSource xaDataSource, final Object[] argumentArray) {
        super(delegate);
        this.xaDataSource = xaDataSource;
    }

    public XADataSource getXADataSource() {
        return xaDataSource;
    }

}
