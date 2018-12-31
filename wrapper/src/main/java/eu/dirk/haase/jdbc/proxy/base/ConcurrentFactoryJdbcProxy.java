package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.IdentityCache;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.util.function.BiFunction;

/**
 * Basis-Klasse f&uuml;r JDBC-Klassen die eingepackt werden sollen und
 * selbst wiederum Factory-Methoden f&uuml;r weitere JDBC-Klassen bieten
 * die (auch) nebenl&auml;fig aufgerufen werden k&ouml;nnen.
 * <p>
 * Zu den JDBC-Klassen mit Factory-Methoden, die nebenl&auml;fig aufgerufen
 * werden k&ouml;nnen, geh&ouml;ren alle Klasse die nicht mittelbar oder
 * unmittelbar mit einer {@link Connection}-Instanz verbunden sind:
 * <ol>
 * <li>{@link ConnectionPoolDataSource}</li>
 * <li>{@link DataSource}</li>
 * <li>{@link PooledConnection}</li>
 * <li>{@link XADataSource}</li>
 * <li>{@link XAConnection}</li>
 * <li>{@link XAResource}</li>
 * </ol>
 *
 * @param <T1> der Typ der jeweiligen abgeleiteten JDBC-Klasse.
 */
public abstract class ConcurrentFactoryJdbcProxy<T1> extends FactoryJdbcProxy<T1> {

    private final IdentityCache identityCache;

    protected ConcurrentFactoryJdbcProxy(T1 delegate) {
        this(delegate, new IdentityCache(new WeakIdentityHashMap<>()));
    }

    protected ConcurrentFactoryJdbcProxy(T1 delegate, final IdentityCache identityCache) {
        super(delegate, identityCache);
        this.identityCache = identityCache;
    }

    protected final <T2> T2 wrapConcurrent(T2 delegate, BiFunction<T2, Object[], T2> objectMaker, final Object... argumentArray) {
        return this.identityCache.getConcurrent(delegate, objectMaker, argumentArray);
    }

}
