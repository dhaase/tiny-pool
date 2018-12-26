package eu.dirk.haase.jdbc.proxy.base;

import java.sql.CallableStatement;
import java.sql.Connection;

public abstract class CallableStatementProxy extends FactoryJdbcProxy<CallableStatement> {

    private final Connection connection;

    protected CallableStatementProxy(CallableStatement delegate, Connection connection) {
        super(delegate);
        this.connection = connection;
    }

    public final Connection getConnection() {
        return connection;
    }

}
