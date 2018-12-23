package eu.dirk.haase.jdbc.pool.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.bytecode.ClassFile;

import javax.sql.DataSource;

public final class JavassistProxyFactory {

    private final ClassPool classPool = new ClassPool();

    private final ProxyClassGenerator dataSourceProxyGenerator;

    private JavassistProxyFactory() {
        this.dataSourceProxyGenerator = new ProxyClassGenerator(classPool);
    }

    public static void main(String... args) throws Exception {
        JavassistProxyFactory factory = new JavassistProxyFactory();

        factory.classPool.importPackage("java.sql");
        factory.classPool.importPackage("javax.sql");
        factory.classPool.appendClassPath(new LoaderClassPath(JavassistProxyFactory.class.getClassLoader()));

        String methodBody = "{ try { return this.delegate.method($$); } catch (SQLException e) { throw checkException(e); } }";
        CtClass targetCt = factory.dataSourceProxyGenerator.generate(DataSource.class, DataSourceProxy.class.getName(), methodBody);

        targetCt.getClassFile().setMajorVersion(ClassFile.JAVA_8);
        targetCt.writeFile( "target/classes");
    }

}
