package eu.dirk.haase.jdbc.proxy.generate;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class JavassistProxyClasses {

    private final Map<Class<?>, Class<?>> iface2ClassMap;
    private CtClass callableStatementCt;
    private JavassistProxyClassGenerator callableStatementGen;
    private ClassPool classPool;
    private CtClass connectionCt;
    private JavassistProxyClassGenerator connectionGen;
    private CtClass connectionPoolDataSourceCt;
    private JavassistProxyClassGenerator connectionPoolDataSourceGen;
    private CtClass dataSourceCt;
    private JavassistProxyClassGenerator dataSourceGen;
    private CtClass pooledConnectionCt;
    private JavassistProxyClassGenerator pooledConnectionGen;
    private CtClass preparedStatementCt;
    private JavassistProxyClassGenerator preparedStatementGen;
    private CtClass resultSetCt;
    private JavassistProxyClassGenerator resultSetGen;
    private CtClass statementCt;
    private JavassistProxyClassGenerator statementGen;
    private CtClass xaConnectionCt;
    private JavassistProxyClassGenerator xaConnectionGen;
    private CtClass xaDataSourceCt;
    private JavassistProxyClassGenerator xaDataSourceGen;
    private CtClass xaResourceCt;
    private JavassistProxyClassGenerator xaResourceGen;

    public JavassistProxyClasses(final BiFunction<String, Class<?>, String> classNameFun, final Map<Class<?>, Class<?>> iface2ClassMap) {
        this.iface2ClassMap = new HashMap<>(iface2ClassMap);

        this.resultSetGen = createIfPresent(ResultSet.class, classNameFun, iface2ClassMap);
        this.connectionGen = createIfPresent(Connection.class, classNameFun, iface2ClassMap);
        this.preparedStatementGen = createIfPresent(PreparedStatement.class, classNameFun, iface2ClassMap);
        this.callableStatementGen = createIfPresent(CallableStatement.class, classNameFun, iface2ClassMap);
        this.statementGen = createIfPresent(Statement.class, classNameFun, iface2ClassMap);
        this.dataSourceGen = createIfPresent(DataSource.class, classNameFun, iface2ClassMap);
        this.xaConnectionGen = createIfPresent(XAConnection.class, classNameFun, iface2ClassMap);
        this.xaDataSourceGen = createIfPresent(XADataSource.class, classNameFun, iface2ClassMap);
        this.xaResourceGen = createIfPresent(XAResource.class, classNameFun, iface2ClassMap);
        this.pooledConnectionGen = createIfPresent(PooledConnection.class, classNameFun, iface2ClassMap);
        this.connectionPoolDataSourceGen = createIfPresent(ConnectionPoolDataSource.class, classNameFun, iface2ClassMap);
    }

    private CtClass createCallableStatement(CtClass resultSetCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(ResultSet.class)) {
            child.put("executeQuery", resultSetCt);
            child.put("getResultSet", resultSetCt);
            child.put("getGeneratedKeys", resultSetCt);
        }
        return this.callableStatementGen.generate(classPool, Connection.class, child);
    }

    private ClassPool createClassPool() {
        ClassPool classPool;
        classPool = new ClassPool();
        classPool.importPackage("java.sql");
        classPool.importPackage("javax.sql");
        classPool.importPackage("java.util.function");
        classPool.importPackage("java.util.concurrent.locks");
        classPool.importPackage(ObjectMaker.class.getPackage().getName());
        classPool.appendClassPath(new LoaderClassPath(JavassistProxyClasses.class.getClassLoader()));
        return classPool;
    }

    private CtClass createConnection(CtClass cStatementCt, CtClass pStatementCt, CtClass statementCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(Statement.class)) {
            child.put("createStatement", statementCt);
        }
        if (iface2ClassMap.containsKey(PreparedStatement.class)) {
            child.put("prepareStatement", pStatementCt);
        }
        if (iface2ClassMap.containsKey(CallableStatement.class)) {
            child.put("prepareCall", cStatementCt);
        }
        return this.connectionGen.generate(classPool, DataSource.class, child);
    }

    private CtClass createConnectionPoolDataSource(CtClass pooledConnectionCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(PooledConnection.class)) {
            child.put("getPooledConnection", pooledConnectionCt);
        }
        return this.connectionPoolDataSourceGen.generate(classPool, null, child);
    }

    private CtClass createDataSource(CtClass connectionCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(Connection.class)) {
            child.put("getConnection", connectionCt);
        }
        return this.dataSourceGen.generate(classPool, null, child);
    }

    private JavassistProxyClassGenerator createIfPresent(Class<?> iface, final BiFunction<String, Class<?>, String> classNameFun, final Map<Class<?>, Class<?>> iface2ClassMap) {
        if (iface2ClassMap.containsKey(iface)) {
            return new JavassistProxyClassGenerator(classNameFun, iface, iface2ClassMap.get(iface));
        }
        return null;
    }

    private CtClass createIfPresent(Class<?> iface, Supplier<CtClass> classCt) {
        if (iface2ClassMap.containsKey(iface)) {
            return classCt.get();
        }
        return null;
    }

    private Map<Class<?>, Object> createInterfaceToClassMap(final Function<CtClass, Object> valueFunction) {
        final Map<Class<?>, Object> interfaceToClassMap = new HashMap<>();

        putIfPresent(interfaceToClassMap, ResultSet.class, this.resultSetCt, valueFunction);
        putIfPresent(interfaceToClassMap, CallableStatement.class, this.callableStatementCt, valueFunction);
        putIfPresent(interfaceToClassMap, PreparedStatement.class, this.preparedStatementCt, valueFunction);
        putIfPresent(interfaceToClassMap, Statement.class, this.statementCt, valueFunction);
        putIfPresent(interfaceToClassMap, Connection.class, this.connectionCt, valueFunction);
        putIfPresent(interfaceToClassMap, DataSource.class, this.dataSourceCt, valueFunction);
        putIfPresent(interfaceToClassMap, XAResource.class, this.xaResourceCt, valueFunction);
        putIfPresent(interfaceToClassMap, XAConnection.class, this.xaConnectionCt, valueFunction);
        putIfPresent(interfaceToClassMap, XADataSource.class, this.xaDataSourceCt, valueFunction);
        putIfPresent(interfaceToClassMap, PooledConnection.class, this.pooledConnectionCt, valueFunction);
        putIfPresent(interfaceToClassMap, ConnectionPoolDataSource.class, this.connectionPoolDataSourceCt, valueFunction);

        return interfaceToClassMap;
    }

    private CtClass createPooledConnection(CtClass connectionCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(Connection.class)) {
            child.put("getConnection", connectionCt);
        }
        return this.pooledConnectionGen.generate(classPool, ConnectionPoolDataSource.class, child);
    }

    private CtClass createPreparedStatement(CtClass resultSetCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(ResultSet.class)) {
            child.put("executeQuery", resultSetCt);
            child.put("getResultSet", resultSetCt);
            child.put("getGeneratedKeys", resultSetCt);
        }
        return this.preparedStatementGen.generate(classPool, Connection.class, child);
    }

    private CtClass createResultSet() {
        return this.resultSetGen.generate(classPool, Statement.class, new HashMap<>());
    }

    private CtClass createStatement(CtClass resultSetCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(ResultSet.class)) {
            child.put("executeQuery", resultSetCt);
            child.put("getResultSet", resultSetCt);
            child.put("getGeneratedKeys", resultSetCt);
        }
        return this.statementGen.generate(classPool, Connection.class, child);
    }

    private CtClass createXAConnection(CtClass connectionCt, CtClass xaResourceCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(Connection.class)) {
            child.put("getConnection", connectionCt);
        }
        if (iface2ClassMap.containsKey(XAResource.class)) {
            child.put("getXAResource", xaResourceCt);
        }
        return this.xaConnectionGen.generate(classPool, XADataSource.class, child);
    }

    private CtClass createXADataSource(CtClass xaConnectionCt) {
        final Map<String, CtClass> child = new HashMap<>();
        if (iface2ClassMap.containsKey(XAConnection.class)) {
            child.put("getXAConnection", xaConnectionCt);
        }
        return this.xaDataSourceGen.generate(classPool, null, child);
    }

    private CtClass createXAResource() {
        return this.xaResourceGen.generate(classPool, XAConnection.class, new HashMap<>());
    }

    public Map<Class<?>, Object> generate(final Function<CtClass, Object> valueFunction) {
        this.classPool = createClassPool();

        this.resultSetCt = createIfPresent(ResultSet.class, () -> createResultSet());
        this.callableStatementCt = createIfPresent(CallableStatement.class, () -> createCallableStatement(resultSetCt));
        this.preparedStatementCt = createIfPresent(PreparedStatement.class, () -> createPreparedStatement(resultSetCt));
        this.statementCt = createIfPresent(Statement.class, () -> createStatement(resultSetCt));
        this.connectionCt = createIfPresent(Connection.class, () -> createConnection(callableStatementCt, preparedStatementCt, statementCt));
        this.dataSourceCt = createIfPresent(DataSource.class, () -> createDataSource(connectionCt));
        this.xaResourceCt = createIfPresent(XAResource.class, () -> createXAResource());
        this.xaConnectionCt = createIfPresent(XAConnection.class, () -> createXAConnection(connectionCt, xaResourceCt));
        this.xaDataSourceCt = createIfPresent(XADataSource.class, () -> createXADataSource(xaConnectionCt));
        this.pooledConnectionCt = createIfPresent(PooledConnection.class, () -> createPooledConnection(connectionCt));
        this.connectionPoolDataSourceCt = createIfPresent(ConnectionPoolDataSource.class, () -> createConnectionPoolDataSource(pooledConnectionCt));

        return createInterfaceToClassMap(valueFunction);
    }

    private void putIfPresent(final Map<Class<?>, Object> interfaceToClassMap, Class<?> iface, CtClass classCt, final Function<CtClass, Object> valueFunction) {
        if (iface2ClassMap.containsKey(iface)) {
            interfaceToClassMap.put(iface, valueFunction.apply(classCt));
        }
    }
}
