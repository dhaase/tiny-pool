package eu.dirk.haase.jdbc.proxy.generate;

import java.util.function.Supplier;

public class ObjectMaker implements Supplier<Object> {

    private final Class<?> implClass;
    private final Object arg1;
    private final Object arg2;

    public ObjectMaker(final Class<?> implClass, final Object arg1, final Object arg2) {
        this.implClass = implClass;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public Object get() {
        try {
            return implClass.getDeclaredConstructors()[0].newInstance(arg1, arg2);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}


