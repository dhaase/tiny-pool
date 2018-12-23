package eu.dirk.haase.jdbc.pool.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class ResultSetProxy {

    private final ResultSet delegate;
    private final Statement statement;

    protected ResultSetProxy(final Statement statement, final ResultSet delegate) {
        this.statement = statement;
        this.delegate = delegate;
    }


    public final Statement getStatement() {
        return statement;
    }


    public final ResultSet getDelegate() {
        return delegate;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }


    protected final <T> T wrap(T delegate, Supplier<T> make) {
        return IdentityCache.getSingleton().get(delegate, make);
    }


    public final  <T> T unwrap(Class<T> iface) throws SQLException {
        return Unwrapper.unwrap(iface, this, this.delegate);
    }

    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return Unwrapper.isWrapperFor(iface, this, this.delegate);
    }


}
