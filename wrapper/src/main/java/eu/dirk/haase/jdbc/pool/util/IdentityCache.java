package eu.dirk.haase.jdbc.pool.util;

import java.util.IdentityHashMap;
import java.util.function.Supplier;

public final class IdentityCache {

    private final static IdentityCache SINGLETON = new IdentityCache();

    private final IdentityHashMap identityHashMap;

    public static IdentityCache getSingleton() {
        return SINGLETON;
    }

    private IdentityCache() {
        this.identityHashMap = new IdentityHashMap();
    }


    protected final <T> T get(T delegate, Supplier<T> make) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> make.get());
    }

}
