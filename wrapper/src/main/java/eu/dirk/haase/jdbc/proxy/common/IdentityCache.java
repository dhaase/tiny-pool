package eu.dirk.haase.jdbc.proxy.common;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class IdentityCache implements Function<Object, Object> {

    private static final int WAITING_SECONDS = 30;
    private final Map<Object, Object> identityHashMap;
    private final AutoReleaseLock readLock;
    private final AutoReleaseLock writeLock;

    public IdentityCache(final Map<Object, Object> identityHashMap, final ReadWriteLock lock) {
        this.identityHashMap = identityHashMap;
        this.readLock = new AutoReleaseLock(lock.readLock());
        this.writeLock = new AutoReleaseLock(lock.writeLock());
    }

    public IdentityCache() {
        this(new WeakIdentityHashMap<>(), new ReentrantReadWriteLock(true));
    }

    @Override
    public Object apply(Object o) {
        return null;
    }

    private Object computeConcurrentIfAbsent(final Map<Object, Object> map,
                                             final Object key,
                                             final Function<? super Object, ? extends Object> mappingFunction) {
        try {
            try (AutoReleaseLock rlock = this.readLock.lock(WAITING_SECONDS, TimeUnit.SECONDS)) {
                Object currValue;
                if ((currValue = map.get(key)) == null) {
                    Object newValue;
                    if ((newValue = mappingFunction.apply(key)) != null) {
                        try (AutoReleaseLock wlock = this.writeLock.lock(WAITING_SECONDS, TimeUnit.SECONDS)) {
                            map.put(key, newValue);
                            return newValue;
                        }
                    }
                }
                return currValue;
            }
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timeout occurred while acquiring the lock for key " + key.getClass() + ": " + e, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread is interrupted occurred while acquiring the lock for key " + key.getClass() + ": " + e, e);
        }
    }

    public final <T> T get(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

    public final <T> T getConcurrent(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) computeConcurrentIfAbsent(identityHashMap, delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

}
