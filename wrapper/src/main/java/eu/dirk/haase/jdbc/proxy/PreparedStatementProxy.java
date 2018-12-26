package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.Connection;
import java.sql.PreparedStatement;

public abstract class PreparedStatementProxy extends FactoryJdbcProxy<PreparedStatement> {

    private final Connection connection;

    protected PreparedStatementProxy(PreparedStatement delegate, Connection connection) {
        super(delegate);
        this.connection = connection;
    }

    public final Connection getConnection() {
        return connection;
    }


}
