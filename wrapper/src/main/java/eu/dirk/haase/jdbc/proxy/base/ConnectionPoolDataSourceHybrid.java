package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class ConnectionPoolDataSourceHybrid implements ConnectionPoolDataSource, DataSource {

    private final ConnectionPoolDataSource connectionPoolDataSource;
    private final DataSource dataSource;

    public ConnectionPoolDataSourceHybrid(final DataSource dataSource, final ConnectionPoolDataSource connectionPoolDataSource) {
        this.dataSource = dataSource;
        this.connectionPoolDataSource = connectionPoolDataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return dataSource.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return dataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        dataSource.setLogWriter(out);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return dataSource.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        dataSource.setLoginTimeout(seconds);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return dataSource.getParentLogger();
    }

    @Override
    public PooledConnection getPooledConnection(String user, String password) throws SQLException {
        return connectionPoolDataSource.getPooledConnection(user, password);
    }

    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return connectionPoolDataSource.getPooledConnection();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return dataSource.isWrapperFor(iface);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return dataSource.unwrap(iface);
    }
}
