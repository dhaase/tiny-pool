package eu.dirk.haase.jdbc.proxy.base;

import java.sql.ResultSet;
import java.sql.Statement;

public abstract class ResultSetProxy extends JdbcProxy<ResultSet> {

    private final Statement statement;

    protected ResultSetProxy(final ResultSet delegate, final Statement statement) {
        super(delegate);
        this.statement = statement;
    }


    public final Statement getStatement() {
        return statement;
    }


}
