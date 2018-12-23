package eu.dirk.haase.jdbc.pool.util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class CallableStatementProxy {

    private final CallableStatement delegate;
    private final Connection connection;

    protected CallableStatementProxy(Connection connection, CallableStatement delegate) {
        this.connection = connection;
        this.delegate = delegate;
    }

    public final CallableStatement getDelegate() {
        return delegate;
    }

    public final Connection getConnection() {
        return connection;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }

}
