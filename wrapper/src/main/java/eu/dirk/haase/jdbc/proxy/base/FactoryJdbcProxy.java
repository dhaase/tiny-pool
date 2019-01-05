package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.function.BiFunction;

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

    private final Map<Object, Object> identityMap;

    /**
     * Erzeugt ein JDBC-Objekt mit einer {@link WeakIdentityHashMap} als Cache.
     *
     * @param delegate das zugrundeliegende JDBC-Objekt.
     */
    protected FactoryJdbcProxy(final T1 delegate) {
        this(delegate, new WeakIdentityHashMap<>());
    }


    /**
     * Erzeugt ein JDBC-Objekt.
     *
     * @param delegate    das zugrundeliegende JDBC-Objekt.
     * @param identityMap eine Map-Implementation als Cache.
     */
    protected FactoryJdbcProxy(final T1 delegate, final Map<Object, Object> identityMap) {
        super(delegate);
        this.identityMap = identityMap;
    }

    /**
     * Liefert unmittelbar das Map-Objekt mit Eintr&auml;gen die in der
     * {@link #wrap(Object, BiFunction, Object...)} entstanden sind.
     * <p>
     * Als Schl&uuml;ssel dient das interne Objekt das dekoriert werden soll.
     * <p>
     * Als Wert wird das dekorierte Objekt verwendet.
     *
     * @return die Cache-Map.
     */
    public final Map<Object, Object> cacheMap() {
        return identityMap;
    }

    /**
     * Dekoriert ein Objekt, das bedeutet: es wird in ein anderes Objekt eingepackt.
     * <p>
     * Diese Methode erzeugt nur dann ein neues Wrapper-Objekt wenn zu der internen Instanz
     * noch kein Wrapper-Objekt erzeugt wurde.
     * <p>
     * Ein Identity-Cache verhindert das bereits dekorierte (eingepackte) Objekte ein
     * zweites Mal eingepackt werden.
     *
     * @param delegate      das interne Objekt das dekoriert werden soll.
     * @param objectMaker   Funktions-Objekt mit dem das Wrapper-Objekt erzeugt werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte (eingepackte) Objekt.
     */
    @SuppressWarnings("unchecked")
    protected final <T2> T2 wrap(T2 delegate, BiFunction<T2, Object[], T2> objectMaker, final Object... argumentArray) {
        return (T2) identityMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

}
