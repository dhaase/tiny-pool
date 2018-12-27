package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class ConnectionProxy extends FactoryJdbcProxy<Connection> implements CloseState {

    private final DataSource dataSource;
    private final Connection delegate;

    protected ConnectionProxy(final Connection delegate, final DataSource dataSource, final Object[] argumentArray) {
        super(delegate);
        this.dataSource = dataSource;
        this.delegate = delegate;
    }

    public final DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public final boolean isClosed() throws SQLException {
        System.out.println("isClosed");
        return delegate.isClosed();
    }

}
