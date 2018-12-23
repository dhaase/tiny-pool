package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class ConnectionProxy extends JdbcProxy<Connection> {

    private final DataSource dataSource;

    protected ConnectionProxy(final DataSource dataSource, final Connection delegate) {
        super(delegate);
        this.dataSource = dataSource;
    }

    public final DataSource getDataSource() {
        return dataSource;
    }

}
