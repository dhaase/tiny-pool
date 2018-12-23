package eu.dirk.haase.jdbc.pool.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class ResultSetProxy {

    private final ResultSet delegate;
    private final Statement statement;

    protected ResultSetProxy(final Statement statement, final ResultSet delegate) {
        this.statement = statement;
        this.delegate = delegate;
    }


    public final Statement getStatement() {
        return statement;
    }


    public final ResultSet getDelegate() {
        return delegate;
    }


    protected final SQLException checkException(SQLException e) {
        return e;
    }


}
