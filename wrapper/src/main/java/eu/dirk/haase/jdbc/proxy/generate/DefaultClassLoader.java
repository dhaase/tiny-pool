package eu.dirk.haase.jdbc.proxy.generate;

import java.util.concurrent.atomic.AtomicReference;

public final class DefaultClassLoader {

    private final static ThreadLocal<ClassLoader> classLoaderThreadLocal = ThreadLocal.withInitial(() -> new MultipleParentClassLoader());
    private final AtomicReference<ClassLoader> classLoader;

    public DefaultClassLoader() {
        this.classLoader = new AtomicReference<>();
    }

    public ClassLoader getClassLoader() {
        final ClassLoader classLoader = this.classLoader.get();
        return classLoader == null ? classLoaderThreadLocal.get() : classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader.set(classLoader);
    }

}
