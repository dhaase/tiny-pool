package eu.dirk.haase.jdbc.proxy.common;

import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class IdentityCache implements Function<Object, Object> {

    private static final int RETRIES = 5;
    private static final long INVALID_STAMP = 0;

    private final StampedLock stampedLock;
    private final Map<Object, Object> identityHashMap;

    public IdentityCache(final Map<Object, Object> identityHashMap, final StampedLock stampedLock) {
        this.identityHashMap = identityHashMap;
        this.stampedLock = stampedLock;
    }

    public IdentityCache() {
        this(new WeakIdentityHashMap<>(), null);
    }

    @Override
    public Object apply(Object o) {
        return null;
    }

    private Object computeConcurrentIfAbsent(final Map<Object, Object> map,
                                             final Object key,
                                             final Function<? super Object, ? extends Object> mappingFunction) {
        long stamp = INVALID_STAMP;
        try {
            stamp = stampedLock.readLockInterruptibly();
            Object currValue;
            if ((currValue = map.get(key)) == null) {
                Object newValue;
                if ((newValue = mappingFunction.apply(key)) != null) {
                    int retry = 0;
                    while (true) {
                        long writeStamp = stampedLock.tryConvertToWriteLock(stamp);
                        if (writeStamp != 0) {
                            stamp = writeStamp;
                            map.put(key, newValue);
                            return newValue;
                        } else if (retry++ >= RETRIES) {
                            // Fallback
                            stampedLock.unlockWrite(stamp);
                            stamp = stampedLock.readLockInterruptibly();
                        }
                    }
                }
            }
            return currValue;
        } catch (InterruptedException ie) {
            throw new IllegalStateException(ie);
        } finally {
            if (stamp != INVALID_STAMP) {
                stampedLock.unlock(stamp);
            }
        }
    }

    public final <T> T get(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

    public final <T> T getConcurrent(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) computeConcurrentIfAbsent(identityHashMap, delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

}
