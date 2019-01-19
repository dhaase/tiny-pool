package eu.dirk.haase.jdbc.proxy.generate;

import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>
 * This {@link java.lang.ClassLoader} is capable of loading classes from multiple parents. This class loader
 * implicitly defines the bootstrap class loader to be its direct parent as it is required for all class loaders.
 * This can be useful when creating a type that inherits a super type and interfaces that are defined by different,
 * non-compatible class loaders.
 * </p>
 * <p>
 * <b>Note</b>: Instances of this class loader can have the same class loader as its parent multiple times,
 * either directly or indirectly by multiple parents sharing a common parent class loader. By definition,
 * this implies that the bootstrap class loader is {@code #(direct parents) + 1} times a parent of this class loader.
 * For the {@link java.lang.ClassLoader#getResources(java.lang.String)} method, this means that this class loader
 * might return the same url multiple times by representing the same class loader multiple times.
 * </p>
 * <p>
 * <b>Important</b>: This class loader does not support the location of packages from its multiple parents. This breaks
 * package equality when loading classes by either loading them directly via this class loader (e.g. by subclassing) or
 * by loading classes with child class loaders of this class loader.
 * </p>
 */
public class MultipleParentClassLoader extends SecureClassLoader {

    /**
     * The parents of this class loader in their application order.
     */
    private final List<ClassLoader> parents;

    public MultipleParentClassLoader() {
        this.parents = new ArrayList<>();
        addClassLoader(Thread.currentThread().getContextClassLoader());
        addClassLoader(MultipleParentClassLoader.class.getClassLoader());
        addClassLoader(ClassLoader.getSystemClassLoader());
    }

    private void addClassLoader(ClassLoader contextClassLoader) {
        try {
            final ClassLoader cl = contextClassLoader;
            if ((cl != null) && (!this.parents.contains(cl))) {
                this.parents.add(cl);
            }
        } catch (Throwable ex) {
            // Cannot access ClassLoader
        }
    }

    /**
     * Creates a new class loader with multiple parents.
     *
     * @param parent  An explicit parent in compliance with the class loader API. This explicit parent should only be set if
     *                the current platform does not allow creating a class loader that extends the bootstrap loader.
     * @param parents The parents of this class loader in their application order. This list must not contain {@code null},
     *                i.e. the bootstrap class loader which is an implicit parent of any class loader.
     */
    public MultipleParentClassLoader(ClassLoader parent, List<ClassLoader> parents) {
        super(parent);
        this.parents = parents;
    }

    /**
     * {@inheritDoc}
     */
    public URL getResource(String name) {
        for (ClassLoader parent : parents) {
            URL url = parent.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return super.getResource(name);
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration<URL> getResources(String name) throws IOException {
        List<Enumeration<URL>> enumerations = new ArrayList<Enumeration<URL>>(parents.size() + 1);
        for (ClassLoader parent : parents) {
            enumerations.add(parent.getResources(name));
        }
        enumerations.add(super.getResources(name));
        return new CompoundEnumeration(enumerations);
    }

    /**
     * {@inheritDoc}
     */
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (ClassLoader parent : parents) {
            try {
                Class<?> type = parent.loadClass(name);
                if (resolve) {
                    resolveClass(type);
                }
                return type;
            } catch (ClassNotFoundException ignored) {
                // try next class loader
            }
        }
        return super.loadClass(name, resolve);
    }

    /**
     * A compound URL enumeration.
     */
    protected static class CompoundEnumeration implements Enumeration<URL> {

        /**
         * Indicates the first index of a list.
         */
        private static final int FIRST = 0;

        /**
         * The remaining lists of enumerations.
         */
        private final List<Enumeration<URL>> enumerations;

        /**
         * The currently represented enumeration or {@code null} if no such enumeration is currently selected.
         */
        private Enumeration<URL> currentEnumeration;

        /**
         * Creates a compound enumeration.
         *
         * @param enumerations The enumerations to represent.
         */
        protected CompoundEnumeration(List<Enumeration<URL>> enumerations) {
            this.enumerations = enumerations;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasMoreElements() {
            if (currentEnumeration != null && currentEnumeration.hasMoreElements()) {
                return true;
            } else if (!enumerations.isEmpty()) {
                currentEnumeration = enumerations.remove(FIRST);
                return hasMoreElements();
            } else {
                return false;
            }
        }

        /**
         * {@inheritDoc}
         */
        public URL nextElement() {
            if (hasMoreElements()) {
                return currentEnumeration.nextElement();
            } else {
                throw new NoSuchElementException();
            }
        }
    }

}