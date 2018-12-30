package eu.dirk.haase.jdbc.proxy.hybrid;

import eu.dirk.haase.jdbc.proxy.factory.DataSourceWrapper;

import javax.sql.*;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
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
 * @see DataSourceWrapper.Hybrid
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
