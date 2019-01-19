package eu.dirk.haase.jdbc.proxy.generate;

import javassist.CannotCompileException;
import javassist.CtClass;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Generator {


    private static final String prefix = "gen.";
    private static final BiFunction<String, Class<?>, String> CLASS_NAME_FUN = (cn, iface) -> cn.replaceAll("(.+)\\.(\\w+)", "$1." + prefix + "$2");
    private final DefaultClassLoaderSupplier defaultClassLoader;

    public Generator() {
        super();
        this.defaultClassLoader = new DefaultClassLoaderSupplier();
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

        final Map<Class<?>, Class<?>> iface2ClassMap = new HashMap<>(iface2CustomClassMap);
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
        return this.defaultClassLoader.getClassLoader();
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.defaultClassLoader.setClassLoader(classLoader);
    }

    private void hierarchyCheck(Class<?> iface, Object implObj) {
        if (!iface.isAssignableFrom((Class<?>) implObj)) {
            throw new IllegalArgumentException(implObj + " is not implementing " + iface);
        }
    }

}
