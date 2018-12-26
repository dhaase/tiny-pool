package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.ConnectionPoolDataSource;

public abstract class ConnectionPoolDataSourceProxy extends JdbcProxy<ConnectionPoolDataSource> {


    protected ConnectionPoolDataSourceProxy(final ConnectionPoolDataSource delegate) {
        super(delegate);
    }

}
