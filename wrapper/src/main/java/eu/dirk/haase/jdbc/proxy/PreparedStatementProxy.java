package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class PreparedStatementProxy extends FactoryJdbcProxy<PreparedStatement> implements CloseState {

    private final Connection connection;
    private final PreparedStatement delegate;

    protected PreparedStatementProxy(PreparedStatement delegate, Connection connection, final Object[] argumentArray) {
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
