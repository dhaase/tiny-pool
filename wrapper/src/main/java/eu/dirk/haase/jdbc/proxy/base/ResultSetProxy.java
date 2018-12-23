package eu.dirk.haase.jdbc.proxy.base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class ResultSetProxy extends JdbcProxy<ResultSet> {

    private final Statement statement;

    protected ResultSetProxy(final Statement statement, final ResultSet delegate) {
        super(delegate);
        this.statement = statement;
    }


    public final Statement getStatement() {
        return statement;
    }


}
