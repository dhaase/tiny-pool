package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;

import javax.sql.ConnectionPoolDataSource;

public abstract class ConnectionPoolDataSourceProxy extends ConcurrentFactoryJdbcProxy<ConnectionPoolDataSource> {


    protected ConnectionPoolDataSourceProxy(final ConnectionPoolDataSource delegate) {
        super(delegate);
    }

}
