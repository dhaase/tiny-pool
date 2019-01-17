package eu.dirk.haase.jdbc.proxy.generate;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import javassist.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JavassistProxyClassGenerator {

    private final Set<String> allFieldSet;
    private final Set<String> allInitFieldSet;
    private final Set<String> allMethodSet;
    private final Function<String, String> delegateMethodBody;
    private final boolean isWrapMethodConcurrent;
    private final String newClassName;
    private final Class<?> primaryIfaceClass;
    private final Class<?> superClass;
    private final BiFunction<String, String, String> wrapMethodBody;
    private ClassPool classPool;

    public JavassistProxyClassGenerator(final BiFunction<String, Class<?>, String> classNameFun, final Class<?> primaryIfaceClass, final Class<?> superClass) {
        this.newClassName = classNameFun.apply(superClass.getName(), primaryIfaceClass);
        this.delegateMethodBody = (d) -> "{ try { return delegate." + d + "($$); } catch (SQLException e) { throw checkException(e); } }";
        this.wrapMethodBody = (w, d) -> "{ try { return " + w + "(delegate." + d + "($$), $args); } catch (SQLException e) { throw checkException(e); } }";
        this.primaryIfaceClass = primaryIfaceClass;
        this.superClass = superClass;
        this.isWrapMethodConcurrent = ConcurrentFactoryJdbcProxy.class.isAssignableFrom(superClass);
        this.allInitFieldSet = new HashSet<>();
        this.allFieldSet = new HashSet<>();
        this.allMethodSet = new HashSet<>();
    }

    private <T> void addAPIMethods(final CtClass targetCt, final Class<T> primaryInterface, final CtClass superCt, final Map<String, CtClass> childs) throws NotFoundException, CannotCompileException {
        excludeFinalMethods(superCt, allMethodSet);
        CtClass intfClass = classPool.getCtClass(primaryInterface.getName());
        for (CtMethod intfMethod : intfClass.getMethods()) {
            final String signature = getSignature(intfMethod);
            if (allMethodSet.add(signature) && isPublic(intfMethod.getModifiers())) {
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

    private CtConstructor addConstructor(final CtClass targetCt, final CtClass parentIfCt, final CtClass primaryIfCt, final CtField field) throws CannotCompileException, NotFoundException {
        CtClass[] parameter;
        String methodBody;
        if (parentIfCt != null) {
            parameter = new CtClass[3];
            parameter[0] = primaryIfCt;
            parameter[1] = parentIfCt;
            parameter[2] = classPool.getCtClass(Object[].class.getName());
            methodBody = "{ super($1, $2, $3); this." + field.getName() + " = $1; }";
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

    private CtField addField(CtClass targetCt, CtClass ifaceCt, String fieldName) throws CannotCompileException, NotFoundException {
        if (allFieldSet.add(fieldName)) {
            CtField field = new CtField(ifaceCt, fieldName, targetCt);
            field.setModifiers(Modifier.FINAL | Modifier.PRIVATE);
            targetCt.addField(field);
            return field;
        } else {
            return targetCt.getDeclaredField(fieldName);
        }
    }

    private void addWrapMethod(CtClass targetCt, CtConstructor targetConstructorCt, Map<String, CtClass> childs, boolean isWrapMethodConcurrent) throws NotFoundException, CannotCompileException {
        final CtClass factoryCt = classPool.getCtClass(BiFunction.class.getName());

        for (CtClass child : childs.values()) {
            final CtClass ifaceParentCt = child.getInterfaces()[0];
            // fuege das BiFunction-Field mit Initialisierung als Factory-Function hinzu:
            final String objectMakerFieldName = "new" + ifaceParentCt.getSimpleName();
            addField(targetCt, factoryCt, objectMakerFieldName);
            if (allInitFieldSet.add(objectMakerFieldName)) {
                targetConstructorCt.insertAfter(objectMakerFieldName + " = new ObjectMaker(" + child.getName() + ".class, $1);");
            }
            // fuege die Wrap-Methode hinzu:
            CtClass[] wrapParameter = {ifaceParentCt, classPool.getCtClass(Object[].class.getName())};
            final String wrapMethodName = "wrap" + ifaceParentCt.getSimpleName();
            CtMethod wrapMethod = new CtMethod(ifaceParentCt, wrapMethodName, wrapParameter, targetCt);
            final String signature = getSignature(wrapMethod);
            if (allMethodSet.add(signature)) {
                wrapMethod.setModifiers(Modifier.FINAL | Modifier.PROTECTED | Modifier.VARARGS);
                String superWrapMethod = (isWrapMethodConcurrent ? "wrapConcurrent" : "wrap");
                String body = "";
                body += "{ ";
                body += " return super." + superWrapMethod + "($1, " + objectMakerFieldName + ", $2); ";
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

    public <T> CtClass generate(final ClassPool classPool, final Class<?> parentIfaceClass, final Map<String, CtClass> childs) {
        try {
            init(classPool);

            final CtClass parentIfCt = (parentIfaceClass != null ? classPool.getCtClass(parentIfaceClass.getName()) : null);
            final CtClass superCt = classPool.getCtClass(superClass.getName());
            final CtClass targetCt = classPool.makeClass(newClassName, superCt);
            targetCt.setModifiers(Modifier.FINAL | Modifier.PUBLIC);

            final CtClass primaryIfCt = classPool.getCtClass(primaryIfaceClass.getName());
            targetCt.addInterface(primaryIfCt);

            final CtField field = addField(targetCt, primaryIfCt, "delegate");
            final CtConstructor targetConstructorCt = addConstructor(targetCt, parentIfCt, primaryIfCt, field);
            if (childs != null) {
                addWrapMethod(targetCt, targetConstructorCt, childs, isWrapMethodConcurrent);
            }
            addAPIMethods(targetCt, primaryIfaceClass, superCt, childs);

            return targetCt;
        } catch (Exception ex) {
            throw new IllegalStateException(ex.toString(), ex);
        }
    }

    private String getSignature(CtMethod intfMethod) {
        // Die Signatur wird in Javassist gebildet aus:
        // den Parameter-Typen und den Rueckgabe-Typ.
        // Daher muss der Methoden-Name noch hinzugefuegt
        // werden:
        return intfMethod.getName() + intfMethod.getSignature();
    }

    private void init(ClassPool classPool) {
        this.classPool = classPool;
        this.allInitFieldSet.clear();
        this.allFieldSet.clear();
        this.allMethodSet.clear();
    }

    /**
     * Liefert {@code true} wenn der {@link Modifier} {@code public} ist.
     *
     * @param modifier der Modifier der gepr&uuml;ft werden soll.
     * @return {@code true} wenn der {@link Modifier} {@code public} ist.
     */
    private boolean isPublic(final int modifier) {
        return (modifier & Modifier.PUBLIC) != 0;
    }


}
