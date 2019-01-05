package eu.dirk.haase.jdbc.proxy.hybrid;

import eu.dirk.haase.jdbc.proxy.common.Unwrapper;
import eu.dirk.haase.jdbc.proxy.factory.DataSourceWrapper;

import javax.sql.*;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.logging.Logger;

/**
 * Diese Wrapper-Klasse implementiert den Sonderfall das ein und dieselbe DataSource-Instanz
 * gleichzeitig die Interfaces {@link ConnectionPoolDataSource}, {@link XADataSource} und
 * {@link DataSource} implementieren.
 * <p>
 * Instanzen dieser Klasse werden gew&ouml;hnlich erzeugt durch die Factory
 * {@link DataSourceWrapper.Hybrid}.
 * <p>
 * Hinweis: Da diese Wrapper-Klasse keine zus&auml;tzliche Funktionalit&auml;t bietet
 * kann sie nur dann sinnvoll eingesetzt werden, wenn
 * <ul>
 * <li>die zugrundeliegende Instanzen der Instanz-Variablen
 * {@link ConnectionPoolXADataSourceHybrid#dataSourceProxy}, {@link ConnectionPoolXADataSourceHybrid#xaDataSourceProxy} und
 * {@link ConnectionPoolXADataSourceHybrid#connectionPoolDataSourceProxy} die identisch sind
 * (siehe {@link ConnectionPoolXADataSourceHybrid#ConnectionPoolXADataSourceHybrid(DataSource, ConnectionPoolDataSource, XADataSource)}).</li>
 * <li>und die Instanz-Variablen durch Wrapper die gew&uuml;nschten zus&auml;tzliche Funktionalit&auml;ten erhalten.</li>
 * </ul>
 *
 * @see DataSourceWrapper.Hybrid#wrapConnectionPoolXADataSourceHybrid(javax.sql.ConnectionPoolDataSource)
 * @see ConnectionPoolDataSource
 * @see XADataSource
 * @see DataSource
 */
public final class ConnectionPoolXADataSourceHybrid extends AbstractDataSourceHybrid implements ConnectionPoolDataSource, XADataSource, DataSource {

    private final ConnectionPoolDataSource connectionPoolDataSourceProxy;
    private final DataSource dataSourceProxy;
    private final XADataSource xaDataSourceProxy;

    /**
     * Erzeugt eine DataSource-Instanz das die Interfaces {@link XADataSource}, {@link ConnectionPoolDataSource}
     * und {@link DataSource} implementiert.
     * <p>
     * Hinweis: Die ursp&uuml;ngliche Instanz der beiden Parameter m&uuml;ssen identisch
     * sein. Sichergestellt wird dies durch die Factory {@link DataSourceWrapper.Hybrid}.
     *
     * @param dataSourceProxy               eine Proxy-Instanz deren ursp&uuml;ngliche Instanz identisch
     *                                      ist mit der ursp&uuml;ngliche Instanz des zweiten Parameters.
     * @param connectionPoolDataSourceProxy eine Proxy-Instanz deren ursp&uuml;ngliche Instanz identisch
     *                                      ist mit den ursp&uuml;nglichen Instanzen des ersten und dritten
     *                                      Parameters.
     * @param xaDataSourceProxy             eine Proxy-Instanz deren ursp&uuml;ngliche Instanz identisch
     *                                      ist mit den ursp&uuml;nglichen Instanzen des ersten und zweiten
     *                                      Parameters.
     */
    public ConnectionPoolXADataSourceHybrid(final DataSource dataSourceProxy, final ConnectionPoolDataSource connectionPoolDataSourceProxy, final XADataSource xaDataSourceProxy) {
        super(dataSourceProxy);
        this.dataSourceProxy = dataSourceProxy;
        this.xaDataSourceProxy = xaDataSourceProxy;
        this.connectionPoolDataSourceProxy = connectionPoolDataSourceProxy;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSourceProxy.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return dataSourceProxy.getConnection(username, password);
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
    public PrintWriter getLogWriter() throws SQLException {
        return dataSourceProxy.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSourceProxy.setLogWriter(out);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSourceProxy.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSourceProxy.setLoginTimeout(seconds);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSourceProxy.getParentLogger();
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return connectionPoolDataSourceProxy.getPooledConnection();
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        return connectionPoolDataSourceProxy.getPooledConnection(user, password);
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return xaDataSourceProxy.getXAConnection();
    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        return xaDataSourceProxy.getXAConnection(user, password);
    }

    /**
     * Liefert das zugrundeliegende {@link XADataSource}-Proxy Objekt.
     *
     * @return das zugrundeliegende {@link XADataSource}-Proxy Objekt.
     */
    public XADataSource getXADataSourceProxy() {
        return xaDataSourceProxy;
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
     * <li>und zuletzt an das interne Wrapper-Objekt das das Interface
     * {@link XADataSource} implementiert.</li>
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
    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (Unwrapper.isWrapperFor(iface, this, this.dataSourceProxy)) {
            return true;
        } else if (Unwrapper.isWrapperFor(iface, this, this.connectionPoolDataSourceProxy)) {
            return true;
        } else {
            return Unwrapper.isWrapperFor(iface, this, this.xaDataSourceProxy);
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
     * <li>und zuletzt an das interne Wrapper-Objekt das das Interface
     * {@link XADataSource} implementiert.</li>
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
    public final <T2> T2 unwrap(Class<T2> iface) throws SQLException {
        if (Unwrapper.isWrapperFor(iface, this, this.dataSourceProxy)) {
            return Unwrapper.unwrap(iface, this, this.dataSourceProxy);
        } else if (Unwrapper.isWrapperFor(iface, this, this.connectionPoolDataSourceProxy)) {
            return Unwrapper.unwrap(iface, this, this.connectionPoolDataSourceProxy);
        } else {
            return Unwrapper.unwrap(iface, this, this.xaDataSourceProxy);
        }
    }

}
