package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import java.sql.Connection;

public abstract class AbstractXAConnectionProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, XAConnection> implements ValidState {

    private final XADataSource xaDataSource;

    protected AbstractXAConnectionProxy(final XAConnection delegate, final XADataSource xaDataSource, final Object[] argumentArray) {
        super(delegate);
        this.xaDataSource = xaDataSource;
    }

    public XADataSource getXADataSource() {
        return xaDataSource;
    }

    protected abstract Connection wrapConnection(Connection delegate, Object... argumentArray);

    protected abstract XAResource wrapXAResource(XAResource delegate, Object... argumentArray);

}
