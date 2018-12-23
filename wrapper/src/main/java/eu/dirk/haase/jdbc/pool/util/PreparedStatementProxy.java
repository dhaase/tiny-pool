package eu.dirk.haase.jdbc.pool.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class PreparedStatementProxy {

    private final PreparedStatement delegate;
    private final Connection connection;

    protected PreparedStatementProxy(Connection connection, PreparedStatement delegate) {
        this.connection = connection;
        this.delegate = delegate;
    }

    public final PreparedStatement getDelegate() {
        return delegate;
    }

    public final Connection getConnection() {
        return connection;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }

}
