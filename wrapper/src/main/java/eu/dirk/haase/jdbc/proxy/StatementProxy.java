package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class StatementProxy extends FactoryJdbcProxy<Statement> implements CloseState {

    private final Connection connection;
    private final Statement delegate;

    protected StatementProxy(final Statement delegate, final Connection connection) {
        super(delegate);
        this.connection = connection;
        this.delegate = delegate;
    }

    public final Connection getConnection() {
        return connection;
    }

    @Override
    public final boolean isClosed() throws SQLException {
        return delegate.isClosed();
    }

}

