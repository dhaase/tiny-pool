package eu.dirk.haase.jdbc.proxy.generate;

import javassist.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JavassistProxyClassGenerator {

    private static final String prefix = "W";
    private static final Function<String, String> CLASS_NAME_FUN = (cn) -> cn.replaceAll("(.+)\\.(\\w+)", "$1." + prefix + "$2");

    private final ClassPool classPool;
    private final Function<String, String> classNameFun;
    private final Function<String, String> delegateMethodBody;
    private final BiFunction<String, String, String> wrapMethodBody;


    public JavassistProxyClassGenerator(final ClassPool classPool) {
        this(classPool, CLASS_NAME_FUN);
    }


    public JavassistProxyClassGenerator(final ClassPool classPool, final Function<String, String> classNameFun) {
        this.classPool = classPool;
        this.classNameFun = classNameFun;
        this.delegateMethodBody = (d) -> "{ try { return delegate." + d + "($$); } catch (SQLException e) { throw checkException(e); } }";
        this.wrapMethodBody = (w, d) -> "{ try { return " + w + "(delegate." + d + "($$)); } catch (SQLException e) { throw checkException(e); } }";
    }

    public <T> CtClass generate(final Class<T> primaryIfaceClass, final Class<?> superClass, final Class<?> parentIfaceClass, final Map<String, CtClass> childs) throws Exception {
        final String newClassName = classNameFun.apply(superClass.getName());
        System.out.println("Generating " + newClassName);

        CtClass parentIfCt = null;
        final CtClass primaryIfCt = classPool.getCtClass(primaryIfaceClass.getName());
        final CtClass superCt = classPool.getCtClass(superClass.getName());
        final CtClass targetCt = classPool.makeClass(newClassName, superCt);

        targetCt.setModifiers(Modifier.FINAL | Modifier.PUBLIC);
        targetCt.addInterface(primaryIfCt);

        if (parentIfaceClass != null) {
            parentIfCt = classPool.getCtClass(parentIfaceClass.getName());
        }

        final CtField field = addField(primaryIfCt, targetCt);
        addConstructor(parentIfCt, primaryIfCt, targetCt, field);

        if (childs != null) {
            addWrapperMethod(childs, targetCt);
        }

        addMethods(primaryIfaceClass, superCt, targetCt, childs);

        return targetCt;
    }

    private void addWrapperMethod(Map<String, CtClass> childs, CtClass targetCt) throws NotFoundException, CannotCompileException {
        final Set<String> methodSet = new HashSet<>();
        for (CtClass child : childs.values()) {
            final CtClass ifaceParentCt = child.getInterfaces()[0];
            CtClass[] parameter = {ifaceParentCt};
            final String wrapMethodName = "wrap" + ifaceParentCt.getSimpleName();
            CtMethod wrapMethod = new CtMethod(ifaceParentCt, wrapMethodName, parameter, targetCt);
            final String signature = getSignature(wrapMethod);
            if (methodSet.add(signature)) {
                wrapMethod.setModifiers(Modifier.FINAL | Modifier.PROTECTED);
                String body = "";
                body += "{ ";
                body += " Supplier supplier = new ObjectMaker(" + child.getName() + ".class, this, $1);";
                body += " return wrap($1, supplier); ";
                body += "}";
                wrapMethod.setBody(body);
                targetCt.addMethod(wrapMethod);
            }
        }
    }

    private <T> void addMethods(final Class<T> primaryInterface, final CtClass superCt, final CtClass targetCt, final Map<String, CtClass> childs) throws NotFoundException, CannotCompileException {
        final Set<String> methodSet = new HashSet<>();
        excludeFinalMethods(superCt, methodSet);
        CtClass intfClass = classPool.getCtClass(primaryInterface.getName());
        for (CtMethod intfMethod : intfClass.getMethods()) {
            final String signature = getSignature(intfMethod);
            if (methodSet.add(signature)) {
                CtMethod newMethod = new CtMethod(intfMethod.getReturnType(), intfMethod.getName(), intfMethod.getParameterTypes(), targetCt);
                newMethod.setExceptionTypes(intfMethod.getExceptionTypes());
                CtClass child = childs.get(intfMethod.getName());
                if (child == null) {
                    newMethod.setBody(delegateMethodBody.apply(intfMethod.getName()));
                } else {
                    final CtClass ifaceParentCt = child.getInterfaces()[0];
                    final String wrapMethodName = "wrap" + ifaceParentCt.getSimpleName();
                    newMethod.setBody(this.wrapMethodBody.apply(wrapMethodName, intfMethod.getName()));
                }
                targetCt.addMethod(newMethod);
            }
        }
    }

    private void excludeFinalMethods(final CtClass superCt, final Set<String> methodSet) {
        for (CtMethod superMethod : superCt.getMethods()) {
            final String signature = getSignature(superMethod);
            if ((superMethod.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
                methodSet.add(signature);
            }
        }
    }

    private String getSignature(CtMethod intfMethod) {
        return intfMethod.getName() + intfMethod.getSignature();
    }

    private void addConstructor(final CtClass parentIfCt, final CtClass primaryIfCt, final CtClass targetCt, final CtField field) throws CannotCompileException {
        CtClass[] parameter;
        String methodBody;
        if (parentIfCt != null) {
            parameter = new CtClass[2];
            parameter[0] = parentIfCt;
            parameter[1] = primaryIfCt;
            methodBody = "{ super($1, $2); this." + field.getName() + " = $2; }";
        } else {
            parameter = new CtClass[1];
            parameter[0] = primaryIfCt;
            methodBody = "{ super($1); this." + field.getName() + " = $1; }";
        }
        CtClass[] exeptions = {};
        CtConstructor constructor = CtNewConstructor.make(parameter, exeptions, targetCt);
        constructor.setBody(methodBody);
        targetCt.addConstructor(constructor);
    }

    private CtField addField(CtClass ifaceCt, CtClass targetCt) throws CannotCompileException {
        CtField field = new CtField(ifaceCt, "delegate", targetCt);
        field.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
        targetCt.addField(field);
        return field;
    }


}
