package eu.dirk.haase.jdbc.proxy.base;

import eu.dirk.haase.jdbc.proxy.common.IdentityCache;

import java.util.function.Function;

public abstract class FactoryJdbcProxy<T> extends JdbcProxy<T> {
    private final IdentityCache identityCache;

    protected FactoryJdbcProxy(T delegate) {
        super(delegate);
        this.identityCache = IdentityCache.getSingleton();
    }

    protected final <T> T wrap(T delegate, Function<T, T> objectMaker) {
        return this.identityCache.get(delegate, objectMaker);
    }

}
