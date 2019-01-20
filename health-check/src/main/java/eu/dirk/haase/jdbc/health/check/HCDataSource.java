package eu.dirk.haase.jdbc.health.check;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

public abstract class HCDataSource extends AbstractDataSourceProxy {

    private final static Predicate<Throwable> isFatal = OracleExceptionAnalyzer.isFatal;

    private final DataSource delegate;
    private final AtomicBoolean isRunning;
    private final AtomicReference<Throwable> lastFailure;
    private final AtomicLong lastFailureTimeMillis;
    private final AtomicLong thresholdFailureMillis;

    protected HCDataSource(DataSource delegate) {
        super(delegate);
        this.delegate = delegate;
        this.isRunning = new AtomicBoolean(true);
        this.lastFailure = new AtomicReference();
        this.lastFailureTimeMillis = new AtomicLong();
        this.thresholdFailureMillis = new AtomicLong(1000L);
    }

    @Override
    protected final SQLException checkException(Throwable ex) {
        registerFatalException(ex);
        return super.checkException(ex);
    }

    final boolean checkLastFailure() {
        return (System.currentTimeMillis() - lastFailureTimeMillis.get()) > thresholdFailureMillis.get();
    }

    final void ensureUpAndRunning() throws SQLException {
        if (!isUpAndRunning()) {
            throw new SQLException("Database is down since " + new Timestamp(lastFailureTimeMillis.get()), lastFailure.get());
        }
    }

    @Override
    public final Connection getConnection() throws SQLException {
        ensureUpAndRunning();
        try {
            return delegate.getConnection();
        } catch (Throwable ex) {
            throw checkException(ex);
        }
    }

    @Override
    public final Connection getConnection(String username, String password) throws SQLException {
        ensureUpAndRunning();
        try {
            return delegate.getConnection(username, password);
        } catch (Throwable ex) {
            throw checkException(ex);
        }
    }

    public final long getLastFailureTimeMillis() {
        return lastFailureTimeMillis.get();
    }

    public final boolean getRunning() {
        return isRunning.get();
    }

    public final void setRunning(boolean isRunning) {
        this.isRunning.set(isRunning);
    }

    final boolean isUpAndRunning() {
        return (isRunning.get() && checkLastFailure());
    }

    @Override
    public final boolean isValid(int timeoutSeconds) throws SQLException {
        if (isUpAndRunning()) {
            return super.isValid(timeoutSeconds);
        }
        return false;
    }

    final void registerFatalException(Throwable ex) {
        if (isFatal.test(ex)) {
            lastFailure.set(ex);
            lastFailureTimeMillis.set(System.currentTimeMillis());
        }
    }

}
