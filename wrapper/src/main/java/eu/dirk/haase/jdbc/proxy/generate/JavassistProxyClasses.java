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

    private final JavassistProxyClassGenerator cStatementGen;
    private final JavassistProxyClassGenerator connectionGen;
    private final JavassistProxyClassGenerator connectionPoolDataSourceGen;
    private final JavassistProxyClassGenerator dataSourceGen;
    private final JavassistProxyClassGenerator pStatementGen;
    private final JavassistProxyClassGenerator pooledConnectionGen;
    private final JavassistProxyClassGenerator resultSetGen;
    private final JavassistProxyClassGenerator statementGen;
    private final JavassistProxyClassGenerator xaResourceGen;
    private final JavassistProxyClassGenerator xaconnectionGen;
    private final JavassistProxyClassGenerator xadataSourceGen;

    private ClassPool classPool;

    private CtClass cStatementCt;
    private CtClass connectionCt;
    private CtClass connectionPoolDataSourceCt;
    private CtClass dataSourceCt;
    private CtClass pStatementCt;
    private CtClass pooledConnectionCt;
    private CtClass resultSetCt;
    private CtClass statementCt;
    private CtClass xaResourceCt;
    private CtClass xaconnectionCt;
    private CtClass xadataSourceCt;

    public JavassistProxyClasses(final BiFunction<String, Class<?>, String> classNameFun, final Map<Class<?>, Class<?>> iface2ClassMap) {
        this.resultSetGen = new JavassistProxyClassGenerator(classNameFun, ResultSet.class, iface2ClassMap.get(ResultSet.class), false);
        this.connectionGen = new JavassistProxyClassGenerator(classNameFun, Connection.class, iface2ClassMap.get(Connection.class), false);

        this.pStatementGen = new JavassistProxyClassGenerator(classNameFun, PreparedStatement.class, iface2ClassMap.get(PreparedStatement.class), false);
        this.cStatementGen = new JavassistProxyClassGenerator(classNameFun, CallableStatement.class, iface2ClassMap.get(CallableStatement.class), false);
        this.statementGen = new JavassistProxyClassGenerator(classNameFun, Statement.class, iface2ClassMap.get(Statement.class), false);

        this.dataSourceGen = new JavassistProxyClassGenerator(classNameFun, DataSource.class, iface2ClassMap.get(DataSource.class), true);
        this.xaconnectionGen = new JavassistProxyClassGenerator(classNameFun, XAConnection.class, iface2ClassMap.get(XAConnection.class), true);
        this.xadataSourceGen = new JavassistProxyClassGenerator(classNameFun, XADataSource.class, iface2ClassMap.get(XADataSource.class), true);
        this.xaResourceGen = new JavassistProxyClassGenerator(classNameFun, XAResource.class, iface2ClassMap.get(XAResource.class), true);
        this.pooledConnectionGen = new JavassistProxyClassGenerator(classNameFun, PooledConnection.class, iface2ClassMap.get(PooledConnection.class), true);
        this.connectionPoolDataSourceGen = new JavassistProxyClassGenerator(classNameFun, ConnectionPoolDataSource.class, iface2ClassMap.get(ConnectionPoolDataSource.class), true);
    }

    private CtClass createCallableStatement(CtClass resultSetCt) throws Exception {
        final Map<String, CtClass> childs3 = new HashMap<>();
        childs3.put("executeQuery", resultSetCt);
        childs3.put("getResultSet", resultSetCt);
        childs3.put("getGeneratedKeys", resultSetCt);
        return this.statementGen.generate(classPool, Connection.class, childs3);
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
        childs2.put("createStatement", statementCt);
        childs2.put("prepareStatement", pStatementCt);
        childs2.put("prepareCall", cStatementCt);
        return this.connectionGen.generate(classPool, DataSource.class, childs2);
    }

    private CtClass createConnectionPoolDataSource(CtClass pooledConnectionCt) throws Exception {
        final Map<String, CtClass> childsd = new HashMap<>();
        childsd.put("getPooledConnection", pooledConnectionCt);
        return this.connectionPoolDataSourceGen.generate(classPool, null, childsd);
    }

    private CtClass createDataSource(CtClass connectionCt) throws Exception {
        final Map<String, CtClass> childs1 = new HashMap<>();
        childs1.put("getConnection", connectionCt);
        return this.dataSourceGen.generate(classPool, null, childs1);
    }

    private Map<String, Object> createInterfaceToClassMap(final Function<CtClass, Object> valueFunction) throws Exception {
        final Map<String, Object> interfaceToClassMap = new HashMap<>();

        interfaceToClassMap.put(ResultSet.class.getName(), valueFunction.apply(this.resultSetCt));
        interfaceToClassMap.put(CallableStatement.class.getName(), valueFunction.apply(this.cStatementCt));
        interfaceToClassMap.put(PreparedStatement.class.getName(), valueFunction.apply(this.pStatementCt));
        interfaceToClassMap.put(Statement.class.getName(), valueFunction.apply(this.statementCt));
        interfaceToClassMap.put(Connection.class.getName(), valueFunction.apply(this.connectionCt));
        interfaceToClassMap.put(DataSource.class.getName(), valueFunction.apply(this.dataSourceCt));
        interfaceToClassMap.put(XAResource.class.getName(), valueFunction.apply(this.xaResourceCt));
        interfaceToClassMap.put(XAConnection.class.getName(), valueFunction.apply(this.xaconnectionCt));
        interfaceToClassMap.put(XADataSource.class.getName(), valueFunction.apply(this.xadataSourceCt));
        interfaceToClassMap.put(PooledConnection.class.getName(), valueFunction.apply(this.pooledConnectionCt));
        interfaceToClassMap.put(ConnectionPoolDataSource.class.getName(), valueFunction.apply(this.connectionPoolDataSourceCt));

        return interfaceToClassMap;
    }

    private CtClass createPooledConnection(CtClass connectionCt) throws Exception {
        final Map<String, CtClass> childsc = new HashMap<>();
        childsc.put("getConnection", connectionCt);
        return this.pooledConnectionGen.generate(classPool, ConnectionPoolDataSource.class, childsc);
    }

    private CtClass createPreparedStatement(CtClass resultSetCt) throws Exception {
        final Map<String, CtClass> childs4 = new HashMap<>();
        childs4.put("executeQuery", resultSetCt);
        childs4.put("getResultSet", resultSetCt);
        childs4.put("getGeneratedKeys", resultSetCt);
        return this.pStatementGen.generate(classPool, Connection.class, childs4);
    }

    private CtClass createResultSet() throws Exception {
        return this.resultSetGen.generate(classPool, Statement.class, new HashMap<>());
    }

    private CtClass createStatement(CtClass resultSetCt) throws Exception {
        final Map<String, CtClass> childs5 = new HashMap<>();
        childs5.put("executeQuery", resultSetCt);
        childs5.put("getResultSet", resultSetCt);
        childs5.put("getGeneratedKeys", resultSetCt);
        return this.cStatementGen.generate(classPool, Connection.class, childs5);
    }

    private CtClass createXAConnection(CtClass connectionCt, CtClass xaResourceCt) throws Exception {
        final Map<String, CtClass> childsa = new HashMap<>();
        childsa.put("getConnection", connectionCt);
        childsa.put("getXAResource", xaResourceCt);
        return this.xaconnectionGen.generate(classPool, XADataSource.class, childsa);
    }

    private CtClass createXADataSource(CtClass xaconnectionCt) throws Exception {
        final Map<String, CtClass> childsb = new HashMap<>();
        childsb.put("getXAConnection", xaconnectionCt);
        return this.xadataSourceGen.generate(classPool, null, childsb);
    }

    private CtClass createXAResource() throws Exception {
        return this.xaResourceGen.generate(classPool, XAConnection.class, new HashMap<>());
    }

    public Map<String, Object> generate(final Function<CtClass, Object> valueFunction) throws Exception {
        this.classPool = createClassPool();

        this.resultSetCt = createResultSet();
        this.cStatementCt = createStatement(resultSetCt);
        this.pStatementCt = createPreparedStatement(resultSetCt);
        this.statementCt = createCallableStatement(resultSetCt);
        this.connectionCt = createConnection(cStatementCt, pStatementCt, statementCt);
        this.dataSourceCt = createDataSource(connectionCt);
        this.xaResourceCt = createXAResource();
        this.xaconnectionCt = createXAConnection(connectionCt, xaResourceCt);
        this.xadataSourceCt = createXADataSource(xaconnectionCt);
        this.pooledConnectionCt = createPooledConnection(connectionCt);
        this.connectionPoolDataSourceCt = createConnectionPoolDataSource(pooledConnectionCt);

        return createInterfaceToClassMap(valueFunction);
    }

}
