package eu.dirk.haase.jdbc.proxy.generate;

import eu.dirk.haase.jdbc.proxy.*;
import javassist.CannotCompileException;
import javassist.CtClass;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Generator {

    private static final Generator SINGLETON = new Generator();
    private static final String prefix = "gen.";
    private static final BiFunction<String, Class<?>, String> CLASS_NAME_FUN = (cn, iface) -> cn.replaceAll("(.+)\\.(\\w+)", "$1." + prefix + "$2");
    private static final long serialVersionUID = 0L;

    static {
        // Standard Wrapper-Klassen werden hier einmalig generiert und geladen.
        // Spaeter muessen diese dann nicht mehr generiert werden. So wird
        // verhindert das es zu Konflikten bei nebenlaeufigen Zugriffen
        // kommen kann.
        final Map<Class<?>, Class<?>> iface2ClassMap = new HashMap<>();

        iface2ClassMap.put(ResultSet.class, AbstractResultSetProxy.class);
        iface2ClassMap.put(CallableStatement.class, AbstractCallableStatementProxy.class);
        iface2ClassMap.put(PreparedStatement.class, AbstractPreparedStatementProxy.class);
        iface2ClassMap.put(Statement.class, AbstractStatementProxy.class);
        iface2ClassMap.put(Connection.class, AbstractConnectionProxy.class);
        iface2ClassMap.put(DataSource.class, AbstractDataSourceProxy.class);
        iface2ClassMap.put(XAResource.class, AbstractXAResourceProxy.class);
        iface2ClassMap.put(XAConnection.class, AbstractXAConnectionProxy.class);
        iface2ClassMap.put(XADataSource.class, AbstractXADataSourceProxy.class);
        iface2ClassMap.put(PooledConnection.class, AbstractPooledConnectionProxy.class);
        iface2ClassMap.put(ConnectionPoolDataSource.class, AbstractConnectionPoolDataSourceProxy.class);

        final Generator generator = new Generator();
        generator.generate(iface2ClassMap);
    }

    private final ConcurrentHashMap<String, Object> parallelLockMap;

    private Generator() {
        super();
        this.parallelLockMap = new ConcurrentHashMap<>();
    }

    static String computeClassName(final BiFunction<String, Class<?>, String> classNameFun, final Class<?> primaryIfaceClass, final Class<?> superClass) {
        return classNameFun.apply(superClass.getName().replace("Abstract", ""), primaryIfaceClass);
    }

    public static Generator getSingleton() {
        return SINGLETON;
    }

    /**
     * Erzeugt aus einer Javassist-Klasse eine normale Klasse.
     *
     * @param ctClass          die Javassist-Klasse aus der die normale Klasse
     *                         erzeugt werden soll.
     * @param classLoader      der ClassLoader mit der die normale Klasse erzeugt
     *                         werden soll.
     * @param protectionDomain die ProtectionDomain die die die normale Klasse
     *                         erhalten soll.
     * @return die geladene normale Klasse.
     */
    private static Object toClass(final CtClass ctClass, final ClassLoader classLoader, final ProtectionDomain protectionDomain) {
        try {
            return ctClass.toClass(classLoader, protectionDomain);
        } catch (CannotCompileException ex) {
            throw new IllegalStateException(ex.toString(), ex);
        }
    }

    private CodeSource createCodeSource(final CodeSource codeSource, final Class<?> candidateCustomClass) {
        try {
            final Package customPackage = candidateCustomClass.getPackage();
            final URL url = new URL("file://generated/jdbc/wrapper/" + customPackage.getName() + "/");
            final Certificate[] certificate = codeSource.getCertificates();
            final CodeSigner[] codeSigner = codeSource.getCodeSigners();
            if ((certificate != null) && (certificate.length > 0)) {
                return new CodeSource(url, certificate);
            } else {
                return new CodeSource(url, codeSigner);
            }
        } catch (Exception ignore) {
            // Sollte nicht auftreten, falls doch dann
            // gibt es eben keine CodeSource
        }
        return null;
    }

    private void ensureTopLevelInterface(final Map<Class<?>, Class<?>> iface2CustomClassMap) {
        boolean hasTopLevel = iface2CustomClassMap.containsKey(DataSource.class);
        hasTopLevel = hasTopLevel || iface2CustomClassMap.containsKey(XADataSource.class);
        hasTopLevel = hasTopLevel || iface2CustomClassMap.containsKey(ConnectionPoolDataSource.class);
        if (!hasTopLevel) {
            throw new IllegalArgumentException("Argument map must contain at least one top level interface: DataSource, XADataSource or ConnectionPoolDataSource.");
        }
    }

    /**
     * Extrahiert einen Klassen-Kandidaten aus der angegebenen Map, auf dessen Basis
     * die Sperre, der ClassLoader und die {@link ProtectionDomain} ermittelt wird.
     * <p>
     * Damit aus der angegebenen Map jeweils stets der gleiche Klassen-Kandidat ermittelt
     * wird, wird aus einer sortierten Map stets die erste Klasse extrahiert.
     *
     * @param iface2CustomClassMap die Map aus der einen Klassen-Kandidat ermittelt wird.
     * @return der ermittelte Klassen-Kandidat.
     */
    private Class<?> extractCandidateCustomClass(final Map<Class<?>, Class<?>> iface2CustomClassMap) {
        final SortedMap<String, Class<?>> name2ClassMap = new TreeMap<>();
        iface2CustomClassMap.forEach((i, c) -> name2ClassMap.put(c.getName(), c));
        return name2ClassMap.get(name2ClassMap.firstKey());
    }

    /**
     * Ermittelt die bereits existierenden Klassen.
     * <p>
     * Bereits existierende Klassen d&uuml;rfen kein zweites Mal generiert werden,
     * daher m&uuml;ssen diese ausgefiltert werden.
     * <p>
     * In der Regel wird dieser Generator beim ersten Aufruf alle Klassen generieren
     * und bei jedem weiteren Aufruf die bereits generierten und geladenen Klassen
     * zur&uuml;ckliefern ohne sie ein weiteres Mal zu generieren.
     *
     * @param iface2ClassMap eine Map mit abstrakten Klassen von denen die JDBC-Wrapper
     *                       Klassen abgeleitet werden sollen.
     * @param classNameFun   Funktion um die neuen vollqualifizierten Klassennamen zu erzeugen.
     * @param classLoader    der ClassLoader mit dem die Klassen probeweise geladen werden sollen.
     * @return eine Map mit bereits existierenden Klassen.
     */
    private Map<Class<?>, Class<?>> filterExistingClasses(final Map<Class<?>, Class<?>> iface2ClassMap, final BiFunction<String, Class<?>, String> classNameFun, final ClassLoader classLoader) {
        final Map<Class<?>, Class<?>> existingClassesMap = new HashMap<>();

        for (final Map.Entry<Class<?>, Class<?>> entry : iface2ClassMap.entrySet()) {
            final Class<?> primaryIfaceClass = entry.getKey();
            final Class<?> superClass = entry.getValue();
            final String newClassName = Generator.computeClassName(classNameFun, primaryIfaceClass, superClass);
            try {
                final Class<?> implClass = Class.forName(newClassName, true, classLoader);
                existingClassesMap.put(primaryIfaceClass, implClass);
            } catch (ClassNotFoundException | NoClassDefFoundError cnfe) {
                // ignore
            } catch (Exception ex) {
                throw new IllegalStateException(ex.toString(), ex);
            }
        }

        for (final Class<?> primaryIfaceClass : existingClassesMap.keySet()) {
            iface2ClassMap.remove(primaryIfaceClass);
        }

        return existingClassesMap;
    }

    /**
     * Generiert JDBC-Wrapper Klassen die von den angegebenen abstrakten Klassen abgeleitet werden.
     * <p>
     * <b>Hinweis:</b> Diese Methode kann nebenl&auml;ufig ausgef&uuml;hrt werden.
     *
     * @param iface2CustomClassMap eine Map mit abstrakten Klassen von denen die JDBC-Wrapper
     *                             Klassen abgeleitet werden sollen.
     * @param classNameFun         Funktion um die neuen vollqualifizierten Klassennamen zu erzeugen.
     * @return eine Map mit generierten konkreten JDBC-Wrapper Klassen.
     */
    public Map<Class<?>, Object> generate(final Map<Class<?>, Class<?>> iface2CustomClassMap, final BiFunction<String, Class<?>, String> classNameFun) {

        ensureTopLevelInterface(iface2CustomClassMap);

        iface2CustomClassMap.forEach((i, c) -> typeCheck(i, c));
        final Map<Class<?>, Class<?>> iface2ClassMap = new HashMap<>(iface2CustomClassMap);

        final Class<?> candidateCustomClass = extractCandidateCustomClass(iface2CustomClassMap);
        final ClassLoader classLoader = getClassLoader(candidateCustomClass);
        final ClassLoader multipleParentClassLoader = new MultipleParentClassLoader(classLoader);
        final ProtectionDomain protectionDomain = getProtectionDomain(candidateCustomClass);

        synchronized (getClassGeneratingLock(candidateCustomClass.getName())) {
            final Map<Class<?>, Class<?>> existingClassesMap = filterExistingClasses(iface2ClassMap, classNameFun, multipleParentClassLoader);

            final Function<CtClass, Object> resultFunction = (c) -> toClass(c, multipleParentClassLoader, protectionDomain);

            final JavassistProxyClasses javassistProxyClasses = new JavassistProxyClasses(classNameFun, iface2ClassMap);
            final Map<Class<?>, Object> iface2ResultClassMap = javassistProxyClasses.generate(resultFunction);

            iface2ResultClassMap.forEach((i, c) -> typeCheck(i, c));
            iface2ResultClassMap.putAll(existingClassesMap);

            return iface2ResultClassMap;
        }
    }

    /**
     * Generiert JDBC-Wrapper Klassen die von den angegebenen abstrakten Klassen abgeleitet werden.
     * <p>
     * Die generierten Klassen werden in ein Subpackage '{@code gen}' unter dem Package
     * der Super-Klasse von der jeweilig generierten Klasse gesetzt.
     * <p>
     * <b>Hinweis:</b> Diese Methode kann nebenl&auml;ufig ausgef&uuml;hrt werden.
     *
     * @param iface2CustomClassMap eine Map mit abstrakten Klassen von denen die JDBC-Wrapper
     *                             Klassen abgeleitet werden sollen.
     * @return eine Map mit generierten konkreten JDBC-Wrapper Klassen.
     */
    public Map<Class<?>, Object> generate(final Map<Class<?>, Class<?>> iface2CustomClassMap) {
        return generate(iface2CustomClassMap, CLASS_NAME_FUN);
    }

    private Object getClassGeneratingLock(final String className) {
        final Object newLock = new Object();
        Object lock = parallelLockMap.putIfAbsent(className, newLock);
        if (lock == null) {
            lock = newLock;
        }
        return lock;
    }

    private ClassLoader getClassLoader(final Class<?> candidateCustomClass) {
        try {
            return candidateCustomClass.getClassLoader();
        } catch (SecurityException ignore) {
            // was koennen wir tun ?
        }
        return null;
    }

    private ProtectionDomain getProtectionDomain(final Class<?> candidateCustomClass) {
        try {
            final ProtectionDomain protectionDomain = candidateCustomClass.getProtectionDomain();
            final CodeSource codeSource = protectionDomain.getCodeSource();
            final PermissionCollection permissionCollection = protectionDomain.getPermissions();
            return new ProtectionDomain(createCodeSource(codeSource, candidateCustomClass), permissionCollection);
        } catch (SecurityException ignore) {
            // was koennen wir tun ?
        }
        return null;
    }

    private void typeCheck(final Class<?> iface, final Object implObj) {
        if (!iface.isAssignableFrom((Class<?>) implObj)) {
            throw new IllegalArgumentException(implObj + " is not implementing " + iface);
        }
    }

}
