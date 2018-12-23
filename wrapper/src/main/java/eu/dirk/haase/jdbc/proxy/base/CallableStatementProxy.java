package eu.dirk.haase.jdbc.proxy.base;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class CallableStatementProxy extends JdbcProxy<CallableStatement> {

    private final CallableStatement delegate;
    private final Connection connection;

    protected CallableStatementProxy(Connection connection, CallableStatement delegate) {
        super(delegate);
        this.connection = connection;
        this.delegate = delegate;
    }

    public final Connection getConnection() {
        return connection;
    }

}
