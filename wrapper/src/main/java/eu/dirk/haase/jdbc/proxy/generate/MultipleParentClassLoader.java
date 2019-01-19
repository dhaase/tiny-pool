package eu.dirk.haase.jdbc.proxy.generate;

import java.io.IOException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.*;

/**
 * Dieser {@link java.lang.ClassLoader} kann Klassen aus mehreren &uuml;bergeordneten
 * ClassLoader laden.
 * <p>
 * Dieser ClassLoader definiert implizit den Bootstrap-ClassLoader als direkten
 * &uuml;bergeordneten ClassLoader, da er f&uuml;r alle ClassLoader erforderlich ist.
 * <p>
 * Dieser ClassLoader l&ouml;st das Problem das bei verschiedenen ClassLoader aus
 * mehreren Quellen oft nicht sicher ist, welcher ClassLoader die Klasse oder Resource
 * erfolgreich laden kann.
 * Daher wird intern eine Liste von potentiellen ClassLoader gef&uuml;hrt, die
 * die beim Aufruf abgefragt werden k&ouml;nnen.
 * <p>
 * M&ouml;gliche Quellen sind:
 * <ul>
 * <li>{@link Thread#getContextClassLoader()} des aktuellen Threads</li>
 * <li>der ClassLoader dieser Klasse</li>
 * <li>{@link ClassLoader#getSystemClassLoader()}</li>
 * </ul>
 */
public final class MultipleParentClassLoader extends SecureClassLoader {

    private final List<ClassLoader> parentList;


    /**
     * Erzeugt einen {@link java.lang.ClassLoader} mit mehreren &uuml;bergeordneten
     * ClassLoader.
     * <p>
     * Es werden weitere ClassLoader, sofern sie sich unterscheiden und
     * tats&auml;chlich existieren, hinzugef&uuml;gt:
     * <ul>
     * <li>{@link Thread#getContextClassLoader()} des aktuellen Threads</li>
     * <li>der ClassLoader dieser Klasse</li>
     * <li>{@link ClassLoader#getSystemClassLoader()}</li>
     * </ul>
     */
    public MultipleParentClassLoader() {
        this(null);
    }


    /**
     * Erzeugt einen {@link java.lang.ClassLoader} mit mehreren &uuml;bergeordneten
     * ClassLoader.
     * <p>
     * Zus&auml;tzlich zu dem explizit angegebenen &uuml;bergeordneten ClassLoader werden
     * weitere ClassLoader, sofern sie sich unterscheiden und tats&auml;chlich existieren,
     * hinzugef&uuml;gt:
     * <ul>
     * <li>{@link Thread#getContextClassLoader()} des aktuellen Threads</li>
     * <li>der ClassLoader dieser Klasse</li>
     * <li>{@link ClassLoader#getSystemClassLoader()}</li>
     * </ul>
     *
     * @param parent ein expliziter &uuml;bergeordneter ClassLoader der zuerst verwendet wird.
     */
    public MultipleParentClassLoader(final ClassLoader parent) {
        this.parentList = new ArrayList<>();
        addClassLoader(parent);
        addClassLoader(Thread.currentThread().getContextClassLoader());
        addClassLoader(MultipleParentClassLoader.class.getClassLoader());
        addClassLoader(ClassLoader.getSystemClassLoader());
    }

    /**
     * Erzeugt einen {@link java.lang.ClassLoader} mit mehreren &uuml;bergeordneten
     * ClassLoader.
     * <p>
     * Zus&auml;tzlich zu dem explizit angegebenen &uuml;bergeordneten ClassLoader werden
     * weitere ClassLoader, sofern sie sich unterscheiden und tats&auml;chlich existieren,
     * hinzugef&uuml;gt:
     * <ul>
     * <li>{@link Thread#getContextClassLoader()} des aktuellen Threads</li>
     * <li>der ClassLoader dieser Klasse</li>
     * <li>{@link ClassLoader#getSystemClassLoader()}</li>
     * </ul>
     *
     * @param parent  ein expliziter &uuml;bergeordneter ClassLoader der zuerst verwendet wird.
     * @param parents eine Liste mit weiteren &uuml;bergeordneten ClassLoader.
     */
    public MultipleParentClassLoader(ClassLoader parent, List<ClassLoader> parents) {
        this.parentList = (parents == null ? new ArrayList<>() : new ArrayList<>(parents));
        addParentOnTop(parent);
        addClassLoader(Thread.currentThread().getContextClassLoader());
        addClassLoader(MultipleParentClassLoader.class.getClassLoader());
        addClassLoader(ClassLoader.getSystemClassLoader());
    }

    private void addParentOnTop(ClassLoader parent) {
        if ((parent != null) && !this.parentList.contains(parent)) {
            Collections.reverse(this.parentList);
            this.parentList.add(parent);
            Collections.reverse(this.parentList);
        }
    }

    private void addClassLoader(ClassLoader cl) {
        try {
            if ((cl != null) && (getParent() != cl) && !this.parentList.contains(cl)) {
                this.parentList.add(cl);
            }
        } catch (Throwable ex) {
            // Kein Zugriff auf den ClassLoader
        }
    }

    @Override
    public URL getResource(String name) {
        for (ClassLoader parent : parentList) {
            URL url = parent.getResource(name);
            if (url != null) {
                return url;
            }
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<Enumeration<URL>> enumerations = new ArrayList<Enumeration<URL>>(parentList.size() + 1);
        for (ClassLoader parent : parentList) {
            enumerations.add(parent.getResources(name));
        }
        enumerations.add(super.getResources(name));
        return new CompoundEnumeration(enumerations);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (ClassLoader parent : parentList) {
            try {
                Class<?> type = parent.loadClass(name);
                if (resolve) {
                    resolveClass(type);
                }
                return type;
            } catch (ClassNotFoundException ignored) {
                // versuche den naechsten
            }
        }
        // gebe an den Bootstrap-ClassLoader weiter
        return super.loadClass(name, resolve);
    }

    static class CompoundEnumeration implements Enumeration<URL> {

        static final int FIRST = 0;

        final List<Enumeration<URL>> enumerations;

        Enumeration<URL> currentEnumeration;

        CompoundEnumeration(List<Enumeration<URL>> enumerations) {
            this.enumerations = enumerations;
        }

        @Override
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

        @Override
        public URL nextElement() {
            if (hasMoreElements()) {
                return currentEnumeration.nextElement();
            } else {
                throw new NoSuchElementException();
            }
        }
    }

}