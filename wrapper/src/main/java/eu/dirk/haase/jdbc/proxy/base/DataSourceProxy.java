package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.IdentityCache;
import eu.dirk.haase.jdbc.proxy.common.Unwrapper;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.Supplier;

public abstract class DataSourceProxy {

    private final DataSource delegate;

    protected DataSourceProxy(final DataSource delegate) {
        this.delegate = delegate;
    }


    public final DataSource getDelegate() {
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
