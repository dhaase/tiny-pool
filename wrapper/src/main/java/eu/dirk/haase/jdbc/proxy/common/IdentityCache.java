package eu.dirk.haase.jdbc.proxy.common;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class IdentityCache implements Function<Object, Object> {

    private final Map<Object, Object> identityHashMap;

    public IdentityCache() {
        this.identityHashMap = new WeakIdentityHashMap<>();
    }

    @Override
    public Object apply(Object o) {
        return null;
    }

    public final <T> T get(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

}
