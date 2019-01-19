package eu.dirk.haase.jdbc.proxy.generate;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class DefaultClassLoaderSupplier implements Supplier<ClassLoader> {

    private final AtomicReference<ClassLoader> classLoaderReference;

    public DefaultClassLoaderSupplier() {
        this.classLoaderReference = new AtomicReference<>();
    }

    @Override
    public ClassLoader get() {
        return getClassLoader();
    }

    public ClassLoader getClassLoader() {
        final ClassLoader classLoader = this.classLoaderReference.get();
        return classLoader == null ? new MultipleParentClassLoader() : classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoaderReference.set(classLoader);
    }

}
