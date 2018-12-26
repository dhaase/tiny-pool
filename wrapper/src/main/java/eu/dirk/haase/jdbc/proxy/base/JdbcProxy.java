package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.IdentityCache;
import eu.dirk.haase.jdbc.proxy.common.Unwrapper;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class JdbcProxy<T1> {

    private final T1 delegate;
    private final Lock lock;
    private final Class<?> thisClass;

    protected JdbcProxy(final T1 delegate) {
        this.delegate = delegate;
        this.thisClass = getClass();
        this.lock = new ReentrantLock();
    }

    protected final SQLException checkException(SQLException e) {
        return e;
    }

    public final T1 getDelegate() {
        return delegate;
    }

    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return Unwrapper.isWrapperFor(iface, this, this.delegate);
    }

    public final <T> T unwrap(Class<T> iface) throws SQLException {
        return Unwrapper.unwrap(iface, this, this.delegate);
    }

    protected final <T> T wrap(T delegate, Function<Object,T> objectMaker) {
        if (thisClass != delegate.getClass()) {
            return IdentityCache.getSingleton().get(delegate, objectMaker);
        } else {
            throw new IllegalStateException("Can not wrap twice: " + this.thisClass);
        }
    }

    protected final <T> T wrapSynchronized(T delegate, Function<Object,T> objectMaker) {
        try {
            if (this.lock.tryLock(10, TimeUnit.SECONDS)) {
                return wrap(delegate, objectMaker);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while acquiring the lock in class " + this.thisClass);
        }
        throw new IllegalStateException("Waiting time 10 sec is elapsed before the lock was acquired in class " + this.thisClass);
    }

}
