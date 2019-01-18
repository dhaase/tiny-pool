package eu.dirk.haase.jdbc.proxy.factory;

import eu.dirk.haase.jdbc.proxy.base.JdbcWrapper;
import eu.dirk.haase.jdbc.proxy.generate.DefaultClassLoader;
import eu.dirk.haase.jdbc.proxy.hybrid.ConnectionPoolDataSourceHybrid;
import eu.dirk.haase.jdbc.proxy.hybrid.ConnectionPoolXADataSourceHybrid;
import eu.dirk.haase.jdbc.proxy.hybrid.XADataSourceHybrid;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DataSourceWrapper implements Serializable {

    private final DefaultClassLoader defaultClassLoader;
    private final Map<Class<?>, Object> interfaceToClassMap;
    private transient volatile Constructor<ConnectionPoolDataSource> connectionPoolDataSourceConstructor;
    private transient volatile Constructor<DataSource> dataSourceConstructor;
    private transient volatile Constructor<XADataSource> xaDataSourceConstructor;

    public DataSourceWrapper(final Map<Class<?>, Object> interfaceToClassMap) throws Exception {
        this.defaultClassLoader = new DefaultClassLoader();
        this.interfaceToClassMap = Collections.unmodifiableMap(new HashMap<>(interfaceToClassMap));
        this.dataSourceConstructor = getDataSourceConstructor(null);
        this.xaDataSourceConstructor = getXADataSourceConstructor(null);
        this.connectionPoolDataSourceConstructor = getConnectionPoolDataSourceConstructor(null);
    }

    private void ensureOnlyWrappingOnce(Object delegate, Object wrapper) throws SQLException {
        if (delegate instanceof JdbcWrapper) {
            if (((JdbcWrapper) delegate).isWrapperFor(wrapper.getClass())) {
                throw new IllegalArgumentException("Can not wrap twice: " + wrapper.getClass());
            }
        }
    }

    public ClassLoader getClassLoader() {
        return this.defaultClassLoader.getClassLoader();
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.defaultClassLoader.setClassLoader(classLoader);
    }

    private Constructor<ConnectionPoolDataSource> getConnectionPoolDataSourceConstructor(Class<?> delegateClass) throws ClassNotFoundException {
        final Class<ConnectionPoolDataSource> ifaceClass = ConnectionPoolDataSource.class;
        if (interfaceToClassMap.containsKey(ifaceClass)) {
            final Class<?> connectionPoolDataSourceProxyClass = loadClass(interfaceToClassMap, ifaceClass);
            if (connectionPoolDataSourceProxyClass != delegateClass) {
                if (connectionPoolDataSourceConstructor == null) {
                    connectionPoolDataSourceConstructor = getDeclaredConstructor(connectionPoolDataSourceProxyClass, ifaceClass);
                }
                return connectionPoolDataSourceConstructor;
            } else {
                throw new IllegalArgumentException("Can not wrap twice: " + connectionPoolDataSourceProxyClass);
            }
        }
        return null;
    }

    private Constructor<DataSource> getDataSourceConstructor(Class<?> delegateClass) throws ClassNotFoundException {
        final Class<DataSource> ifaceClass = DataSource.class;
        if (interfaceToClassMap.containsKey(ifaceClass)) {
            final Class<?> dataSourceProxyClass = loadClass(interfaceToClassMap, ifaceClass);
            if (dataSourceProxyClass != delegateClass) {
                if (this.dataSourceConstructor == null) {
                    this.dataSourceConstructor = getDeclaredConstructor(dataSourceProxyClass, ifaceClass);
                }
                return this.dataSourceConstructor;
            } else {
                throw new IllegalArgumentException("Can not wrap twice: " + dataSourceProxyClass);
            }
        }
        return null;
    }

    private <T> Constructor<T> getDeclaredConstructor(Class<?> dataSourceProxyClass, Class<?> delegateClass) {
        try {
            final Class<?>[] arguments = {delegateClass};
            return (Constructor<T>) dataSourceProxyClass.getDeclaredConstructor(arguments);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No such Constructor on " + dataSourceProxyClass + " with argument " + delegateClass + ".", e);
        }
    }

    public Map<Class<?>, Object> getInterfaceToClassMap() {
        return interfaceToClassMap;
    }

    private Constructor<XADataSource> getXADataSourceConstructor(Class<?> delegateClass) throws ClassNotFoundException {
        final Class<XADataSource> ifaceClass = XADataSource.class;
        if (interfaceToClassMap.containsKey(ifaceClass)) {
            final Class<?> xaDataSourceProxyClass = loadClass(interfaceToClassMap, ifaceClass);
            if (xaDataSourceProxyClass != delegateClass) {
                if (this.xaDataSourceConstructor == null) {
                    this.xaDataSourceConstructor = getDeclaredConstructor(xaDataSourceProxyClass, ifaceClass);
                }
                return this.xaDataSourceConstructor;
            } else {
                throw new IllegalArgumentException("Can not wrap twice: " + xaDataSourceProxyClass);
            }
        }
        return null;
    }

    private Class<?> loadClass(Map<Class<?>, Object> ifaceToClassMap, final Class<?> iface) throws ClassNotFoundException {
        Object proxyClass = ifaceToClassMap.get(iface);
        if (proxyClass instanceof String) {
            return Class.forName((String) proxyClass, true, getClassLoader());
        }
        return (Class<?>) proxyClass;
    }

    public ConnectionPoolDataSource wrapConnectionPoolDataSource(final ConnectionPoolDataSource delegate) throws Exception {
        final ConnectionPoolDataSource wrapper = getConnectionPoolDataSourceConstructor(delegate.getClass()).newInstance(delegate);
        ensureOnlyWrappingOnce(delegate, wrapper);
        return wrapper;
    }

    public DataSource wrapDataSource(final DataSource delegate) throws Exception {
        final DataSource wrapper = getDataSourceConstructor(delegate.getClass()).newInstance(delegate);
        ensureOnlyWrappingOnce(delegate, wrapper);
        return wrapper;
    }

    public XADataSource wrapXADataSource(final XADataSource delegate) throws Exception {
        final XADataSource wrapper = getXADataSourceConstructor(delegate.getClass()).newInstance(delegate);
        ensureOnlyWrappingOnce(delegate, wrapper);
        return wrapper;
    }

    /**
     * Factory f&uuml;r die Sonderf&auml;lle das ein und dieselbe DataSource-Instanz
     * gleichzeitig mehrere Interfaces {@link DataSource} und {@link XADataSource}
     * oder {@link ConnectionPoolDataSource} implementiert.
     *
     * @see ConnectionPoolDataSourceHybrid
     * @see ConnectionPoolXADataSourceHybrid
     * @see XADataSourceHybrid
     */
    public final class Hybrid {

        private Hybrid() {
        }

        /**
         * Diese Wrapper-Klasse implementiert den Sonderfall das ein und dieselbe DataSource-Instanz
         * gleichzeitig die Interfaces {@link ConnectionPoolDataSource} und {@link DataSource}
         * implementiert.
         *
         * @param delegate die zugrundeliegende Instanz die gleichzeitig die Interfaces
         *                 {@link ConnectionPoolDataSource} und {@link DataSource} implementiert.
         * @param <T>      zusammengesetzter generischer Typ der gleichzeitig beschr&auml;nkt ist auf
         *                 {@link ConnectionPoolDataSource} und {@link DataSource}.
         * @return eine DataSource-Instanz die gleichzeitig die Interfaces
         * {@link ConnectionPoolDataSource} und {@link DataSource}
         * implementiert.
         * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekte erzeugt werden k&ouml;nnen.
         */
        @SuppressWarnings("unchecked")
        public <T extends ConnectionPoolDataSource & DataSource> T wrapConnectionPoolDataSourceHybrid(final T delegate) throws Exception {
            final T wrapper = (T) new ConnectionPoolDataSourceHybrid(wrapDataSource(delegate), wrapConnectionPoolDataSource(delegate));
            ensureOnlyWrappingOnce(delegate, wrapper);
            return wrapper;
        }

        /**
         * Diese Wrapper-Klasse implementiert den Sonderfall das ein und dieselbe DataSource-Instanz
         * gleichzeitig die Interfaces {@link ConnectionPoolDataSource}, {@link XADataSource} und
         * {@link DataSource} implementiert.
         *
         * @param delegate die zugrundeliegende Instanz die gleichzeitig die Interfaces
         *                 Interfaces {@link ConnectionPoolDataSource}, {@link XADataSource} und
         *                 {@link DataSource} implementiert.
         * @param <T>      zusammengesetzter generischer Typ der gleichzeitig beschr&auml;nkt ist auf die
         *                 Interfaces {@link ConnectionPoolDataSource}, {@link XADataSource} und
         *                 {@link DataSource}.
         * @return eine DataSource-Instanz die gleichzeitig die Interfaces
         * {@link ConnectionPoolDataSource}, {@link XADataSource} und
         * {@link DataSource} implementiert.
         * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekte erzeugt werden k&ouml;nnen.
         */
        @SuppressWarnings("unchecked")
        public <T extends ConnectionPoolDataSource & XADataSource & DataSource> T wrapConnectionPoolXADataSourceHybrid(final T delegate) throws Exception {
            final T wrapper = (T) new ConnectionPoolXADataSourceHybrid(wrapDataSource(delegate), wrapConnectionPoolDataSource(delegate), wrapXADataSource(delegate));
            ensureOnlyWrappingOnce(delegate, wrapper);
            return wrapper;
        }

        /**
         * Diese Wrapper-Klasse implementiert den Sonderfall das ein und dieselbe DataSource-Instanz
         * gleichzeitig die Interfaces {@link XADataSource} und {@link DataSource} implementiert.
         *
         * @param delegate die zugrundeliegende Instanz die gleichzeitig die Interfaces
         *                 {@link XADataSource} und {@link DataSource} implementiert.
         * @param <T>      zusammengesetzter generischer Typ der gleichzeitig beschr&auml;nkt ist auf
         *                 die Interfaces {@link XADataSource} und {@link DataSource}.
         * @return eine DataSource-Instanz die gleichzeitig die Interfaces
         * {@link XADataSource} und {@link DataSource} implementiert.
         * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekte erzeugt werden k&ouml;nnen.
         */
        @SuppressWarnings("unchecked")
        public <T extends XADataSource & DataSource> T wrapXADataSourceHybrid(final T delegate) throws Exception {
            final T wrapper = (T) new XADataSourceHybrid(wrapDataSource(delegate), wrapXADataSource(delegate));
            ensureOnlyWrappingOnce(delegate, wrapper);
            return wrapper;
        }

    }
}
