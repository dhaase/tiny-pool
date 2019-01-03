package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.CallableStatement;
import java.sql.Connection;

public abstract class CallableStatementProxy extends FactoryJdbcProxy<CallableStatement> implements CloseState {

    private final Connection connection;
    private final CallableStatement delegate;

    protected CallableStatementProxy(CallableStatement delegate, Connection connection, final Object[] argumentArray) {
        super(delegate);
        this.connection = connection;
        this.delegate = delegate;
    }

    public final Connection getConnection() {
        return connection;
    }

}
