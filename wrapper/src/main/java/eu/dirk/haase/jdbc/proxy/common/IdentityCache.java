package eu.dirk.haase.jdbc.proxy.common;

import java.util.IdentityHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public final class IdentityCache {

    private final static IdentityCache SINGLETON = new IdentityCache();

    private final IdentityHashMap<Object,Object> identityHashMap;

    private IdentityCache() {
        this.identityHashMap = new IdentityHashMap<>();
    }

    public static IdentityCache getSingleton() {
        return SINGLETON;
    }

    public final <T> T get(T delegate, Function<Object, T> objectMaker) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate));
    }


}
