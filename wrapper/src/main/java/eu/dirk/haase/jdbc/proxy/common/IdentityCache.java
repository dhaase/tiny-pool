package eu.dirk.haase.jdbc.proxy.common;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class IdentityCache implements Function<Object, Object> {

    private static final int WAITING_SECONDS = 30;
    private final Map<Object, Object> identityHashMap;
    private final ReentrantReadWriteUpgradableLock readWriteLock;

    public IdentityCache(final Map<Object, Object> identityHashMap, final ReentrantReadWriteUpgradableLock readWriteLock) {
        this.identityHashMap = identityHashMap;
        this.readWriteLock = readWriteLock;
    }

    public IdentityCache() {
        this(new WeakIdentityHashMap<>(), new AutoReleaseReadWriteLock());
    }

    @Override
    public Object apply(Object o) {
        return null;
    }

    private Object computeConcurrentIfAbsent(final Map<Object, Object> map,
                                             final Object key,
                                             final Function<? super Object, ? extends Object> mappingFunction) {
        try {
            try (AutoReleaseLock readLock = this.readWriteLock.readLock(WAITING_SECONDS, TimeUnit.SECONDS)) {
                Object currValue;
                if ((currValue = map.get(key)) == null) {
                    Object newValue;
                    if ((newValue = mappingFunction.apply(key)) != null) {
                        try (AutoReleaseLock writeLock = this.readWriteLock.writeLock(WAITING_SECONDS, TimeUnit.SECONDS)) {
                            map.put(key, newValue);
                            return newValue;
                        }
                    }
                }
                return currValue;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread is interrupted occurred while acquiring the lock for key " + key.getClass() + ": " + e, e);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Timeout occurred while acquiring the lock for key " + key.getClass() + ": " + e, e);
        }
    }

    public final <T> T get(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

    public final <T> T getConcurrent(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) computeConcurrentIfAbsent(identityHashMap, delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

}
