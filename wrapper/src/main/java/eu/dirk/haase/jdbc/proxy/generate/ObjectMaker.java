package eu.dirk.haase.jdbc.proxy.generate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
                if (isClosed(delegate)) {
                    throw new IllegalStateException("Instance is already closed: " + delegate.getClass());
                }
                return implClass.getDeclaredConstructors()[0].newInstance(delegate, parentObject);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new IllegalStateException(e);
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


