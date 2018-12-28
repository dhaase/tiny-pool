package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.Unwrapper;

import java.sql.SQLException;

/**
 * Basis-Klasse f&uuml;r alle JDBC-Klassen die eingepackt werden sollen.
 *
 * @param <T1> der Typ der jeweiligen abgeleiteten JDBC-Klasse.
 */
public abstract class JdbcProxy<T1> {

    private final T1 delegate;

    protected JdbcProxy(final T1 delegate) {
        this.delegate = delegate;
    }

    protected SQLException checkException(SQLException e) {
        return e;
    }

    @Override
    public boolean equals(Object thatObj) {
        if (this == thatObj) return true;
        if (!(thatObj instanceof JdbcProxy)) return false;

        JdbcProxy<?> that = (JdbcProxy<?>) thatObj;

        return this.delegate != null ? this.delegate.equals(that.delegate) : that.delegate == null;
    }

    public final T1 getDelegate() {
        return delegate;
    }

    @Override
    public int hashCode() {
        return delegate != null ? delegate.hashCode() : 0;
    }

    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return Unwrapper.isWrapperFor(iface, this, this.delegate);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "delegate=" + delegate +
                '}';
    }

    public final <T2> T2 unwrap(Class<T2> iface) throws SQLException {
        return Unwrapper.unwrap(iface, this, this.delegate);
    }
}
