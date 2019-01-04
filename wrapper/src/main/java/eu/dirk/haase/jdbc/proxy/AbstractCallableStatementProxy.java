package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

public abstract class AbstractCallableStatementProxy extends FactoryJdbcProxy<CallableStatement> implements CloseState {

    private final Connection connection;
    private final CallableStatement delegate;

    protected AbstractCallableStatementProxy(CallableStatement delegate, Connection connection, final Object[] argumentArray) {
        super(delegate);
        this.connection = connection;
        this.delegate = delegate;
    }

    public final Connection getConnection() {
        return connection;
    }

    protected abstract ResultSet wrapResultSet(ResultSet delegate, Object... argumentArray);

}
