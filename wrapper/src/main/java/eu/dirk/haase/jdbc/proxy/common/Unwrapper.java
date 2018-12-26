package eu.dirk.haase.jdbc.proxy.common;

import java.sql.SQLException;
import java.sql.Wrapper;

public final class Unwrapper {

    private Unwrapper() {
    }

    public static boolean isWrapperFor(Class<?> iface, final Object wrapper, final Object delegate) throws SQLException {
        if (iface.isInstance(wrapper)) {
            return true;
        } else if (Wrapper.class.isInstance(delegate)) {
            return ((Wrapper) delegate).isWrapperFor(iface);
        }
        return false;
    }

    public static <T> T unwrap(Class<T> iface, final Object wrapper, final Object delegate) throws SQLException {
        if (iface.isInstance(wrapper)) {
            return (T) wrapper;
        } else if (Wrapper.class.isInstance(delegate)) {
            return ((Wrapper) delegate).unwrap(iface);
        }
        throw new SQLException("No wrapper for " + iface);
    }

}
