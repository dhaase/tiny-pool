package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.IdentityCache;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.function.Function;

/**
 * Basis-Klasse f&uuml;r JDBC-Klassen die eingepackt werden sollen und
 * selbst wiederum Factory-Methoden f&uuml;r weitere JDBC-Klassen bieten.
 * <p>
 * Zu den JDBC-Klassen mit Factory-Methoden geh&ouml;ren:
 * <ol>
 * <li>{@link CallableStatement}</li>
 * <li>{@link ConnectionPoolDataSource}</li>
 * <li>{@link DataSource}</li>
 * <li>{@link Connection}</li>
 * <li>{@link PooledConnection}</li>
 * <li>{@link PreparedStatement}</li>
 * <li>{@link Statement}</li>
 * <li>{@link XADataSource}</li>
 * <li>{@link XAConnection}</li>
 * <li>{@link XAResource}</li>
 * </ol>
 *
 * @param <T1> der Typ der jeweiligen abgeleiteten JDBC-Klasse.
 */
public abstract class FactoryJdbcProxy<T1> extends JdbcProxy<T1> {
    private final IdentityCache identityCache;

    protected FactoryJdbcProxy(T1 delegate) {
        super(delegate);
        this.identityCache = new IdentityCache();
    }

    protected final <T2> T2 wrap(T2 delegate, Function<T2, T2> objectMaker) {
        return this.identityCache.get(delegate, objectMaker);
    }

}
