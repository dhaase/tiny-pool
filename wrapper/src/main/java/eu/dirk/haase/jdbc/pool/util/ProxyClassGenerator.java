package eu.dirk.haase.jdbc.pool.util;

import javassist.*;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public class ProxyClassGenerator {

    private static final Function<String, String> CLASS_NAME_FUN = (cn) -> cn.replaceAll("(.+)\\.(\\w+)", "$1.Hikari$2");

    private final ClassPool classPool;
    private final Function<String, String> classNameFun;

    public ProxyClassGenerator(final ClassPool classPool) {
        this(classPool, CLASS_NAME_FUN);
    }

    public ProxyClassGenerator(final ClassPool classPool, final Function<String, String> classNameFun) {
        this.classPool = classPool;
        this.classNameFun = classNameFun;
    }

    public <T> CtClass generate(Class<T> primaryInterface, String superClassName, String methodBody) throws Exception {
        String newClassName = classNameFun.apply(superClassName);
        System.out.println("Generating " + newClassName);

        CtClass ifaceCt = classPool.getCtClass(primaryInterface.getName());
        CtClass superCt = classPool.getCtClass(superClassName);
        CtClass targetCt = classPool.makeClass(newClassName, superCt);

        targetCt.setModifiers(Modifier.FINAL | Modifier.PUBLIC);
        targetCt.addInterface(ifaceCt);

        CtField field = addField(ifaceCt, targetCt);
        addConstructor(ifaceCt, targetCt, field);

        addMethods(primaryInterface, methodBody, targetCt);

        return targetCt;
    }

    private <T> void addMethods(Class<T> primaryInterface, String methodBody, CtClass targetCt) throws NotFoundException, CannotCompileException {
        for (Class<?> intf : getAllInterfaces(primaryInterface)) {
            CtClass intfCt = classPool.getCtClass(intf.getName());
            for (CtMethod intfMethod : intfCt.getDeclaredMethods()) {
                final String signature = getSignature(intfMethod);

                CtMethod method = new CtMethod(intfMethod.getReturnType(), intfMethod.getName(), intfMethod.getParameterTypes(), targetCt);

                method.setBody(methodBody.replace("method", intfMethod.getName()));
                targetCt.addMethod(method);
            }
        }
    }

    private String getSignature(CtMethod intfMethod) {
        return intfMethod.getName() + intfMethod.getSignature();
    }


    private void addConstructor(CtClass ifaceCt, CtClass targetCt, CtField field) throws CannotCompileException {
        CtClass[] parameter = {ifaceCt};
        CtClass[] exeptions = {};
        CtConstructor constructor = CtNewConstructor.make(parameter, exeptions, targetCt);
        constructor.setBody("{super($1); this." + field.getName() + " = $1;}");
        targetCt.addConstructor(constructor);
    }


    private CtField addField(CtClass ifaceCt, CtClass targetCt) throws CannotCompileException {
        CtField field = new CtField(ifaceCt, "delegate", targetCt);
        field.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
        targetCt.addField(field);
        return field;
    }


    private static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        for (Class<?> intf : clazz.getInterfaces()) {
            if (intf.getInterfaces().length > 0) {
                interfaces.addAll(getAllInterfaces(intf));
            }
            interfaces.add(intf);
        }
        if (clazz.getSuperclass() != null) {
            interfaces.addAll(getAllInterfaces(clazz.getSuperclass()));
        }

        if (clazz.isInterface()) {
            interfaces.add(clazz);
        }

        return interfaces;
    }

}
