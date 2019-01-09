package eu.dirk.haase.jdbc.proxy.generate;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.BiFunction;

public class ObjectMaker implements BiFunction<Object, Object[], Object> {

    private final Class<?> implClass;
    private final Object parentObject;


    public ObjectMaker(final Class<?> implClass, final Object parentObject) {
        this.implClass = implClass;
        this.parentObject = parentObject;
    }

    @Override
    public Object apply(Object delegate, final Object[] argumentArray) {
        if (implClass != delegate.getClass()) {
            try {
                if (isClosed(delegate)) {
                    throw new IllegalStateException("Instance is already closed: " + delegate.getClass());
                }
                final Constructor<?>[] declaredConstructors = implClass.getDeclaredConstructors();
                if (declaredConstructors.length == 1) {
                    return declaredConstructors[0].newInstance(delegate, parentObject, argumentArray);
                } else {
                    throw new IllegalStateException("Only one constructor expected, but " + implClass + " has " + declaredConstructors.length);
                }
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception ex) {
                throw new IllegalStateException(ex.toString(), ex);
            }
        } else {
            throw new IllegalStateException("Can not wrap twice: " + implClass);
        }
    }

    private boolean isClosed(Object delegate) throws SQLException {
        if (delegate instanceof Connection) {
            return ((Connection) delegate).isClosed();
        } else if (delegate instanceof Statement) {
            return ((Statement) delegate).isClosed();
        } else if (delegate instanceof ResultSet) {
            return ((ResultSet) delegate).isClosed();
        }
        return false;
    }

}


