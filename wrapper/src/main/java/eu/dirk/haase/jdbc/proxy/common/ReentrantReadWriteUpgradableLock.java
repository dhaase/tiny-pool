package eu.dirk.haase.jdbc.proxy.common;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

public interface ReentrantReadWriteUpgradableLock {

    default <T> T read(Supplier<T> block) {
        readLock().lock();
        try {
            return block.get();
        } finally {
            readLock().unlock();
        }
    }

    default void read(Runnable block) {
        readLock().lock();
        try {
            block.run();
        } finally {
            readLock().unlock();
        }
    }

    AutoReleaseLock readLock(long time, TimeUnit unit) throws InterruptedException, TimeoutException;

    Lock readLock();

    default <T> T write(Supplier<T> block) {
        writeLock().lock();
        try {
            return block.get();
        } finally {
            writeLock().unlock();
        }
    }

    default void write(Runnable block) {
        writeLock().lock();
        try {
            block.run();
        } finally {
            writeLock().unlock();
        }
    }

    AutoReleaseLock writeLock(long time, TimeUnit unit) throws InterruptedException, TimeoutException;

    Lock writeLock();
}
