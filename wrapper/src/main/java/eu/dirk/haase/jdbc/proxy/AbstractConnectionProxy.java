package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;

import javax.sql.DataSource;
import java.sql.*;

public abstract class AbstractConnectionProxy extends FactoryJdbcProxy<Connection> implements CloseState, ValidState {

    private final DataSource dataSource;
    private final Connection delegate;

    protected AbstractConnectionProxy(final Connection delegate, final DataSource dataSource, final Object[] argumentArray) throws SQLException {
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

    public final void setAutoCommit(boolean autoCommit) throws SQLException {
        throw new SQLException("AutoCommit is not allowed for a transaction managed Connection.");
    }

    protected abstract CallableStatement wrapCallableStatement(CallableStatement delegate, Object... argumentArray);

    protected abstract PreparedStatement wrapPreparedStatement(PreparedStatement delegate, Object... argumentArray);

    protected abstract Statement wrapStatement(Statement delegate, Object... argumentArray);

}
