package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public class XAResourceProxy extends JdbcProxy<XAResource> {

    private final XAConnection xaConnection;

    protected XAResourceProxy(final XAConnection xaConnection, final XAResource delegate) {
        super(delegate);
        this.xaConnection = xaConnection;
    }

    public XAConnection getXAConnection() {
        return xaConnection;
    }

}