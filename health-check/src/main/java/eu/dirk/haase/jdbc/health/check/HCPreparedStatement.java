package eu.dirk.haase.jdbc.health.check;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class HCPreparedStatement extends HCStatement implements PreparedStatement {
    private final PreparedStatement delegate;

    protected HCPreparedStatement(PreparedStatement delegate, Connection connection, Object[] argumentArray) {
        super(delegate, connection, argumentArray);
        this.delegate = delegate;
    }

    @Override
    public final boolean execute() throws SQLException {
        ensureUpAndRunning();
        try {
            return delegate.execute();
        } catch (Throwable ex) {
            throw checkException(ex);
        }
    }


}
