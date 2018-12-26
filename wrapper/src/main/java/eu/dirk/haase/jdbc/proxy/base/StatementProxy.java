package eu.dirk.haase.jdbc.proxy.base;

import java.sql.Connection;
import java.sql.Statement;

public abstract class StatementProxy extends JdbcProxy<Statement> {

    private final Connection connection;

    protected StatementProxy(final Statement delegate, final Connection connection) {
        super(delegate);
        this.connection = connection;
    }

    public final Connection getConnection() {
        return connection;
    }

}


