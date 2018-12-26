package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.Connection;
import java.sql.Statement;

public abstract class StatementProxy extends FactoryJdbcProxy<Statement> {

    private final Connection connection;

    protected StatementProxy(final Statement delegate, final Connection connection) {
        super(delegate);
        this.connection = connection;
    }

    public final Connection getConnection() {
        return connection;
    }

}


