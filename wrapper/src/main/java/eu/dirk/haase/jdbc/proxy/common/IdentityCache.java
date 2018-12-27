package eu.dirk.haase.jdbc.proxy.common;

import eu.dirk.haase.jdbc.proxy.base.CloseState;

import java.sql.SQLException;
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

    public final <T> T get(T delegate, Function<T, T> objectMaker) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate));
    }

}
