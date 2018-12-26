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

    protected final SQLException checkException(SQLException e) {
        return e;
    }

    public final T1 getDelegate() {
        return delegate;
    }

    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return Unwrapper.isWrapperFor(iface, this, this.delegate);
    }

    public final <T> T unwrap(Class<T> iface) throws SQLException {
        return Unwrapper.unwrap(iface, this, this.delegate);
    }

}
