package eu.dirk.haase.jdbc.pool.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class DataSourceProxy {

    private final DataSource delegate;

    protected DataSourceProxy(final DataSource delegate) {
        this.delegate = delegate;
    }


    public final DataSource getDelegate() {
        return delegate;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }

}
