package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.Unwrapper;

import java.sql.SQLException;
import java.sql.Wrapper;

/**
 * Basis-Klasse f&uuml;r alle JDBC-Klassen die eingepackt werden sollen.
 * <p>
 * Im Gegensatz zu der JDBC-API Implementiert diese Klasse das Interface
 * {@link Wrapper} f&uuml;r alle JDBC-Klassen.
 *
 * @param <T1> der Typ der jeweiligen abgeleiteten JDBC-Klasse.
 */
public abstract class JdbcProxy<T1> implements JdbcWrapper {

    private final T1 delegate;

    protected JdbcProxy(final T1 delegate) {
        this.delegate = delegate;
    }

    protected SQLException checkException(SQLException e) {
        return e;
    }

    /**
     * Liefert {@code true} wenn das Argument-Objekt auch ein Wrapper-Objekt ist
     * und das gleiche JDBC-Objekt wie dieses Wrapper-Objekt enth&auml;hlt.
     *
     * @param thatObj das andere Objekt das gepr&uuml;ft werden soll.
     * @return {@code true} wenn dieses Wrapper-Objekt gegen&uuml;ber dem Argument
     * das gleiche JDBC-Objekt enth&auml;hlt.
     */
    @Override
    public final boolean equals(Object thatObj) {
        if (this == thatObj) return true;
        if (!(thatObj instanceof JdbcProxy)) return false;

        JdbcProxy<?> that = (JdbcProxy<?>) thatObj;

        return this.delegate != null ? this.delegate.equals(that.delegate) : that.delegate == null;
    }

    public final T1 getDelegate() {
        return delegate;
    }

    /**
     * Liefert den Hash-Code des JDBC-Objekts, das dieses Wrapper-Objekt enth&auml;hlt.
     *
     * @return den Hash-Code des JDBC-Objekts, das dieses Wrapper-Objekt enth&auml;hlt.
     */
    @Override
    public final int hashCode() {
        return delegate != null ? delegate.hashCode() : 0;
    }

    /**
     * Gibt {@code true} zur&uuml;ck, wenn dieses Objekt entweder von der angegebenen Klasse
     * oder vom Interface abstammt oder direkt oder indirekt ein Wrapper ist f&uuml;r
     * die angegebenen Klasse oder f&uuml;r das Interface ist.
     * <p>
     * Die Spezifikation in {@link Wrapper#isWrapperFor(Class)} impliziert das nur Interfaces
     * als Argument erlaubt sind.
     * Im Gegensatz dazu ist diese Implementation nicht auf Interfaces beschr&auml;nkt sondern
     * kann auch mit Klassen erfolgreich aufgerufen werden.
     * <p>
     * Damit ist es m&ouml;glich, neben den nativen Datenbank-Objekten, auch die Wrapper-Klassen
     * selbst abzufragen.
     * <p>
     * Wenn als Argument Klassen angegeben werden, dann sollte (bei Objekten unbekannter Herkunft)
     * allerdings die Pr&uuml;ung zweistufig erfolgen um im Zweifel keinen Fehler auszul&ouml;sen:
     * <pre><code>
     * Connection connection = ...
     * Class&lt;?&gt; wrapper = MyConnectionProxy.class;
     * if (JdbcWrapper.class.isInstance(connection)) {
     *     return connection.isWrapperFor(wrapper);
     * } else {
     *     return false;
     * }
     * </code></pre>
     *
     * @param iface die Klasse oder das Interface vom dieses Objekt direkt oder indirekt
     *              abstammen soll.
     * @return {@code true} wenn dieses Objekt entweder von der angegebenen Klasse
     * oder Interface abstammt oder direkt oder indirekt ein Wrapper ist
     * f&uuml;r angegebenen Klasse ist.
     * @throws SQLException wird ausgel&ouml;st wenn die Pr&uuml;fung nicht durchgef&uuml;hrt
     *                      werden kann.
     */
    @Override
    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return Unwrapper.isWrapperFor(iface, this, this.delegate);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "delegate=" + delegate +
                '}';
    }

    /**
     * Gibt da Objekt zur&uuml;ck das entweder von der angegebenen Klasse
     * oder vom Interface abstammt oder direkt oder indirekt ein Wrapper ist f&uuml;r
     * die angegebenen Klasse oder f&uuml;r das Interface ist.
     * <p>
     * Die Spezifikation in {@link Wrapper#unwrap(Class)} impliziert das nur Interfaces
     * als Argument erlaubt sind.
     * Im Gegensatz dazu ist diese Implementation nicht auf Interfaces beschr&auml;nkt sondern
     * kann auch mit Klassen erfolgreich aufgerufen werden.
     * <p>
     * Damit ist es m&ouml;glich, neben den nativen Datenbank-Objekten, auch die Wrapper-Klassen
     * selbst abzufragen.
     * <p>
     * Wenn als Argument Klassen angegeben werden, dann sollte (bei Objekten unbekannter Herkunft)
     * allerdings die Pr&uuml;ung zweistufig erfolgen um im Zweifel keinen Fehler auszul&ouml;sen:
     * <pre><code>
     * Connection connection = ...
     * Class&lt;?&gt; wrapper = MyConnectionProxy.class;
     * if (JdbcWrapper.class.isInstance(connection)) {
     *     if  (connection.isWrapperFor(wrapper)) {
     *         return connection.unwrap(wrapper);
     *     }
     * }
     * ...
     * </code></pre>
     *
     * @param iface die Klasse oder das Interface vom dieses Objekt direkt oder indirekt
     *              abstammen soll.
     * @return das Objekt entweder von der angegebenen Klasse
     * oder Interface abstammt oder direkt oder indirekt ein Wrapper ist f&uuml;r angegebenen
     * Klasse ist.
     * @throws SQLException wird ausgel&ouml;st wenn die Pr&uuml;fung nicht durchgef&uuml;hrt
     *                      werden kann.
     */
    @Override
    public final <T2> T2 unwrap(Class<T2> iface) throws SQLException {
        return Unwrapper.unwrap(iface, this, this.delegate);
    }
}
