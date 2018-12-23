package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.XADataSource;

public abstract class ConnectionPoolDataSourceProxy extends JdbcProxy<ConnectionPoolDataSource> {


    protected ConnectionPoolDataSourceProxy(final ConnectionPoolDataSource delegate) {
        super(delegate);
    }

}
