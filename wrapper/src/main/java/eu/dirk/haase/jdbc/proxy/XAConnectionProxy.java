package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

public abstract class XAConnectionProxy extends ConcurrentFactoryJdbcProxy<XAConnection> {

    private final XADataSource xaDataSource;

    protected XAConnectionProxy(final XAConnection delegate, final XADataSource xaDataSource, final Object[] argumentArray) {
        super(delegate);
        this.xaDataSource = xaDataSource;
    }

    public XADataSource getXADataSource() {
        return xaDataSource;
    }

}
