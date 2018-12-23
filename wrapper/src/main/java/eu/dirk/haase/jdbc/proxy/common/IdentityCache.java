package eu.dirk.haase.jdbc.proxy.common;

import java.util.IdentityHashMap;
import java.util.function.Supplier;

public final class IdentityCache {

    private final static IdentityCache SINGLETON = new IdentityCache();

    private final IdentityHashMap identityHashMap;

    private IdentityCache() {
        this.identityHashMap = new IdentityHashMap();
    }

    public static IdentityCache getSingleton() {
        return SINGLETON;
    }

    public final <T> T get(T delegate, Supplier<T> make) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> make.get());
    }

}
