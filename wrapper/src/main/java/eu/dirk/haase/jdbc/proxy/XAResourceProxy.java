package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public class XAResourceProxy extends ConcurrentFactoryJdbcProxy<XAResource> {

    private final XAConnection xaConnection;

    protected XAResourceProxy(final XAResource delegate, final XAConnection xaConnection, final Object[] argumentArray) {
        super(delegate);
        this.xaConnection = xaConnection;
    }

    public XAConnection getXAConnection() {
        return xaConnection;
    }

}