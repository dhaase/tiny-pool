package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractConnectionProxy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class MyWrapConnection extends AbstractConnectionProxy implements Connection {
    protected MyWrapConnection(Connection delegate, DataSource dataSource, Object[] argumentArray) throws SQLException {
        super(delegate, dataSource, argumentArray);
    }

    @Override
    public final String nativeSQL(String sql) throws SQLException {
        return String.valueOf(System.nanoTime());
    }
}
