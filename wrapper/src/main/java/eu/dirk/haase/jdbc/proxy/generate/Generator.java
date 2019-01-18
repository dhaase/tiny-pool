package eu.dirk.haase.jdbc.proxy.generate;

import eu.dirk.haase.jdbc.proxy.*;
import javassist.CannotCompileException;
import javassist.CtClass;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Generator {

    private final static ThreadLocal<ClassLoader> classLoaderThreadLocal = ThreadLocal.withInitial(() -> new MultipleParentClassLoader());

    private static final String prefix = "gen.";
    private static final BiFunction<String, Class<?>, String> CLASS_NAME_FUN = (cn, iface) -> cn.replaceAll("(.+)\\.(\\w+)", "$1." + prefix + "$2");
    private ClassLoader classLoader;

    public Generator() {
        super();
    }

    static String computeClassName(BiFunction<String, Class<?>, String> classNameFun, Class<?> primaryIfaceClass, Class<?> superClass) {
        return classNameFun.apply(superClass.getName().replace("Abstract", ""), primaryIfaceClass);
    }

    private static Object toClass(CtClass ctClass) {
        try {
            return ctClass.toClass();
        } catch (CannotCompileException e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<Class<?>, Class<?>> filterExistingClasses(final Map<Class<?>, Class<?>> iface2ClassMap, final BiFunction<String, Class<?>, String> classNameFun) {
        final Map<Class<?>, Class<?>> existingClassesMap = new HashMap<>();

        for (final Map.Entry<Class<?>, Class<?>> entry : iface2ClassMap.entrySet()) {
            final Class<?> primaryIfaceClass = entry.getKey();
            final Class<?> superClass = entry.getValue();
            String newClassName = Generator.computeClassName(classNameFun, primaryIfaceClass, superClass);
            try {
                Class<?> implClass = Class.forName(newClassName, true, getClassLoader());
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

    public Map<Class<?>, Object> generate(final Map<Class<?>, Class<?>> iface2CustomClassMap, final BiFunction<String, Class<?>, String> classNameFun) {

        iface2CustomClassMap.forEach((i, c) -> hierarchyCheck(i, c));

        final Map<Class<?>, Class<?>> iface2ClassMap = newIface2ClassMap();
        iface2ClassMap.putAll(iface2CustomClassMap);
        final Map<Class<?>, Class<?>> existingClassesMap = filterExistingClasses(iface2ClassMap, classNameFun);

        JavassistProxyClasses javassistProxyClasses = new JavassistProxyClasses(classNameFun, iface2ClassMap);
        final Function<CtClass, Object> valueFunction = (c) -> toClass(c);

        final Map<Class<?>, Object> iface2ResultClassMap = javassistProxyClasses.generate(valueFunction);
        iface2ResultClassMap.forEach((i, c) -> hierarchyCheck(i, c));
        iface2ResultClassMap.putAll(existingClassesMap);

        return iface2ResultClassMap;
    }

    public Map<Class<?>, Object> generate(final Map<Class<?>, Class<?>> iface2CustomClassMap) {
        return generate(iface2CustomClassMap, CLASS_NAME_FUN);
    }

    public ClassLoader getClassLoader() {
        return classLoader == null ? classLoaderThreadLocal.get() : classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private void hierarchyCheck(Class<?> iface, Object implObj) {
        if (!iface.isAssignableFrom((Class<?>) implObj)) {
            throw new IllegalArgumentException(implObj + " is not implementing " + iface);
        }
    }

    private Map<Class<?>, Class<?>> newIface2ClassMap() {
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

        return iface2ClassMap;
    }

}