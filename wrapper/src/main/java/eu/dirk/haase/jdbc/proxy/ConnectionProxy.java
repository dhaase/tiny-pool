package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import javax.sql.DataSource;
import java.sql.Connection;

public abstract class ConnectionProxy extends FactoryJdbcProxy<Connection> {

    private final DataSource dataSource;

    protected ConnectionProxy(final Connection delegate, final DataSource dataSource) {
        super(delegate);
        this.dataSource = dataSource;
    }

    public final DataSource getDataSource() {
        return dataSource;
    }

}
