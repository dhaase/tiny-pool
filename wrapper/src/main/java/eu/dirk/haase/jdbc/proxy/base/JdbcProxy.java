package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.IdentityCache;
import eu.dirk.haase.jdbc.proxy.common.Unwrapper;

import java.sql.SQLException;
import java.util.function.Supplier;

public abstract class JdbcProxy<T> {

    private final T delegate;

    protected JdbcProxy(final T delegate) {
        this.delegate = delegate;
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

    public final T getDelegate() {
        return delegate;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }

}
