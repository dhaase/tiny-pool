package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public abstract class AbstractPreparedStatementProxy extends FactoryJdbcProxy<PreparedStatement> implements CloseState {

    private final Connection connection;
    private final PreparedStatement delegate;

    protected AbstractPreparedStatementProxy(PreparedStatement delegate, Connection connection, final Object[] argumentArray) {
        super(delegate);
        this.connection = connection;
        this.delegate = delegate;
    }

    public final Connection getConnection() {
        return connection;
    }

    protected abstract ResultSet wrapResultSet(ResultSet delegate, Object... argumentArray);

}
