package eu.dirk.haase.jdbc.proxy.common;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class IdentityCache implements Function<Object, Object> {

    private static final int WAITING_SECONDS = 30;
    private final Map<Object, Object> identityHashMap;

    public IdentityCache(final Map<Object, Object> identityHashMap) {
        this.identityHashMap = identityHashMap;
    }

    public IdentityCache() {
        this(new WeakIdentityHashMap<>());
    }

    @Override
    public Object apply(Object o) {
        return null;
    }

    private Object computeConcurrentIfAbsent(final Map<Object, Object> map,
                                             final Object key,
                                             final Function<? super Object, ? extends Object> mappingFunction) {
        Object currValue;
        if ((currValue = map.get(key)) == null) {
            Object newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                map.put(key, newValue);
                return newValue;
            }
        }
        return currValue;
    }

    public final <T> T get(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) identityHashMap.computeIfAbsent(delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

    public final <T> T getConcurrent(T delegate, BiFunction<T, Object[], T> objectMaker, final Object... argumentArray) {
        return (T) computeConcurrentIfAbsent(identityHashMap, delegate, (k) -> objectMaker.apply(delegate, argumentArray));
    }

}
