package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractConnectionProxy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class FF extends AbstractConnectionProxy {
    protected FF(Connection delegate, DataSource dataSource, Object[] argumentArray) throws SQLException {
        super(delegate, dataSource, argumentArray);
    }
}
