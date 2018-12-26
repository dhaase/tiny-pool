package eu.dirk.haase.jdbc.proxy.generate;

import eu.dirk.haase.jdbc.proxy.base.*;
import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.bytecode.ClassFile;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class JavassistProxyFactory {

    private static final String prefix = "W";
    private static final BiFunction<String, Class<?>, String> CLASS_NAME_FUN = (cn, iface) -> cn.replaceAll("(.+)\\.(\\w+)", "$1." + prefix + "$2");

    private JavassistProxyClasses javassistProxyClasses;

    private JavassistProxyFactory() {
    }


    public static void main(final String... args) throws Exception {
        final Map<Class<?>, Class<?>> iface2ClassMap = new HashMap<>();

        iface2ClassMap.put(ResultSet.class, ResultSetProxy.class);
        iface2ClassMap.put(CallableStatement.class, CallableStatementProxy.class);
        iface2ClassMap.put(PreparedStatement.class, PreparedStatementProxy.class);
        iface2ClassMap.put(Statement.class, StatementProxy.class);
        iface2ClassMap.put(Connection.class, ConnectionProxy.class);
        iface2ClassMap.put(DataSource.class, DataSourceProxy.class);
        iface2ClassMap.put(XAResource.class, XAResourceProxy.class);
        iface2ClassMap.put(XAConnection.class, XAConnectionProxy.class);
        iface2ClassMap.put(XADataSource.class, XADataSourceProxy.class);
        iface2ClassMap.put(PooledConnection.class, PooledConnectionProxy.class);
        iface2ClassMap.put(ConnectionPoolDataSource.class, ConnectionPoolDataSourceProxy.class);

        final JavassistProxyFactory f = new JavassistProxyFactory();
        f.javassistProxyClasses = new JavassistProxyClasses(CLASS_NAME_FUN, iface2ClassMap);

        final Function<CtClass, Object> valueFunction = (c) -> c; //(c)->toClass(c); // (c)->c.getName();
        final Map<String, Object> interfaceToClassMap = f.javassistProxyClasses.generate(valueFunction);

        for (final Object ctClassObj : interfaceToClassMap.values()) {
            writeFile((CtClass) ctClassObj);
        }
    }

    private static Class<?> toClass(CtClass ctClass) {
        try {
            return ctClass.toClass();
        } catch (CannotCompileException e) {
            throw new IllegalStateException(e);
        }
    }


    private static void writeFile(final CtClass ctClass) throws CannotCompileException, IOException {
        ctClass.getClassFile().setMajorVersion(ClassFile.JAVA_8);
        ctClass.writeFile("target/test-classes");
    }

}
