package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class ConnectionProxy extends FactoryJdbcProxy<Connection> implements CloseState {

    private final DataSource dataSource;
    private final Connection delegate;

    protected ConnectionProxy(final Connection delegate, final DataSource dataSource, final Object[] argumentArray) throws SQLException {
        super(delegate);
        this.dataSource = dataSource;
        this.delegate = delegate;
        if (this.delegate.getAutoCommit()) {
            this.delegate.setAutoCommit(false);
        }
    }

    public final DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public final boolean isClosed() throws SQLException {
        return delegate.isClosed();
    }

    public final void setAutoCommit(boolean autoCommit) throws SQLException {
        throw new SQLException("AutoCommit is not allowed for transaction managed Connection.");
    }
}
