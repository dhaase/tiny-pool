package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.IdentityCache;
import eu.dirk.haase.jdbc.proxy.common.Unwrapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Supplier;

public abstract class PreparedStatementProxy {

    private final PreparedStatement delegate;
    private final Connection connection;

    protected PreparedStatementProxy(Connection connection, PreparedStatement delegate) {
        this.connection = connection;
        this.delegate = delegate;
    }

    public final PreparedStatement getDelegate() {
        return delegate;
    }

    public final Connection getConnection() {
        return connection;
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
