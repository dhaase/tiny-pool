package eu.dirk.haase.jdbc.proxy.generate;

import eu.dirk.haase.jdbc.proxy.base.*;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.bytecode.ClassFile;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class JavassistProxyClasses {

    private static final String prefix = "W";
    private static final BiFunction<String, Class<?>, String> CLASS_NAME_FUN = (cn,iface) -> cn.replaceAll("(.+)\\.(\\w+)", "$1." + prefix + "$2");

    private ClassPool classPool;

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

    private final Map<String, Object> interfaceToClassMap;

    public JavassistProxyClasses() {
        this(CLASS_NAME_FUN);
    }

    public JavassistProxyClasses(final BiFunction<String, Class<?>, String> classNameFun) {
        this.interfaceToClassMap = new HashMap<>();

        this.resultSetGen = new JavassistProxyClassGenerator(classNameFun, ResultSet.class, ResultSetProxy.class, false);
        this.connectionGen = new JavassistProxyClassGenerator(classNameFun, Connection.class, ConnectionProxy.class, false);

        this.pStatementGen = new JavassistProxyClassGenerator(classNameFun, PreparedStatement.class, PreparedStatementProxy.class, false);
        this.cStatementGen = new JavassistProxyClassGenerator(classNameFun, CallableStatement.class, CallableStatementProxy.class, false);
        this.statementGen = new JavassistProxyClassGenerator(classNameFun, Statement.class, StatementProxy.class, false);

        this.dataSourceGen = new JavassistProxyClassGenerator(classNameFun, DataSource.class, DataSourceProxy.class, true);
        this.xaconnectionGen = new JavassistProxyClassGenerator(classNameFun, XAConnection.class, XAConnectionProxy.class, true);
        this.xadataSourceGen = new JavassistProxyClassGenerator(classNameFun, XADataSource.class, XADataSourceProxy.class, true);
        this.xaResourceGen = new JavassistProxyClassGenerator(classNameFun, XAResource.class, XAResourceProxy.class, true);
        this.pooledConnectionGen = new JavassistProxyClassGenerator(classNameFun, PooledConnection.class, PooledConnectionProxy.class, true);
        this.connectionPoolDataSourceGen = new JavassistProxyClassGenerator(classNameFun, ConnectionPoolDataSource.class, ConnectionPoolDataSourceProxy.class, true);
    }

    private ClassPool createClassPool() {
        ClassPool classPool;
        classPool = new ClassPool();
        classPool.importPackage("java.sql");
        classPool.importPackage("javax.sql");
        classPool.importPackage("java.util.function");
        classPool.importPackage("java.util.concurrent.locks");
        classPool.importPackage(ObjectMaker.class.getPackage().getName());
        classPool.appendClassPath(new LoaderClassPath(JavassistProxyFactory.class.getClassLoader()));
        return classPool;
    }

    private CtClass createCallableStatement(CtClass resultSetCt) throws Exception {
        final Map<String, CtClass> childs3 = new HashMap<>();
        childs3.put("executeQuery", resultSetCt);
        childs3.put("getResultSet", resultSetCt);
        childs3.put("getGeneratedKeys", resultSetCt);
        return this.statementGen.generate(classPool, Connection.class, childs3);
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

    private CtClass createPreparedStatement(CtClass resultSetCt) throws Exception {
        final Map<String, CtClass> childs4 = new HashMap<>();
        childs4.put("executeQuery", resultSetCt);
        childs4.put("getResultSet", resultSetCt);
        childs4.put("getGeneratedKeys", resultSetCt);
        return this.pStatementGen.generate(classPool, Connection.class, childs4);
    }

    private CtClass createPooledConnection(CtClass connectionCt) throws Exception {
        final Map<String, CtClass> childsc = new HashMap<>();
        childsc.put("getConnection", connectionCt);
        return this.pooledConnectionGen.generate(classPool, ConnectionPoolDataSource.class, childsc);
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

    private CtClass createXADataSource(CtClass xaconnectionCt) throws Exception {
        final Map<String, CtClass> childsb = new HashMap<>();
        childsb.put("getXAConnection", xaconnectionCt);
        return this.xadataSourceGen.generate(classPool, null, childsb);
    }

    private CtClass createXAConnection(CtClass connectionCt, CtClass xaResourceCt) throws Exception {
        final Map<String, CtClass> childsa = new HashMap<>();
        childsa.put("getConnection", connectionCt);
        childsa.put("getXAResource", xaResourceCt);
        return this.xaconnectionGen.generate(classPool, XADataSource.class, childsa);
    }

    private CtClass createXAResource() throws Exception {
        return this.xaResourceGen.generate(classPool, XAConnection.class, new HashMap<>());
    }

    public Map<String, Object> generate() throws Exception {
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

        interfaceToClassMap.put(ResultSet.class.getName(), this.resultSetCt.getName());
        interfaceToClassMap.put(CallableStatement.class.getName(), this.cStatementCt.getName());
        interfaceToClassMap.put(PreparedStatement.class.getName(), this.pStatementCt.getName());
        interfaceToClassMap.put(Statement.class.getName(), this.statementCt.getName());
        interfaceToClassMap.put(Connection.class.getName(), this.connectionCt.getName());
        interfaceToClassMap.put(DataSource.class.getName(), this.dataSourceCt.getName());
        interfaceToClassMap.put(XAResource.class.getName(), this.xaResourceCt.getName());
        interfaceToClassMap.put(XAConnection.class.getName(), this.xaconnectionCt.getName());
        interfaceToClassMap.put(XADataSource.class.getName(), this.xadataSourceCt.getName());
        interfaceToClassMap.put(PooledConnection.class.getName(), this.pooledConnectionCt.getName());
        interfaceToClassMap.put(ConnectionPoolDataSource.class.getName(), this.connectionPoolDataSourceCt.getName());

        return interfaceToClassMap;
    }

    public void writeFile() throws CannotCompileException, IOException {
        writeFile(dataSourceCt);
        writeFile(connectionCt);
        writeFile(pStatementCt);
        writeFile(cStatementCt);
        writeFile(statementCt);
        writeFile(resultSetCt);
        writeFile(xaconnectionCt);
        writeFile(xadataSourceCt);
        writeFile(xaResourceCt);
        writeFile(pooledConnectionCt);
        writeFile(connectionPoolDataSourceCt);
    }

    private void writeFile(final CtClass dataSourceCt) throws CannotCompileException, IOException {
        dataSourceCt.getClassFile().setMajorVersion(ClassFile.JAVA_8);
        dataSourceCt.writeFile("target/classes");
    }

}
