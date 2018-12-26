package eu.dirk.haase.jdbc.proxy.generate;

import java.util.function.Function;

public class ObjectMaker implements Function<Object, Object> {

    private final Class<?> implClass;
    private final Object arg2;

    public ObjectMaker(final Class<?> implClass, final Object arg2) {
        this.implClass = implClass;
        this.arg2 = arg2;
    }

    @Override
    public Object apply(Object arg1) {
        try {
            return implClass.getDeclaredConstructors()[0].newInstance(arg1, arg2);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}


