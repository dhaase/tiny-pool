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

    /**
     * Erzeugt ein JDBC-Objekt mit einer {@link WeakIdentityHashMap} als Cache und einem
     * {@link StampedLock} als Sperre zur Synchronisation der
     * {@link #wrapConcurrent(Object, BiFunction, Object...)}-Methode.
     *
     * @param delegate das zugrundeliegende JDBC-Objekt.
     */
    @SuppressWarnings("unchecked")
    protected ConcurrentFactoryJdbcProxy(final Class<T1> type, T1 delegate) {
        this(type, delegate, (M) new WeakIdentityHashMap<>(), new StampedLock());
    }

    /**
     * Erzeugt ein JDBC-Objekt.
     *
     * @param delegate    das zugrundeliegende JDBC-Objekt.
     * @param identityMap eine Map-Implementation als Cache das auch das Interface
     *                    {@link ModificationStampingObject} implementiert.
     * @param stampedLock die {@link StampedLock}-Sperre zur Synchronisation in der
     *                    {@link #wrapConcurrent(Object, BiFunction, Object...)}-Methode.
     */
    private ConcurrentFactoryJdbcProxy(final Class<T1> type, T1 delegate, final M identityMap, final StampedLock stampedLock) {
        super(type, delegate, identityMap);
        this.stampedLock = stampedLock;
        this.concurrentMapFunktions = new ConcurrentMapFunktions<>(identityMap);
    }

    public boolean isValid(int timeoutSeconds) throws SQLException {
        return true;
    }


    /**
     * Dekoriert ein Objekt, das bedeutet: es wird in ein anderes Objekt eingepackt.
     * <p>
     * Diese Methode erzeugt nur dann ein neues Wrapper-Objekt wenn zu der internen Instanz
     * noch kein Wrapper-Objekt erzeugt wurde.
     * <p>
     * Ein Identity-Cache verhindert das bereits dekorierte (eingepackte) Objekte ein
     * zweites Mal eingepackt werden.
     * <p>
     * Diese Methode verwendet eine {@link StampedLock}-Sperre zur Synchronisation und
     * kann daher nebenl&auml;fig ausgef&uuml;hrt werden.
     *
     * @param delegate      das interne Objekt das dekoriert werden soll.
     * @param objectMaker   Funktions-Objekt mit dem das Wrapper-Objekt erzeugt werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte (eingepackte) Objekt.
     */
    @SuppressWarnings("unchecked")
    protected final <T2> T2 wrapConcurrent(T2 delegate, BiFunction<T2, Object[], T2> objectMaker, final Object... argumentArray) throws SQLException {
        try {
            return (T2) this.concurrentMapFunktions.computeIfAbsent(stampedLock, delegate, (k) -> objectMaker.apply(delegate, argumentArray));
        } catch (InterruptedException | TimeoutException ex) {
            throw new SQLException(ex.toString(), ex);
        }
    }
}
