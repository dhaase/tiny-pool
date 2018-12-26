package eu.dirk.haase.jdbc.proxy.base;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public class ConcurrentFactoryJdbcProxy<T> extends FactoryJdbcProxy<T> {
    private final Lock lock;

    protected ConcurrentFactoryJdbcProxy(T delegate) {
        super(delegate);
        this.lock = new ReentrantLock();
    }

    protected final <T> T wrapConcurrent(T delegate, Function<T, T> objectMaker) {
        try {
            if (this.lock.tryLock(10, TimeUnit.SECONDS)) {
                return wrap(delegate, objectMaker);
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while acquiring the lock in class " + getClass());
        }
        throw new IllegalStateException("Waiting time 10 sec is elapsed before the lock was acquired in class " + getClass());
    }

}
