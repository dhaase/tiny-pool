package eu.dirk.haase.jdbc.proxy.hybrid;

import eu.dirk.haase.jdbc.proxy.factory.DataSourceWrapper;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
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
 * <li>und die Instanz-Variablen durch Wrapper die zus&auml;tzliche Funktionalit&auml;ten bieten.</li>
 * </ul>
 * @see DataSource
 * @see XADataSource
 * @see DataSourceWrapper.Hybrid
 */
public final class XADataSourceHybrid extends AbstractHybrid implements XADataSource, DataSource {

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

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSourceProxy.isWrapperFor(iface);
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSourceProxy.unwrap(iface);
    }

}
