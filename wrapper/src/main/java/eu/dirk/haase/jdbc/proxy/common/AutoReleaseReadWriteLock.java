package eu.dirk.haase.jdbc.proxy.common;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class AutoReleaseReadWriteLock implements ReentrantReadWriteUpgradableLock {

    private final AutoReleaseLock readLock;
    private final AutoReleaseLock writeLock;

    public AutoReleaseReadWriteLock() {
        final ReadWriteLock readWriteLock = new ReadWriteLock();
        this.readLock = new ReadLock(readWriteLock);
        this.writeLock = new WriteLock(readWriteLock);
    }

    public AutoReleaseLock readLock(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (readLock.tryLock(time, unit)) {
            return readLock;
        } else {
            throw new TimeoutException("Timeout occurred while acquiring the lock; Was waiting " + time + " " + unit.name());
        }
    }

    @Override
    public Lock readLock() {
        return readLock;
    }

    public AutoReleaseLock writeLock(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (writeLock.tryLock(time, unit)) {
            return writeLock;
        } else {
            throw new TimeoutException("Timeout occurred while acquiring the lock; Was waiting " + time + " " + unit.name());
        }
    }

    @Override
    public Lock writeLock() {
        return writeLock;
    }

    static class ReadLock implements AutoReleaseLock {

        final ReadWriteLock readWriteLock;

        ReadLock(ReadWriteLock readWriteLock) {
            this.readWriteLock = readWriteLock;
        }

        @Override
        public void close() {
            unlock();
        }

        @Override
        public void lock() {
            try {
                readWriteLock.lockRead();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ie.toString(), ie);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            readWriteLock.lockRead();
        }

        @Override
        public Condition newCondition() {
            return null;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            try {
                return readWriteLock.lockRead(time, unit);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            }
        }

        @Override
        public boolean tryLock() {
            try {
                readWriteLock.lockRead(1, TimeUnit.MILLISECONDS);
                return true;
            } catch (InterruptedException ie) {
                return false;
            }
        }

        @Override
        public void unlock() {
            readWriteLock.unlockRead();
        }
    }

    static class ReadWriteLock {

        private final Object lockMonitor;
        private final Map<Thread, Integer> readingThreads;
        private int writeAccesses = 0;
        private int writeRequests = 0;
        private Thread writingThread = null;

        ReadWriteLock() {
            this.lockMonitor = new Object();
            this.readingThreads = new IdentityHashMap<>();
        }

        private boolean canGrantReadAccess(Thread callingThread) {
            if (isWriter(callingThread)) return true;
            if (hasWriter()) return false;
            if (isReader(callingThread)) return true;
            if (hasWriteRequests()) return false;
            return true;
        }

        private boolean canGrantWriteAccess(Thread callingThread) {
            if (isOnlyReader(callingThread)) return true;
            if (hasReaders()) return false;
            if (writingThread == null) return true;
            if (!isWriter(callingThread)) return false;
            return true;
        }

        private int getReadAccessCount(Thread callingThread) {
            final Integer accessCount = readingThreads.get(callingThread);
            if (accessCount == null) return 0;
            return accessCount.intValue();
        }

        private boolean hasReaders() {
            return readingThreads.size() > 0;
        }

        private boolean hasWriteRequests() {
            return this.writeRequests > 0;
        }

        private boolean hasWriter() {
            return writingThread != null;
        }

        private boolean isOnlyReader(Thread callingThread) {
            return readingThreads.size() == 1 && readingThreads.get(callingThread) != null;
        }

        private boolean isReader(Thread callingThread) {
            return readingThreads.get(callingThread) != null;
        }

        private boolean isWriter(Thread callingThread) {
            return writingThread == callingThread;
        }

        public void lockRead() throws InterruptedException {
            lockRead(0, TimeUnit.MILLISECONDS);
        }

        public boolean lockRead(long time, TimeUnit unit) throws InterruptedException {
            synchronized (lockMonitor) {
                final Thread callingThread = Thread.currentThread();
                final long timeoutMillis = unit.toMillis(time);

                final boolean isReadAccessGranted = canGrantReadAccess(callingThread);
                if (!isReadAccessGranted && (timeoutMillis == 1)) {
                    return false;
                }
                while (!canGrantReadAccess(callingThread)) {
                    lockMonitor.wait(timeoutMillis);
                }

                readingThreads.put(callingThread, (getReadAccessCount(callingThread) + 1));

                return true;
            }
        }

        public void lockWrite() throws InterruptedException {
            lockWrite(0, TimeUnit.MILLISECONDS);
        }

        public boolean lockWrite(long time, TimeUnit unit) throws InterruptedException {
            synchronized (lockMonitor) {
                writeRequests++;
                final Thread callingThread = Thread.currentThread();
                final long timeoutMillis = unit.toMillis(time);

                final boolean isWriteAccessGranted = canGrantWriteAccess(callingThread);
                if (!isWriteAccessGranted && (timeoutMillis == 1)) {
                    return false;
                }
                while (!canGrantWriteAccess(callingThread)) {
                    lockMonitor.wait(timeoutMillis);
                }

                writeRequests--;
                writeAccesses++;
                writingThread = callingThread;

                return true;
            }
        }

        public void unlockRead() {
            synchronized (lockMonitor) {
                final Thread callingThread = Thread.currentThread();
                if (!isReader(callingThread)) {
                    throw new IllegalMonitorStateException("Calling Thread does not hold a read lock on this ReadWriteLock");
                }
                final int accessCount = getReadAccessCount(callingThread);
                if (accessCount == 1) {
                    readingThreads.remove(callingThread);
                } else {
                    readingThreads.put(callingThread, (accessCount - 1));
                }
                lockMonitor.notifyAll();
            }
        }

        public void unlockWrite() {
            synchronized (lockMonitor) {
                if (!isWriter(Thread.currentThread())) {
                    throw new IllegalMonitorStateException("Calling Thread does not hold the write lock on this ReadWriteLock");
                }
                writeAccesses--;
                if (writeAccesses == 0) {
                    writingThread = null;
                }
                lockMonitor.notifyAll();
            }
        }

    }

    class WriteLock implements AutoReleaseLock {

        final ReadWriteLock readWriteLock;

        WriteLock(ReadWriteLock readWriteLock) {
            this.readWriteLock = readWriteLock;
        }

        @Override
        public void close() {
            unlock();
        }

        @Override
        public void lock() {
            try {
                readWriteLock.lockWrite();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ie.toString(), ie);
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            readWriteLock.lockWrite();
        }

        @Override
        public Condition newCondition() {
            return null;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            try {
                return readWriteLock.lockWrite(time, unit);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw ie;
            }
        }

        @Override
        public boolean tryLock() {
            try {
                return readWriteLock.lockWrite(1, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ie) {
                return false;
            }
        }

        @Override
        public void unlock() {
            readWriteLock.unlockWrite();
        }
    }
}
