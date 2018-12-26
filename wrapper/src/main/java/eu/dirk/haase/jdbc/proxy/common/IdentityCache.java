package eu.dirk.haase.jdbc.proxy.common;

import java.util.Map;
import java.util.function.Function;

public final class IdentityCache {

    private final Map<Object, Object> identityHashMap;

    public IdentityCache() {
        this.identityHashMap = new WeakIdentityHashMap<>();
    }

    public final <T> T get(T delegate, Function<T, T> objectMaker) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate));
    }

}
