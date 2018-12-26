package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public class XAResourceProxy extends ConcurrentFactoryJdbcProxy<XAResource> {

    private final XAConnection xaConnection;

    protected XAResourceProxy(final XAResource delegate, final XAConnection xaConnection) {
        super(delegate);
        this.xaConnection = xaConnection;
    }

    public XAConnection getXAConnection() {
        return xaConnection;
    }

}