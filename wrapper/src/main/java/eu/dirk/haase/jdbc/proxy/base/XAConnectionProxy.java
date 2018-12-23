package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

public abstract class XAConnectionProxy extends JdbcProxy<XAConnection> {

    private final XADataSource xaDataSource;

    protected XAConnectionProxy(final XADataSource xaDataSource, final XAConnection delegate) {
        super(delegate);
        this.xaDataSource = xaDataSource;
    }

    public XADataSource getXADataSource() {
        return xaDataSource;
    }

}
