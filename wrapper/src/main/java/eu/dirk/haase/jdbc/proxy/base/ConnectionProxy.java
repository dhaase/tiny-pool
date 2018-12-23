package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.IdentityCache;
import eu.dirk.haase.jdbc.proxy.common.Unwrapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

public abstract class ConnectionProxy {

    private final Connection delegate;
    private final DataSource dataSource;

    protected ConnectionProxy(final DataSource dataSource, final Connection delegate) {
        this.dataSource = dataSource;
        this.delegate = delegate;
    }

    public final DataSource getDataSource() {
        return dataSource;
    }


    public final Connection getDelegate() {
        return delegate;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }


    protected final <T> T wrap(T delegate, Supplier<T> make) {
        return IdentityCache.getSingleton().get(delegate, make);
    }

    public final <T> T unwrap(Class<T> iface) throws SQLException {
        return Unwrapper.unwrap(iface, this, this.delegate);
    }

    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return Unwrapper.isWrapperFor(iface, this, this.delegate);
    }

}
