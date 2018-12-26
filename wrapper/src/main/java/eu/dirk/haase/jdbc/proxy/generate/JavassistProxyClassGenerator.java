package eu.dirk.haase.jdbc.proxy.generate;

import javassist.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class JavassistProxyClassGenerator {

    private static final String prefix = "W";
    private static final Function<String, String> CLASS_NAME_FUN = (cn) -> cn.replaceAll("(.+)\\.(\\w+)", "$1." + prefix + "$2");
    private final Function<String, String> classNameFun;
    private final ClassPool classPool;
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

    public <T> CtClass generate(final Class<T> primaryIfaceClass, final Class<?> superClass, final Class<?> parentIfaceClass, final Map<String, CtClass> childs, boolean isWrapMethodSynchronized) throws Exception {
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

        final CtField field = addField(targetCt, primaryIfCt, "delegate");
        CtConstructor targetConstructorCt = addConstructor(targetCt, parentIfCt, primaryIfCt, field);

        if (childs != null) {
            addWrapMethod(targetCt, targetConstructorCt, childs, isWrapMethodSynchronized);
        }

        addAPIMethods(targetCt, primaryIfaceClass, superCt, childs);

        return targetCt;
    }

    private <T> void addAPIMethods(final CtClass targetCt, final Class<T> primaryInterface, final CtClass superCt, final Map<String, CtClass> childs) throws NotFoundException, CannotCompileException {
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

    private CtConstructor addConstructor(final CtClass targetCt, final CtClass parentIfCt, final CtClass primaryIfCt, final CtField field) throws CannotCompileException {
        CtClass[] parameter;
        String methodBody;
        if (parentIfCt != null) {
            parameter = new CtClass[2];
            parameter[0] = primaryIfCt;
            parameter[1] = parentIfCt;
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
        return constructor;
    }

    private CtField addField(CtClass targetCt, CtClass ifaceCt, String fieldName) throws CannotCompileException {
        CtField field = new CtField(ifaceCt, fieldName, targetCt);
        field.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
        targetCt.addField(field);
        return field;
    }

    private void addWrapMethod(CtClass targetCt, CtConstructor targetConstructorCt, Map<String, CtClass> childs, boolean isWrapMethodSynchronized) throws NotFoundException, CannotCompileException {
        final Set<String> fieldSet = new HashSet<>();
        final Set<String> methodSet = new HashSet<>();
        final CtClass factoryCt = classPool.getCtClass(Function.class.getName());
        for (CtClass child : childs.values()) {
            final CtClass ifaceParentCt = child.getInterfaces()[0];
            // fuege das Supplier-Field mit Initialisierung als Factory-Function hinzu:
            final String objectMakerFieldName = "new" + ifaceParentCt.getSimpleName();
            if (fieldSet.add(objectMakerFieldName)) {
                addField(targetCt, factoryCt, objectMakerFieldName);
                targetConstructorCt.insertAfter(objectMakerFieldName + " = new ObjectMaker(" + child.getName() + ".class, $1);");
            }
            // fuege die Wrap-Methode hinzu:
            CtClass[] parameter = {ifaceParentCt};
            final String wrapMethodName = "wrap" + ifaceParentCt.getSimpleName();
            CtMethod wrapMethod = new CtMethod(ifaceParentCt, wrapMethodName, parameter, targetCt);
            final String signature = getSignature(wrapMethod);
            if (methodSet.add(signature)) {
                wrapMethod.setModifiers(Modifier.FINAL | Modifier.PROTECTED);
                String superWrapMethod = (isWrapMethodSynchronized ? "wrapSynchronized" : "wrap");
                String body = "";
                body += "{ ";
                body += " return super." + superWrapMethod + "($1, " + objectMakerFieldName + "); ";
                body += "}";
                wrapMethod.setBody(body);
                targetCt.addMethod(wrapMethod);
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
        // Signature wird in Javassist gebildet aus:
        // den Parameter-Typen und den Rueckgabe-Typ.
        // Daher muss der Methoden-Name noch hinzugefuegt
        // werden:
        return intfMethod.getName() + intfMethod.getSignature();
    }


}
