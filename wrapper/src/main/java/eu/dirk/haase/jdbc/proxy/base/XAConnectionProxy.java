package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

public abstract class XAConnectionProxy extends JdbcProxy<XAConnection> {

    private final XADataSource xaDataSource;

    protected XAConnectionProxy(final XAConnection delegate, final XADataSource xaDataSource) {
        super(delegate);
        this.xaDataSource = xaDataSource;
    }

    public XADataSource getXADataSource() {
        return xaDataSource;
    }

}
