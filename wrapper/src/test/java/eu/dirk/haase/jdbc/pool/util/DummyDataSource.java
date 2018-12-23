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

    private DataSource dataSource;
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;

    public DummyDataSource(final boolean singleton) {
        this.singleton = singleton;
    }

    public DataSource newDataSource() {
        if (singleton && (dataSource != null)) {
            return dataSource;
        }
        Class<?>[] ifaces = {DataSource.class};
        return dataSource = (DataSource) Proxy.newProxyInstance(DummyDataSource.class.getClassLoader(), ifaces, new DataSourceHandler());
    }

    public Connection newConnection() {
        if (singleton && (connection != null)) {
            return connection;
        }
        Class<?>[] ifaces = {Connection.class};
        return connection = (Connection) Proxy.newProxyInstance(DummyDataSource.class.getClassLoader(), ifaces, new ConnectionHandler());
    }

    public Statement newStatement() {
        if (singleton && (statement != null)) {
            return statement;
        }
        Class<?>[] ifaces = {Statement.class};
        return statement = (Statement) Proxy.newProxyInstance(DummyDataSource.class.getClassLoader(), ifaces, new StatementHandler());
    }

    public ResultSet newResultSet() {
        if (singleton && (resultSet != null)) {
            return resultSet;
        }
        Class<?>[] ifaces = {ResultSet.class};
        return resultSet = (ResultSet) Proxy.newProxyInstance(DummyDataSource.class.getClassLoader(), ifaces, new ResultSetHandler());
    }

    class DataSourceHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getConnection".equals(method.getName())) {
                return newConnection();
            }
            return null;
        }
    }

    class ConnectionHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("createStatement".equals(method.getName())) {
                return newStatement();
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
            return null;
        }
    }


    class ResultSetHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return null;
        }
    }

}
