package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public abstract class AbstractStatementProxy extends FactoryJdbcProxy<Statement> implements CloseState {

    private final Connection connection;
    private final Statement delegate;

    protected AbstractStatementProxy(final Statement delegate, final Connection connection, final Object[] argumentArray) {
        super(delegate);
        this.connection = connection;
        this.delegate = delegate;
    }

    public final Connection getConnection() {
        return connection;
    }

    protected abstract ResultSet wrapResultSet(ResultSet delegate, Object... argumentArray);

}


