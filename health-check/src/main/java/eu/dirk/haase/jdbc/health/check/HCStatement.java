package eu.dirk.haase.jdbc.health.check;

import eu.dirk.haase.jdbc.proxy.AbstractStatementProxy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class HCStatement extends AbstractStatementProxy {

    private final Statement delegate;

    protected HCStatement(Statement delegate, Connection connection, Object[] argumentArray) {
        super(delegate, connection, argumentArray);
        this.delegate = delegate;
    }

    @Override
    protected final SQLException checkException(Throwable ex) {
        return getHcConnection().checkException(ex);
    }

    final HCConnection getHcConnection() {
        return (HCConnection) this.getConnectionProxy();
    }

    @Override
    public final boolean execute(String sql) throws SQLException {
        ensureUpAndRunning();
        try {
            return delegate.execute(sql);
        } catch (Throwable ex) {
            throw checkException(ex);
        }
    }

    final void ensureUpAndRunning() throws SQLException {
        getHcConnection().ensureUpAndRunning();
    }

}
