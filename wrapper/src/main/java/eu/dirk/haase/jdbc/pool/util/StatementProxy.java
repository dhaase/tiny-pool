package eu.dirk.haase.jdbc.pool.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class StatementProxy {

    private final Statement delegate;
    private final Connection connection;

    protected StatementProxy(final Connection connection, final Statement delegate) {
        this.connection = connection;
        this.delegate = delegate;
    }


    public final Connection getConnection() {
        return connection;
    }


    public final Statement getDelegate() {
        return delegate;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }


}
