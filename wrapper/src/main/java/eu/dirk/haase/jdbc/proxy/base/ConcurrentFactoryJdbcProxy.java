package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.ConcurrentMapFunktions;
import eu.dirk.haase.jdbc.proxy.common.ModificationStampingObject;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;

/**
 * Basis-Klasse f&uuml;r JDBC-Klassen die eingepackt werden sollen und
 * selbst wiederum Factory-Methoden f&uuml;r weitere JDBC-Klassen bieten
 * und die (auch) nebenl&auml;fig aufgerufen werden k&ouml;nnen.
 * <p>
 * Zu den JDBC-Klassen mit Factory-Methoden, die nebenl&auml;fig aufgerufen
 * werden k&ouml;nnen, dazu geh&ouml;ren alle Klassen die nicht mittelbar oder
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
 * @param <M>  der generische Typ einer Map die gleichzeitig auch das Interface
 *             {@link ModificationStampingObject} implementiert.
 * @param <T1> der Typ der jeweiligen abgeleiteten JDBC-Klasse.
 */
public abstract class ConcurrentFactoryJdbcProxy<M extends Map<Object, Object> & ModificationStampingObject, T1> extends FactoryJdbcProxy<T1> {

    private final ConcurrentMapFunktions<M, Object, Object> concurrentMapFunktions;
    private final StampedLock stampedLock;

    @SuppressWarnings("unchecked")
    protected ConcurrentFactoryJdbcProxy(T1 delegate) {
        this(delegate, (M) new WeakIdentityHashMap<>(), new StampedLock());
    }

    private ConcurrentFactoryJdbcProxy(T1 delegate, final M identityMap, final StampedLock stampedLock) {
        super(delegate, identityMap);
        this.stampedLock = stampedLock;
        this.concurrentMapFunktions = new ConcurrentMapFunktions<>(identityMap);
    }

    public boolean isValid(int timeoutSeconds) throws SQLException {
        return true;
    }

    @SuppressWarnings("unchecked")
    protected final <T2> T2 wrapConcurrent(T2 delegate, BiFunction<T2, Object[], T2> objectMaker, final Object... argumentArray) throws SQLException {
        try {
            return (T2) this.concurrentMapFunktions.computeIfAbsent(stampedLock, delegate, (k) -> objectMaker.apply(delegate, argumentArray));
        } catch (InterruptedException | TimeoutException ex) {
            throw new SQLException(ex.toString(), ex);
        }
    }
}
