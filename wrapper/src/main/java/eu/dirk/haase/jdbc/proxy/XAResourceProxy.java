package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public abstract class XAResourceProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, XAResource> implements ValidState {

    private final XAConnection xaConnection;

    protected XAResourceProxy(final XAResource delegate, final XAConnection xaConnection, final Object[] argumentArray) {
        super(delegate);
        this.xaConnection = xaConnection;
    }

    public XAConnection getXAConnection() {
        return xaConnection;
    }

}