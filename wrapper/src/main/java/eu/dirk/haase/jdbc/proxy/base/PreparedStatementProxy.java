package eu.dirk.haase.jdbc.proxy.base;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class PreparedStatementProxy extends JdbcProxy<PreparedStatement> {

    private final Connection connection;

    protected PreparedStatementProxy(Connection connection, PreparedStatement delegate) {
        super(delegate);
        this.connection = connection;
    }

    public final Connection getConnection() {
        return connection;
    }


}
