package eu.dirk.haase.jdbc.pool.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class ConnectionProxy {

    private final Connection delegate;
    private final DataSource dataSource;

    protected ConnectionProxy(final DataSource dataSource, final Connection delegate) {
        this.dataSource = dataSource;
        this.delegate = delegate;
    }

    public final DataSource getDataSource() {
        return dataSource;
    }


    public final Connection getDelegate() {
        return delegate;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }

}
