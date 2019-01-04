package eu.dirk.haase.jdbc.proxy.hybrid;

import eu.dirk.haase.jdbc.proxy.common.Unwrapper;
import eu.dirk.haase.jdbc.proxy.factory.DataSourceWrapper;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Wrapper;
import java.util.logging.Logger;

/**
 * Diese Wrapper-Klasse implementiert den Sonderfall das ein und dieselbe DataSource-Instanz
 * gleichzeitig die Interfaces {@link XADataSource} und {@link DataSource} implementieren.
 * <p>
 * Instanzen dieser Klasse werden gew&ouml;hnlich erzeugt durch die Factory
 * {@link DataSourceWrapper.Hybrid}.
 * <p>
 * Hinweis: Da diese Wrapper-Klasse keine zus&auml;tzliche Funktionalit&auml;t bietet
 * kann sie nur dann sinnvoll eingesetzt werden, wenn
 * <ul>
 * <li>die zugrundeliegende Instanzen der Instanz-Variablen
 * {@link XADataSourceHybrid#dataSourceProxy} und
 * {@link XADataSourceHybrid#xaDataSourceProxy} die identisch sind
 * (siehe {@link XADataSourceHybrid#XADataSourceHybrid(DataSource, XADataSource)}).</li>
 * <li>und die Instanz-Variablen durch Wrapper die gew&uuml;nschten zus&auml;tzliche Funktionalit&auml;ten erhalten.</li>
 * </ul>
 *
 * @see DataSource
 * @see XADataSource
 * @see DataSourceWrapper.Hybrid
 */
public final class XADataSourceHybrid extends AbstractDataSourceHybrid implements XADataSource, DataSource {

    private final DataSource dataSourceProxy;
    private final XADataSource xaDataSourceProxy;

    /**
     * Erzeugt eine DataSource-Instanz das die Interfaces {@link XADataSource}
     * und {@link DataSource} implementiert.
     * <p>
     * Hinweis: Die ursp&uuml;ngliche Instanzen der beiden Parameter m&uuml;ssen identisch
     * sein. Sichergestellt wird dies durch die Factory {@link DataSourceWrapper.Hybrid}.
     *
     * @param dataSourceProxy   eine Proxy-Instanz deren ursp&uuml;ngliche Instanz identisch
     *                          ist mit der ursp&uuml;ngliche Instanz des zweiten Parameters.
     * @param xaDataSourceProxy eine Proxy-Instanz deren ursp&uuml;ngliche Instanz identisch
     *                          ist mit der ursp&uuml;ngliche Instanz des ersten Parameters.
     */
    public XADataSourceHybrid(final DataSource dataSourceProxy, final XADataSource xaDataSourceProxy) {
        super(dataSourceProxy);
        this.dataSourceProxy = dataSourceProxy;
        this.xaDataSourceProxy = xaDataSourceProxy;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSourceProxy.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return dataSourceProxy.getConnection(username, password);
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
    public XAConnection getXAConnection() throws SQLException {
        return xaDataSourceProxy.getXAConnection();
    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        return xaDataSourceProxy.getXAConnection(user, password);
    }

    public XADataSource getXaDataSourceProxy() {
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
        if (Unwrapper.isWrapperFor(iface, this, this.dataSourceProxy)) {
            return Unwrapper.unwrap(iface, this, this.dataSourceProxy);
        } else {
            return Unwrapper.unwrap(iface, this, this.xaDataSourceProxy);
        }
    }

}
