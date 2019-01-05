package eu.dirk.haase.jdbc.lock;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.lang.reflect.Constructor;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class ReadWriteLockTest {


    @Test
    public void test() throws InterruptedException {
        final StampedLock stampedLock = new StampedLock();
        System.out.println("#1");
        //long stamp1 = stampedLock.readLockInterruptibly();
        System.out.println("#2");
        long stamp2 = stampedLock.readLockInterruptibly();
        System.out.println("#3");
        long stamp3 = stampedLock.tryConvertToWriteLock(stamp2);
        System.out.println("#4 " + stamp3);
    }

    private void test_for_concurrent_modification(final boolean isConcurrentModificationExpected, final ExecutorService executorService, Constructor<Callable<Boolean>> constructor, final CountDownLatch countDownLatch) throws Exception {
        final long startNanos = System.nanoTime();
        // Given
        final SharedObject sharedObject = new SharedObject();
        final Callable<Boolean> callable0 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable1 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable2 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable3 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable4 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable5 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable6 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable7 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable8 = constructor.newInstance(sharedObject, countDownLatch);
        final Callable<Boolean> callable9 = constructor.newInstance(sharedObject, countDownLatch);
        // When
        Future<Boolean> future0 = executorService.submit(callable0);
        Future<Boolean> future1 = executorService.submit(callable1);
        Future<Boolean> future2 = executorService.submit(callable2);
        Future<Boolean> future3 = executorService.submit(callable3);
        Future<Boolean> future4 = executorService.submit(callable4);
        Future<Boolean> future5 = executorService.submit(callable5);
        Future<Boolean> future6 = executorService.submit(callable6);
        Future<Boolean> future7 = executorService.submit(callable7);
        Future<Boolean> future8 = executorService.submit(callable8);
        Future<Boolean> future9 = executorService.submit(callable9);
        //
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
        boolean isConcurrentModified = future0.get();
        isConcurrentModified = isConcurrentModified || future1.get();
        isConcurrentModified = isConcurrentModified || future2.get();
        isConcurrentModified = isConcurrentModified || future3.get();
        isConcurrentModified = isConcurrentModified || future4.get();
        isConcurrentModified = isConcurrentModified || future5.get();
        isConcurrentModified = isConcurrentModified || future6.get();
        isConcurrentModified = isConcurrentModified || future7.get();
        isConcurrentModified = isConcurrentModified || future8.get();
        isConcurrentModified = isConcurrentModified || future9.get();
        // Then Calculate speedup
        final long endNanos = System.nanoTime();
        final long durationOverall = TimeUnit.NANOSECONDS.toMillis(endNanos - startNanos);
        System.out.println(constructor.getDeclaringClass().getSimpleName() + " -> Duration: " + durationOverall + " ms; Speedup: " + AbstractSharedObjectCallable.speedup(durationOverall, 10) + " (1.5 theoretical maximum)");
        // Then Check results
        assertThat(isConcurrentModified).isEqualTo(isConcurrentModificationExpected);
    }

    @Test
    @Ignore("Kein automatischer Test da nur einmaliger konzeptioneller Test")
    public void test_for_no_concurrent_modification_lock() throws Exception {
        final int nThreads = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        final ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        final Constructor<Callable<Boolean>> constructor = (Constructor<Callable<Boolean>>) SharedObjectLockCallable.class.getDeclaredConstructors()[0];
        test_for_concurrent_modification(false, executorService, constructor, countDownLatch);
    }

    @Test
    @Ignore("Kein automatischer Test da nur einmaliger konzeptioneller Test")
    public void test_for_no_concurrent_modification_optimistic_stamped_lock() throws Exception {
        final int nThreads = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        final ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        final Constructor<Callable<Boolean>> constructor = (Constructor<Callable<Boolean>>) SharedObjectOptimisticStampedLockCallable.class.getDeclaredConstructors()[0];
        test_for_concurrent_modification(false, executorService, constructor, countDownLatch);
    }

    @Test
    @Ignore("Kein automatischer Test da nur einmaliger konzeptioneller Test")
    public void test_for_no_concurrent_modification_read_write_lock() throws Exception {
        final int nThreads = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        final ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        final Constructor<Callable<Boolean>> constructor = (Constructor<Callable<Boolean>>) SharedObjectReadWriteLockCallable.class.getDeclaredConstructors()[0];
        test_for_concurrent_modification(false, executorService, constructor, countDownLatch);
    }

    @Test
    @Ignore("Kein automatischer Test da nur einmaliger konzeptioneller Test")
    public void test_for_no_concurrent_modification_synchronized() throws Exception {
        final int nThreads = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        final ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        final Constructor<Callable<Boolean>> constructor = (Constructor<Callable<Boolean>>) SharedObjectSynchronizedCallable.class.getDeclaredConstructors()[0];
        test_for_concurrent_modification(false, executorService, constructor, countDownLatch);
    }

    @Test
    @Ignore("Kein automatischer Test da nur einmaliger konzeptioneller Test")
    public void test_serial_execution() throws Exception {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Constructor<Callable<Boolean>> constructor = (Constructor<Callable<Boolean>>) SharedObjectCallable.class.getDeclaredConstructors()[0];
        test_for_concurrent_modification(false, executorService, constructor, null);
    }

    @Test
    @Ignore("Kein automatischer Test da nur einmaliger konzeptioneller Test")
    public void test_with_concurrent_modification() throws Exception {
        final int nThreads = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        final ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        final Constructor<Callable<Boolean>> constructor = (Constructor<Callable<Boolean>>) SharedObjectCallable.class.getDeclaredConstructors()[0];
        test_for_concurrent_modification(true, executorService, constructor, countDownLatch);
    }

    static abstract class AbstractSharedObjectCallable implements Callable<Boolean> {
        final CountDownLatch countDownLatch;
        final SharedObject sharedObject;

        AbstractSharedObjectCallable(final SharedObject sharedObject, final CountDownLatch countDownLatch) {
            this.sharedObject = sharedObject;
            this.countDownLatch = countDownLatch;
        }

        public static double speedup(long durationOverall, long times) {
            // in der Methode doWork() wird durch das SharedObject
            // fuenf mal Thread.sleep() ausgefuehrt:
            final long waitingMillis = SharedObject.getWaitingMillis() * 5;
            // Insgesamt wird die Methode doWork() 'times' Mal aufgerufen
            final double sequentialDuration = waitingMillis * times;
            return (sequentialDuration / durationOverall);
        }

        final void await() throws InterruptedException {
            if (countDownLatch != null) {
                countDownLatch.countDown();
                countDownLatch.await();
            }
        }

        Boolean doWork() throws InterruptedException {
            final int expectedCounter1 = sharedObject.getCount() + 1;
            final int expectedCounter2 = sharedObject.preProcess() + 1;
            final int currentCounter = sharedObject.count();
            final int expectedCounter3 = sharedObject.getCount();
            final boolean isConcurrentModified1 = sharedObject.postProcess();
            final boolean isConcurrentModified2 = currentCounter != expectedCounter1;
            final boolean isConcurrentModified3 = currentCounter != expectedCounter2;
            final boolean isConcurrentModified4 = currentCounter != expectedCounter3;
            return (isConcurrentModified1 || isConcurrentModified2 || isConcurrentModified3 || isConcurrentModified4);
        }
    }

    static class SharedObjectCallable extends AbstractSharedObjectCallable implements Callable<Boolean> {


        SharedObjectCallable(final SharedObject sharedObject, final CountDownLatch countDownLatch) {
            super(sharedObject, countDownLatch);
        }

        @Override
        public Boolean call() throws Exception {
            await();
            return doWork();
        }
    }

    static class SharedObjectLockCallable extends AbstractSharedObjectCallable implements Callable<Boolean> {

        final static Lock lock = new ReentrantLock(true);

        SharedObjectLockCallable(final SharedObject sharedObject, final CountDownLatch countDownLatch) {
            super(sharedObject, countDownLatch);
        }

        @Override
        public Boolean call() throws Exception {
            await();
            lock.lock();
            try {
                return doWork();
            } finally {
                lock.unlock();
            }
        }
    }

    static class SharedObjectOptimisticStampedLockCallable extends AbstractSharedObjectCallable implements Callable<Boolean> {

        static final int RETRIES = 5;
        final static StampedLock stampedLock = new StampedLock();

        SharedObjectOptimisticStampedLockCallable(SharedObject sharedObject, CountDownLatch countDownLatch) {
            super(sharedObject, countDownLatch);
        }

        @Override
        public Boolean call() throws Exception {
            await();
            return doWorkOptimisticReadWrite();
        }


        Boolean doWorkOptimisticReadWrite() throws InterruptedException {
            int expectedCounter1;
            int expectedCounter2;
            int currentCounter;
            boolean isConcurrentModified1;
            long stamp = stampedLock.readLockInterruptibly();
            try {
                sharedObject.getCount();
                int retry = 0;
                while (true) {
                    final long writeStamp = stampedLock.tryConvertToWriteLock(stamp);
                    if (writeStamp != 0) {
                        stamp = writeStamp;
                        expectedCounter1 = sharedObject.preProcess() + 1;
                        currentCounter = sharedObject.count();
                        retry = 0;
                        while (true) {
                            final long readStamp = stampedLock.tryConvertToReadLock(stamp);
                            if (readStamp != 0) {
                                stamp = readStamp;
                                expectedCounter2 = sharedObject.getCount();
                                isConcurrentModified1 = sharedObject.postProcess();
                                break;
                            } else if (retry++ >= RETRIES) {
                                // Fallback
                                stampedLock.unlockWrite(stamp);
                                stamp = stampedLock.readLockInterruptibly();
                            }
                        }
                        break;
                    } else if (retry++ >= RETRIES) {
                        // Fallback
                        stampedLock.unlockRead(stamp);
                        stamp = stampedLock.writeLockInterruptibly();
                    }
                }
            } finally {
                stampedLock.unlock(stamp);
            }
            final boolean isConcurrentModified2 = currentCounter != expectedCounter1;
            final boolean isConcurrentModified3 = currentCounter != expectedCounter2;
            return (isConcurrentModified1 || isConcurrentModified2 || isConcurrentModified3);
        }
    }

    static class SharedObjectReadWriteLockCallable extends AbstractSharedObjectCallable implements Callable<Boolean> {

        final static ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

        SharedObjectReadWriteLockCallable(final SharedObject sharedObject, final CountDownLatch countDownLatch) {
            super(sharedObject, countDownLatch);
        }

        @Override
        public Boolean call() throws Exception {
            await();
            return doWorkReadWrite();
        }


        Boolean doWorkReadWrite() throws InterruptedException {
            int expectedCounter1;
            int expectedCounter2;
            int currentCounter;
            boolean isConcurrentModified1;
            try {
                readWriteLock.readLock().lockInterruptibly();
                sharedObject.getCount();
            } finally {
                // Hochstufen (upgrade) des Locks ist nicht erlaubt,
                // daher muss hier der Lock freigegeben werden:
                readWriteLock.readLock().unlock();
            }
            // Es entsteht hier eine Luecke in der weder
            // ein Read-Lock noch ein Write-Lock gesetzt ist.
            // Mit ReentrantReadWriteLock ist es nicht moeglich
            // vom Read-Lock zu einem Write-Lock zu wechseln
            // ohne das es zu dieser Luecke kommt.
            boolean isReadLockHeld = false;
            try {
                readWriteLock.writeLock().lockInterruptibly();
                expectedCounter1 = sharedObject.preProcess() + 1;
                currentCounter = sharedObject.count();
                // Runterstufen (downgrade) des Locks dagegen ist erlaubt.
                // Wenn das Setzen des Read-Locks fehlschlaegt, zum Beispiel
                // durch ein Thread-Interrupt (siehe naechstes Statement)
                // dann koennen wir im nachhinein an Hand des Lock-Objects
                // nicht feststellen ob der Read-Lock tatsaechlich gesetzt
                // wurde oder nicht.
                readWriteLock.readLock().lockInterruptibly();
                // Wenn wir hier angekommen sind hat das Setzen des Read-Locks
                // funktioniert.
                // Daher hier die Status-Variable:
                isReadLockHeld = true;
                // Write-Lock wird freigegeben, Read-Lock ist
                // aber immer noch gesetzt:
                readWriteLock.writeLock().unlock();
                expectedCounter2 = sharedObject.getCount();
                isConcurrentModified1 = sharedObject.postProcess();
            } finally {
                if (readWriteLock.writeLock().isHeldByCurrentThread()) {
                    readWriteLock.writeLock().unlock();
                }
                if (isReadLockHeld) {
                    readWriteLock.readLock().unlock();
                }
            }
            final boolean isConcurrentModified2 = currentCounter != expectedCounter1;
            final boolean isConcurrentModified3 = currentCounter != expectedCounter2;
            return (isConcurrentModified1 || isConcurrentModified2 || isConcurrentModified3);
        }
    }

    static class SharedObjectSynchronizedCallable extends AbstractSharedObjectCallable implements Callable<Boolean> {

        SharedObjectSynchronizedCallable(final SharedObject sharedObject, final CountDownLatch countDownLatch) {
            super(sharedObject, countDownLatch);
        }

        @Override
        public Boolean call() throws Exception {
            await();
            synchronized (sharedObject) {
                return doWork();
            }
        }
    }

}
