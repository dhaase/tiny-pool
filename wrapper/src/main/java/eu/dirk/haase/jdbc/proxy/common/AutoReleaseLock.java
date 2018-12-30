package eu.dirk.haase.jdbc.proxy.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class AutoReleaseLock implements Lock, AutoCloseable {
    private final Lock delegate;

    public AutoReleaseLock(final Lock delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        System.out.println("delegate.unlock()");
        delegate.unlock();
    }

    @Override
    public void lock() {
        delegate.lock();
    }

    public AutoReleaseLock lock(long time, TimeUnit unit) throws TimeoutException, InterruptedException {
        boolean isLocked = delegate.tryLock(time, unit);
        if (isLocked) {
            return this;
        } else {
            throw new TimeoutException("Waiting time of " + time + " " + unit.name() + " is elapsed before the lock was acquired.");
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        delegate.lockInterruptibly();
    }

    @Override
    public Condition newCondition() {
        return delegate.newCondition();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return delegate.tryLock(time, unit);
    }

    @Override
    public boolean tryLock() {
        return delegate.tryLock();
    }

    @Override
    public void unlock() {
        delegate.unlock();
    }
}
