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
    private CtClass xaResourceCt;
    private JavassistProxyClassGenerator xaResourceGen;
    private CtClass xaconnectionCt;
    private JavassistProxyClassGenerator xaconnectionGen;
    private CtClass xadataSourceCt;
    private JavassistProxyClassGenerator xadataSourceGen;

    public JavassistProxyClasses(final BiFunction<String, Class<?>, String> classNameFun, final Map<Class<?>, Class<?>> iface2ClassMap) {
        this.iface2ClassMap = new HashMap<>(iface2ClassMap);

        if (iface2ClassMap.containsKey(ResultSet.class)) {
            this.resultSetGen = new JavassistProxyClassGenerator(classNameFun, ResultSet.class, iface2ClassMap.get(ResultSet.class));
        }
        if (iface2ClassMap.containsKey(Connection.class)) {
            this.connectionGen = new JavassistProxyClassGenerator(classNameFun, Connection.class, iface2ClassMap.get(Connection.class));
        }
        if (iface2ClassMap.containsKey(PreparedStatement.class)) {
            this.preparedStatementGen = new JavassistProxyClassGenerator(classNameFun, PreparedStatement.class, iface2ClassMap.get(PreparedStatement.class));
        }
        if (iface2ClassMap.containsKey(CallableStatement.class)) {
            this.callableStatementGen = new JavassistProxyClassGenerator(classNameFun, CallableStatement.class, iface2ClassMap.get(CallableStatement.class));
        }
        if (iface2ClassMap.containsKey(Statement.class)) {
            this.statementGen = new JavassistProxyClassGenerator(classNameFun, Statement.class, iface2ClassMap.get(Statement.class));
        }
        if (iface2ClassMap.containsKey(DataSource.class)) {
            this.dataSourceGen = new JavassistProxyClassGenerator(classNameFun, DataSource.class, iface2ClassMap.get(DataSource.class));
        }
        if (iface2ClassMap.containsKey(XAConnection.class)) {
            this.xaconnectionGen = new JavassistProxyClassGenerator(classNameFun, XAConnection.class, iface2ClassMap.get(XAConnection.class));
        }
        if (iface2ClassMap.containsKey(XADataSource.class)) {
            this.xadataSourceGen = new JavassistProxyClassGenerator(classNameFun, XADataSource.class, iface2ClassMap.get(XADataSource.class));
        }
        if (iface2ClassMap.containsKey(XAResource.class)) {
            this.xaResourceGen = new JavassistProxyClassGenerator(classNameFun, XAResource.class, iface2ClassMap.get(XAResource.class));
        }
        if (iface2ClassMap.containsKey(PooledConnection.class)) {
            this.pooledConnectionGen = new JavassistProxyClassGenerator(classNameFun, PooledConnection.class, iface2ClassMap.get(PooledConnection.class));
        }
        if (iface2ClassMap.containsKey(ConnectionPoolDataSource.class)) {
            this.connectionPoolDataSourceGen = new JavassistProxyClassGenerator(classNameFun, ConnectionPoolDataSource.class, iface2ClassMap.get(ConnectionPoolDataSource.class));
        }
    }

    private CtClass createCallableStatement(CtClass resultSetCt) throws Exception {
        final Map<String, CtClass> childs3 = new HashMap<>();
        if (iface2ClassMap.containsKey(ResultSet.class)) {
            childs3.put("executeQuery", resultSetCt);
            childs3.put("getResultSet", resultSetCt);
            childs3.put("getGeneratedKeys", resultSetCt);
        }
        return this.callableStatementGen.generate(classPool, Connection.class, childs3);
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

    private CtClass createConnection(CtClass cStatementCt, CtClass pStatementCt, CtClass statementCt) throws Exception {
        final Map<String, CtClass> childs2 = new HashMap<>();
        if (iface2ClassMap.containsKey(Statement.class)) {
            childs2.put("createStatement", statementCt);
        }
        if (iface2ClassMap.containsKey(PreparedStatement.class)) {
            childs2.put("prepareStatement", pStatementCt);
        }
        if (iface2ClassMap.containsKey(CallableStatement.class)) {
            childs2.put("prepareCall", cStatementCt);
        }
        return this.connectionGen.generate(classPool, DataSource.class, childs2);
    }

    private CtClass createConnectionPoolDataSource(CtClass pooledConnectionCt) throws Exception {
        final Map<String, CtClass> childsd = new HashMap<>();
        if (iface2ClassMap.containsKey(PooledConnection.class)) {
            childsd.put("getPooledConnection", pooledConnectionCt);
        }
        return this.connectionPoolDataSourceGen.generate(classPool, null, childsd);
    }

    private CtClass createDataSource(CtClass connectionCt) throws Exception {
        final Map<String, CtClass> childs1 = new HashMap<>();
        if (iface2ClassMap.containsKey(Connection.class)) {
            childs1.put("getConnection", connectionCt);
        }
        return this.dataSourceGen.generate(classPool, null, childs1);
    }

    private Map<String, Object> createInterfaceToClassMap(final Function<CtClass, Object> valueFunction) throws Exception {
        final Map<String, Object> interfaceToClassMap = new HashMap<>();

        if (iface2ClassMap.containsKey(ResultSet.class)) {
            interfaceToClassMap.put(ResultSet.class.getName(), valueFunction.apply(this.resultSetCt));
        }
        if (iface2ClassMap.containsKey(CallableStatement.class)) {
            interfaceToClassMap.put(CallableStatement.class.getName(), valueFunction.apply(this.callableStatementCt));
        }
        if (iface2ClassMap.containsKey(PreparedStatement.class)) {
            interfaceToClassMap.put(PreparedStatement.class.getName(), valueFunction.apply(this.preparedStatementCt));
        }
        if (iface2ClassMap.containsKey(Statement.class)) {
            interfaceToClassMap.put(Statement.class.getName(), valueFunction.apply(this.statementCt));
        }
        if (iface2ClassMap.containsKey(Connection.class)) {
            interfaceToClassMap.put(Connection.class.getName(), valueFunction.apply(this.connectionCt));
        }
        if (iface2ClassMap.containsKey(DataSource.class)) {
            interfaceToClassMap.put(DataSource.class.getName(), valueFunction.apply(this.dataSourceCt));
        }
        if (iface2ClassMap.containsKey(XAResource.class)) {
            interfaceToClassMap.put(XAResource.class.getName(), valueFunction.apply(this.xaResourceCt));
        }
        if (iface2ClassMap.containsKey(XAConnection.class)) {
            interfaceToClassMap.put(XAConnection.class.getName(), valueFunction.apply(this.xaconnectionCt));
        }
        if (iface2ClassMap.containsKey(XADataSource.class)) {
            interfaceToClassMap.put(XADataSource.class.getName(), valueFunction.apply(this.xadataSourceCt));
        }
        if (iface2ClassMap.containsKey(PooledConnection.class)) {
            interfaceToClassMap.put(PooledConnection.class.getName(), valueFunction.apply(this.pooledConnectionCt));
        }
        if (iface2ClassMap.containsKey(ConnectionPoolDataSource.class)) {
            interfaceToClassMap.put(ConnectionPoolDataSource.class.getName(), valueFunction.apply(this.connectionPoolDataSourceCt));
        }

        return interfaceToClassMap;
    }

    private CtClass createPooledConnection(CtClass connectionCt) throws Exception {
        final Map<String, CtClass> childsc = new HashMap<>();
        if (iface2ClassMap.containsKey(Connection.class)) {
            childsc.put("getConnection", connectionCt);
        }
        return this.pooledConnectionGen.generate(classPool, ConnectionPoolDataSource.class, childsc);
    }

    private CtClass createPreparedStatement(CtClass resultSetCt) throws Exception {
        final Map<String, CtClass> childs4 = new HashMap<>();
        if (iface2ClassMap.containsKey(ResultSet.class)) {
            childs4.put("executeQuery", resultSetCt);
            childs4.put("getResultSet", resultSetCt);
            childs4.put("getGeneratedKeys", resultSetCt);
        }
        return this.preparedStatementGen.generate(classPool, Connection.class, childs4);
    }

    private CtClass createResultSet() throws Exception {
        return this.resultSetGen.generate(classPool, Statement.class, new HashMap<>());
    }

    private CtClass createStatement(CtClass resultSetCt) throws Exception {
        final Map<String, CtClass> childs5 = new HashMap<>();
        if (iface2ClassMap.containsKey(ResultSet.class)) {
            childs5.put("executeQuery", resultSetCt);
            childs5.put("getResultSet", resultSetCt);
            childs5.put("getGeneratedKeys", resultSetCt);
        }
        return this.statementGen.generate(classPool, Connection.class, childs5);
    }

    private CtClass createXAConnection(CtClass connectionCt, CtClass xaResourceCt) throws Exception {
        final Map<String, CtClass> childsa = new HashMap<>();
        if (iface2ClassMap.containsKey(Connection.class)) {
            childsa.put("getConnection", connectionCt);
        }
        if (iface2ClassMap.containsKey(XAResource.class)) {
            childsa.put("getXAResource", xaResourceCt);
        }
        return this.xaconnectionGen.generate(classPool, XADataSource.class, childsa);
    }

    private CtClass createXADataSource(CtClass xaconnectionCt) throws Exception {
        final Map<String, CtClass> childsb = new HashMap<>();
        if (iface2ClassMap.containsKey(XAConnection.class)) {
            childsb.put("getXAConnection", xaconnectionCt);
        }
        return this.xadataSourceGen.generate(classPool, null, childsb);
    }

    private CtClass createXAResource() throws Exception {
        return this.xaResourceGen.generate(classPool, XAConnection.class, new HashMap<>());
    }

    public Map<String, Object> generate(final Function<CtClass, Object> valueFunction) throws Exception {
        this.classPool = createClassPool();

        if (iface2ClassMap.containsKey(ResultSet.class)) {
            this.resultSetCt = createResultSet();
        }
        if (iface2ClassMap.containsKey(CallableStatement.class)) {
            this.callableStatementCt = createCallableStatement(resultSetCt);
        }
        if (iface2ClassMap.containsKey(PreparedStatement.class)) {
            this.preparedStatementCt = createPreparedStatement(resultSetCt);
        }
        if (iface2ClassMap.containsKey(Statement.class)) {
            this.statementCt = createStatement(resultSetCt);
        }
        if (iface2ClassMap.containsKey(Connection.class)) {
            this.connectionCt = createConnection(callableStatementCt, preparedStatementCt, statementCt);
        }
        if (iface2ClassMap.containsKey(DataSource.class)) {
            this.dataSourceCt = createDataSource(connectionCt);
        }
        if (iface2ClassMap.containsKey(XAResource.class)) {
            this.xaResourceCt = createXAResource();
        }
        if (iface2ClassMap.containsKey(XAConnection.class)) {
            this.xaconnectionCt = createXAConnection(connectionCt, xaResourceCt);
        }
        if (iface2ClassMap.containsKey(XADataSource.class)) {
            this.xadataSourceCt = createXADataSource(xaconnectionCt);
        }
        if (iface2ClassMap.containsKey(PooledConnection.class)) {
            this.pooledConnectionCt = createPooledConnection(connectionCt);
        }
        if (iface2ClassMap.containsKey(ConnectionPoolDataSource.class)) {
            this.connectionPoolDataSourceCt = createConnectionPoolDataSource(pooledConnectionCt);
        }

        return createInterfaceToClassMap(valueFunction);
    }

}
