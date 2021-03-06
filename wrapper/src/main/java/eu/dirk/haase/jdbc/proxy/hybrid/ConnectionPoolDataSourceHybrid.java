package eu.dirk.haase.jdbc.proxy.hybrid;

import eu.dirk.haase.jdbc.proxy.common.Unwrapper;
import eu.dirk.haase.jdbc.proxy.factory.DataSourceWrapperFactory;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import java.sql.SQLException;
import java.sql.Wrapper;

/**
 * Diese Wrapper-Klasse implementiert den Sonderfall das ein und dieselbe DataSource-Instanz
 * gleichzeitig die Interfaces {@link ConnectionPoolDataSource} und {@link DataSource}
 * implementieren.
 * <p>
 * Instanzen dieser Klasse werden gew&ouml;hnlich erzeugt durch die Factory
 * {@link DataSourceWrapperFactory.Hybrid}.
 * <p>
 * Hinweis: Da diese Wrapper-Klasse keine zus&auml;tzliche Funktionalit&auml;t bietet
 * kann sie nur dann sinnvoll eingesetzt werden, wenn
 * <ul>
 * <li>die zugrundeliegende Instanzen der Instanz-Variablen
 * {@link ConnectionPoolDataSourceHybrid#dataSourceProxy} und
 * {@link ConnectionPoolDataSourceHybrid#connectionPoolDataSourceProxy} die identisch sind
 * (siehe {@link ConnectionPoolDataSourceHybrid#ConnectionPoolDataSourceHybrid(DataSource, ConnectionPoolDataSource)}).</li>
 * <li>und die Instanz-Variablen durch Wrapper die gew&uuml;nschten zus&auml;tzliche Funktionalit&auml;ten erhalten.</li>
 * </ul>
 *
 * @see DataSourceWrapperFactory.Hybrid#wrapConnectionPoolDataSourceHybrid(javax.sql.DataSource)
 * @see DataSourceWrapperFactory.Hybrid#wrapAutoType(javax.sql.DataSource)
 * @see ConnectionPoolDataSource
 * @see DataSource
 */
public final class ConnectionPoolDataSourceHybrid extends AbstractDataSourceHybrid implements ConnectionPoolDataSource {

    private final ConnectionPoolDataSource connectionPoolDataSourceProxy;
    private final DataSource dataSourceProxy;

    /**
     * Erzeugt eine DataSource-Instanz das die Interfaces {@link DataSource}
     * und {@link ConnectionPoolDataSource} implementiert.
     * <p>
     * Hinweis: Die ursp&uuml;ngliche Instanzen der beiden Parameter m&uuml;ssen identisch
     * sein. Sichergestellt wird dies durch die Factory {@link DataSourceWrapperFactory.Hybrid}.
     *
     * @param dataSourceProxy               eine Proxy-Instanz deren ursp&uuml;ngliche Instanz identisch
     *                                      ist mit der ursp&uuml;ngliche Instanz des zweiten Parameters.
     * @param connectionPoolDataSourceProxy eine Proxy-Instanz deren ursp&uuml;ngliche Instanz identisch
     *                                      ist mit der ursp&uuml;ngliche Instanz des ersten Parameters.
     */
    public ConnectionPoolDataSourceHybrid(final DataSource dataSourceProxy, final ConnectionPoolDataSource connectionPoolDataSourceProxy) {
        super(dataSourceProxy);
        this.dataSourceProxy = dataSourceProxy;
        this.connectionPoolDataSourceProxy = connectionPoolDataSourceProxy;
    }

    /**
     * Liefert das zugrundeliegende {@link ConnectionPoolDataSource}-Proxy Objekt.
     *
     * @return das zugrundeliegende {@link ConnectionPoolDataSource}-Proxy Objekt.
     */
    public ConnectionPoolDataSource getConnectionPoolDataSourceProxy() {
        return connectionPoolDataSourceProxy;
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        return connectionPoolDataSourceProxy.getPooledConnection(user, password);
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return connectionPoolDataSourceProxy.getPooledConnection();
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
     * Da es sich hier um ein Objekt handelt das mehrere Wrapper-Objekte enth&auml;hlt wird
     * der Aufruf in folgender Reihenfolge abgearbeitet:
     * <ol>
     * <li>zuerst an das interne Wrapper-Objekt das das Interface
     * {@link DataSource} implementiert.</li>
     * <li>dann an das interne Wrapper-Objekt das das Interface
     * {@link ConnectionPoolDataSource} implementiert.</li>
     * </ol>
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
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (Unwrapper.isWrapperFor(iface, this, this.dataSourceProxy)) {
            return true;
        } else {
            return Unwrapper.isWrapperFor(iface, this, this.connectionPoolDataSourceProxy);
        }
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
     * Da es sich hier um ein Objekt handelt das mehrere Wrapper-Objekte enth&auml;hlt wird
     * der Aufruf in folgender Reihenfolge gepr&uuml;ft:
     * <ol>
     * <li>zuerst an das interne Wrapper-Objekt das das Interface
     * {@link DataSource} implementiert.</li>
     * <li>dann an das interne Wrapper-Objekt das das Interface
     * {@link ConnectionPoolDataSource} implementiert.</li>
     * </ol>
     * Eventuelle Fehler werden wahrscheinlich mit dem letzten Aufruf ausgel&ouml;st.
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
     * @throws SQLException wird ausgel&ouml;st wenn kein Objekt gefunden werden konnte das
     *                      von der angegebenen Klasse oder Interface abstammt.
     */
    @Override
    public <T2> T2 unwrap(Class<T2> iface) throws SQLException {
        if (Unwrapper.isWrapperFor(iface, this, this.dataSourceProxy)) {
            return Unwrapper.unwrap(iface, this, this.dataSourceProxy);
        } else {
            return Unwrapper.unwrap(iface, this, this.connectionPoolDataSourceProxy);
        }
    }

}
