package eu.dirk.haase.jdbc.proxy.generate;

import java.util.function.Function;

public class ObjectMaker implements Function<Object, Object> {

    private final Class<?> implClass;
    private final Object parentObject;

    public ObjectMaker(final Class<?> implClass, final Object parentObject) {
        this.implClass = implClass;
        this.parentObject = parentObject;
    }

    @Override
    public Object apply(Object delegate) {
        if (implClass != delegate.getClass()) {
            try {
                return implClass.getDeclaredConstructors()[0].newInstance(delegate, parentObject);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalStateException("Can not wrap twice: " + this.implClass);
        }
    }

}


