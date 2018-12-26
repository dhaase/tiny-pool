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

    private final JavassistProxyClasses javassistProxyClasses;

    private JavassistProxyFactory() {
        this.javassistProxyClasses = new JavassistProxyClasses();
    }


    public static void main(final String... args) throws Exception {
        final JavassistProxyFactory f = new JavassistProxyFactory();

        f.javassistProxyClasses.generate();
        f.javassistProxyClasses.writeFile();
    }


}
