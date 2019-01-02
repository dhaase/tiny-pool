package eu.dirk.haase.jdbc.pool.util;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DummyDataSource {

    private final boolean singleton;
    private Connection connection;
    private DataSource dataSource;
    private ResultSet resultSet;
    private Statement statement;

    public DummyDataSource(final boolean singleton) {
        this.singleton = singleton;
    }

    public Connection newConnection() {
        if (singleton && (connection != null)) {
            return connection;
        }
        Class<?>[] ifaces = {Connection.class};
        return connection = (Connection) Proxy.newProxyInstance(DummyDataSource.class.getClassLoader(), ifaces, new ConnectionHandler());
    }

    public DataSource newDataSource() {
        if (singleton && (dataSource != null)) {
            return dataSource;
        }
        Class<?>[] ifaces = {DataSource.class};
        return dataSource = (DataSource) Proxy.newProxyInstance(DummyDataSource.class.getClassLoader(), ifaces, new DataSourceHandler());
    }

    public ResultSet newResultSet() {
        if (singleton && (resultSet != null)) {
            return resultSet;
        }
        Class<?>[] ifaces = {ResultSet.class};
        return resultSet = (ResultSet) Proxy.newProxyInstance(DummyDataSource.class.getClassLoader(), ifaces, new ResultSetHandler());
    }

    public Statement newStatement() {
        if (singleton && (statement != null)) {
            return statement;
        }
        Class<?>[] ifaces = {Statement.class};
        return statement = (Statement) Proxy.newProxyInstance(DummyDataSource.class.getClassLoader(), ifaces, new StatementHandler());
    }

    class ConnectionHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("createStatement".equals(method.getName())) {
                return newStatement();
            }
            if ("isClosed".equals(method.getName())) {
                return false;
            }
            if ("getAutoCommit".equals(method.getName())) {
                return false;
            }
            if ("setAutoCommit".equals(method.getName())) {
                return null;
            }
            if ("toString".equals(method.getName())) {
                return "Proxy-Connection: " + this;
            }
            return null;
        }
    }

    class DataSourceHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getConnection".equals(method.getName())) {
                return newConnection();
            }
            if ("toString".equals(method.getName())) {
                return "Proxy-DataSource: " + this;
            }
            return null;
        }
    }

    class ResultSetHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("isClosed".equals(method.getName())) {
                return false;
            }
            if ("toString".equals(method.getName())) {
                return "Proxy-ResultSet: " + this;
            }
            return null;
        }
    }

    class StatementHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("executeQuery".equals(method.getName())) {
                return newResultSet();
            }
            if ("getResultSet".equals(method.getName())) {
                return newResultSet();
            }
            if ("getGeneratedKeys".equals(method.getName())) {
                return newResultSet();
            }
            if ("isClosed".equals(method.getName())) {
                return false;
            }
            if ("toString".equals(method.getName())) {
                return "Proxy-Statement: " + this;
            }
            return null;
        }
    }

}
