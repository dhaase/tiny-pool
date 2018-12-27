package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.JdbcProxy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class ResultSetProxy extends JdbcProxy<ResultSet> implements CloseState {

    private final ResultSet delegate;
    private final Statement statement;

    protected ResultSetProxy(final ResultSet delegate, final Statement statement, final Object[] argumentArray) {
        super(delegate);
        this.statement = statement;
        this.delegate = delegate;
    }

    public final Statement getStatement() {
        return statement;
    }

    @Override
    public final boolean isClosed() throws SQLException {
        return delegate.isClosed();
    }


}
