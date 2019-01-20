package eu.dirk.haase.jdbc.health.check;

import eu.dirk.haase.jdbc.proxy.AbstractConnectionProxy;

import javax.sql.DataSource;
import java.sql.*;

public abstract class HCConnection extends AbstractConnectionProxy {

    private final Connection delegate;

    protected HCConnection(Connection delegate, DataSource dataSource, Object[] argumentArray) throws SQLException {
        super(delegate, dataSource, argumentArray);
        this.delegate = delegate;
    }

    @Override
    protected final SQLException checkException(Throwable ex) {
        return getHcDataSource().checkException(ex);
    }

    @Override
    public final Statement createStatement() throws SQLException {
        ensureUpAndRunning();
        try {
            return delegate.createStatement();
        } catch (Throwable ex) {
            throw checkException(ex);
        }
    }

    final void ensureUpAndRunning() throws SQLException {
        getHcDataSource().ensureUpAndRunning();
    }

    final HCDataSource getHcDataSource() {
        return (HCDataSource) this.getDataSourceProxy();
    }

    @Override
    public final CallableStatement prepareCall(String sql) throws SQLException {
        ensureUpAndRunning();
        try {
            return delegate.prepareCall(sql);
        } catch (Throwable ex) {
            throw checkException(ex);
        }
    }

    @Override
    public final PreparedStatement prepareStatement(String sql) throws SQLException {
        ensureUpAndRunning();
        try {
            return delegate.prepareStatement(sql);
        } catch (Throwable ex) {
            throw checkException(ex);
        }
    }
}
