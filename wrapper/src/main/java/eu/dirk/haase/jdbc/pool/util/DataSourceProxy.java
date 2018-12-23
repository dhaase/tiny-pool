package eu.dirk.haase.jdbc.pool.util;

import javax.sql.DataSource;
import java.sql.SQLException;

public class DataSourceProxy {

    public final DataSource delegate;

    public DataSourceProxy(final DataSource delegate) {
        this.delegate = delegate;
    }

    protected SQLException checkException(SQLException e) {
        return e;
    }
}
