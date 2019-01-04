package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.JdbcProxy;

import java.sql.ResultSet;
import java.sql.Statement;

public abstract class AbstractResultSetProxy extends JdbcProxy<ResultSet> implements CloseState {

    private final ResultSet delegate;
    private final Statement statement;

    protected AbstractResultSetProxy(final ResultSet delegate, final Statement statement, final Object[] argumentArray) {
        super(delegate);
        this.statement = statement;
        this.delegate = delegate;
    }

    public final Statement getStatement() {
        return statement;
    }


}
