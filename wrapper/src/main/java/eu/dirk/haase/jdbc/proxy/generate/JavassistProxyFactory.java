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

public final class JavassistProxyFactory {

    private final ClassPool classPool = new ClassPool();

    private final JavassistProxyClassGenerator proxyGenerator;

    private JavassistProxyFactory() {
        this.proxyGenerator = new JavassistProxyClassGenerator(classPool);
    }


    public static void main(final String... args) throws Exception {
        final JavassistProxyFactory f = new JavassistProxyFactory();

        f.classPool.importPackage("java.sql");
        f.classPool.importPackage("javax.sql");
        f.classPool.importPackage("java.util.function");
        f.classPool.importPackage("eu.dirk.haase.jdbc.proxy.generate");
        f.classPool.appendClassPath(new LoaderClassPath(JavassistProxyFactory.class.getClassLoader()));

        final CtClass resultSetCt = f.proxyGenerator.generate(ResultSet.class, ResultSetProxy.class, Statement.class, new HashMap<>(), false);

        final Map<String, CtClass> childs5 = new HashMap<>();
        childs5.put("executeQuery", resultSetCt);
        childs5.put("getResultSet", resultSetCt);
        childs5.put("getGeneratedKeys", resultSetCt);
        final CtClass cStatementCt = f.proxyGenerator.generate(CallableStatement.class, CallableStatementProxy.class, Connection.class, childs5, false);

        final Map<String, CtClass> childs4 = new HashMap<>();
        childs4.put("executeQuery", resultSetCt);
        childs4.put("getResultSet", resultSetCt);
        childs4.put("getGeneratedKeys", resultSetCt);
        final CtClass pStatementCt = f.proxyGenerator.generate(PreparedStatement.class, PreparedStatementProxy.class, Connection.class, childs4, false);

        final Map<String, CtClass> childs3 = new HashMap<>();
        childs3.put("executeQuery", resultSetCt);
        childs3.put("getResultSet", resultSetCt);
        childs3.put("getGeneratedKeys", resultSetCt);
        final CtClass statementCt = f.proxyGenerator.generate(Statement.class, StatementProxy.class, Connection.class, childs3, false);

        final Map<String, CtClass> childs2 = new HashMap<>();
        childs2.put("createStatement", statementCt);
        childs2.put("prepareStatement", pStatementCt);
        childs2.put("prepareCall", cStatementCt);
        final CtClass connectionCt = f.proxyGenerator.generate(Connection.class, ConnectionProxy.class, DataSource.class, childs2, false);

        final Map<String, CtClass> childs1 = new HashMap<>();
        childs1.put("getConnection", connectionCt);
        final CtClass dataSourceCt = f.proxyGenerator.generate(DataSource.class, DataSourceProxy.class, null, childs1, true);

        final CtClass xaResourceCt = f.proxyGenerator.generate(XAResource.class, XAResourceProxy.class, XAConnection.class, new HashMap<>(), true);

        final Map<String, CtClass> childsa = new HashMap<>();
        childsa.put("getConnection", connectionCt);
        childsa.put("getXAResource", xaResourceCt);
        final CtClass xaconnectionCt = f.proxyGenerator.generate(XAConnection.class, XAConnectionProxy.class, XADataSource.class, childsa, true);

        final Map<String, CtClass> childsb = new HashMap<>();
        childsb.put("getXAConnection", xaconnectionCt);
        final CtClass xadataSourceCt = f.proxyGenerator.generate(XADataSource.class, XADataSourceProxy.class, null, childsb, true);

        final Map<String, CtClass> childsc = new HashMap<>();
        childsc.put("getConnection", connectionCt);
        final CtClass pooledConnectionCt = f.proxyGenerator.generate(PooledConnection.class, PooledConnectionProxy.class, ConnectionPoolDataSource.class, childsc, true);

        final Map<String, CtClass> childsd = new HashMap<>();
        childsd.put("getPooledConnection", pooledConnectionCt);
        final CtClass connectionPoolDataSourceCt = f.proxyGenerator.generate(ConnectionPoolDataSource.class, ConnectionPoolDataSourceProxy.class, null, childsd, true);

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


    private static void writeFile(final CtClass dataSourceCt) throws CannotCompileException, IOException {
        dataSourceCt.getClassFile().setMajorVersion(ClassFile.JAVA_8);
        dataSourceCt.writeFile("target/classes");
    }

}
