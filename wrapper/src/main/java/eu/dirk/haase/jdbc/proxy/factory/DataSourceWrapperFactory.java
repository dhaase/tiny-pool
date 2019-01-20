package eu.dirk.haase.jdbc.proxy.factory;

import eu.dirk.haase.jdbc.proxy.base.JdbcWrapper;
import eu.dirk.haase.jdbc.proxy.generate.Generator;
import eu.dirk.haase.jdbc.proxy.generate.MultipleParentClassLoader;
import eu.dirk.haase.jdbc.proxy.hybrid.ConnectionPoolDataSourceHybrid;
import eu.dirk.haase.jdbc.proxy.hybrid.ConnectionPoolXADataSourceHybrid;
import eu.dirk.haase.jdbc.proxy.hybrid.XADataSourceHybrid;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class DataSourceWrapperFactory {

    private final Map<Class<?>, Object> interfaceToClassMap;
    private volatile Constructor<ConnectionPoolDataSource> connectionPoolDataSourceConstructor;
    private volatile Constructor<DataSource> dataSourceConstructor;
    private volatile Constructor<XADataSource> xaDataSourceConstructor;

    public DataSourceWrapperFactory(final Map<Class<?>, Object> ifaceToWrapperClassMap) throws Exception {
        this.interfaceToClassMap = new HashMap<>(ifaceToWrapperClassMap);
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

    /**
     * Liefert eine DataSource-Factory um DataSource-Instanzen in einem Wrapper einzupacken.
     *
     * @param iface2CustomClassMap eine Map mit abstrakten Klassen von denen die JDBC-Wrapper
     *                             Klassen abgeleitet werden sollen.
     * @return eine DataSource-Factory um DataSources in einen Wrapper einzupacken.
     * @throws Exception wird ausgel&ouml;st wenn die DataSource-Factory nicht erzeugt werden kann.
     * @see #wrapConnectionPoolDataSource(javax.sql.ConnectionPoolDataSource)
     * @see #wrapDataSource(javax.sql.DataSource)
     * @see #wrapXADataSource(javax.sql.XADataSource)
     */
    public static DataSourceWrapperFactory newInstance(final Map<Class<?>, Class<?>> iface2CustomClassMap) throws Exception {
        final Map<Class<?>, Object> ifaceToWrapperClassMap = Generator.instance().generate(iface2CustomClassMap);
        return new DataSourceWrapperFactory(ifaceToWrapperClassMap);
    }

    private Constructor<ConnectionPoolDataSource> getConnectionPoolDataSourceConstructor(Class<?> delegateClass) throws ClassNotFoundException {
        final Class<ConnectionPoolDataSource> ifaceClass = ConnectionPoolDataSource.class;
        if (interfaceToClassMap.containsKey(ifaceClass)) {
            final Class<?> connectionPoolDataSourceProxyClass = loadClass(interfaceToClassMap, ifaceClass);
            if (connectionPoolDataSourceProxyClass != delegateClass) {
                if ((delegateClass == null) || ifaceClass.isAssignableFrom(delegateClass)) {
                    if (connectionPoolDataSourceConstructor == null) {
                        connectionPoolDataSourceConstructor = getDeclaredConstructor(connectionPoolDataSourceProxyClass, ifaceClass);
                    }
                    return connectionPoolDataSourceConstructor;
                } else {
                    throw new IllegalArgumentException(delegateClass + " is not implementing " + ifaceClass);
                }
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
                if ((delegateClass == null) || ifaceClass.isAssignableFrom(delegateClass)) {
                    if (this.dataSourceConstructor == null) {
                        this.dataSourceConstructor = getDeclaredConstructor(dataSourceProxyClass, ifaceClass);
                    }
                    return this.dataSourceConstructor;
                } else {
                    throw new IllegalArgumentException(delegateClass + " is not implementing " + ifaceClass);
                }
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

    private Constructor<XADataSource> getXADataSourceConstructor(Class<?> delegateClass) throws ClassNotFoundException {
        final Class<XADataSource> ifaceClass = XADataSource.class;
        if (interfaceToClassMap.containsKey(ifaceClass)) {
            final Class<?> xaDataSourceProxyClass = loadClass(interfaceToClassMap, ifaceClass);
            if (xaDataSourceProxyClass != delegateClass) {
                if ((delegateClass == null) || ifaceClass.isAssignableFrom(delegateClass)) {
                    if (this.xaDataSourceConstructor == null) {
                        this.xaDataSourceConstructor = getDeclaredConstructor(xaDataSourceProxyClass, ifaceClass);
                    }
                    return this.xaDataSourceConstructor;
                } else {
                    throw new IllegalArgumentException(delegateClass + " is not implementing " + ifaceClass);
                }
            } else {
                throw new IllegalArgumentException("Can not wrap twice: " + xaDataSourceProxyClass);
            }
        }
        return null;
    }

    private Class<?> loadClass(Map<Class<?>, Object> ifaceToClassMap, final Class<?> iface) throws ClassNotFoundException {
        Object proxyClass = ifaceToClassMap.get(iface);
        if (proxyClass instanceof String) {
            return Class.forName((String) proxyClass, true, new MultipleParentClassLoader());
        }
        return (Class<?>) proxyClass;
    }

    /**
     * Dekoriert eine {@link ConnectionPoolDataSource}-Instanz mit einem Wrapper.
     *
     * @param delegate die zugrundeliegende Instanz.
     * @return die dekorierte {@link ConnectionPoolDataSource}-Instanz.
     * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekt
     *                   erzeugt werden k&ouml;nnen.
     */
    public ConnectionPoolDataSource wrapConnectionPoolDataSource(final ConnectionPoolDataSource delegate) throws Exception {
        final ConnectionPoolDataSource wrapper = getConnectionPoolDataSourceConstructor(delegate.getClass()).newInstance(delegate);
        ensureOnlyWrappingOnce(delegate, wrapper);
        return wrapper;
    }

    /**
     * Dekoriert eine {@link DataSource}-Instanz mit einem Wrapper.
     *
     * @param delegate die zugrundeliegende Instanz.
     * @return die dekorierte {@link DataSource}-Instanz.
     * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekt
     *                   erzeugt werden k&ouml;nnen.
     */
    public DataSource wrapDataSource(final DataSource delegate) throws Exception {
        final DataSource wrapper = getDataSourceConstructor(delegate.getClass()).newInstance(delegate);
        ensureOnlyWrappingOnce(delegate, wrapper);
        return wrapper;
    }

    /**
     * Dekoriert eine {@link XADataSource}-Instanz mit einem Wrapper.
     *
     * @param delegate die zugrundeliegende Instanz.
     * @return die dekorierte {@link XADataSource}-Instanz.
     * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekt
     *                   erzeugt werden k&ouml;nnen.
     */
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
     * @param <T1> generischer Typ der auf {@link DataSource} beschr&auml;nkt ist.
     * @param <T2> zusammengesetzter generischer Typ der gleichzeitig beschr&auml;nkt
     *             ist auf die Interfaces {@link XADataSource} und {@link DataSource}.
     * @param <T3> zusammengesetzter generischer Typ der gleichzeitig beschr&auml;nkt
     *             ist auf die Interfaces {@link ConnectionPoolDataSource},
     *             {@link XADataSource} und {@link DataSource}.
     * @param <T4> zusammengesetzter generischer Typ der gleichzeitig beschr&auml;nkt
     *             ist auf {@link ConnectionPoolDataSource} und {@link DataSource}.
     * @see ConnectionPoolDataSourceHybrid
     * @see ConnectionPoolXADataSourceHybrid
     * @see XADataSourceHybrid
     */
    public final class Hybrid<
            T1 extends DataSource,
            T2 extends DataSource & XADataSource,
            T3 extends DataSource & XADataSource & ConnectionPoolDataSource,
            T4 extends DataSource & ConnectionPoolDataSource> {

        private Hybrid() {
        }

        /**
         * Dekoriert eine DataSource-Instanz mit einem Wrapper der das Interface
         * {@link DataSource} zusammen mit {@link ConnectionPoolDataSource} implementiert.
         *
         * @param delegate die zugrundeliegende Instanz die gleichzeitig die Interfaces
         *                 {@link ConnectionPoolDataSource} und {@link DataSource} implementiert.
         * @return eine DataSource-Instanz die gleichzeitig die Interfaces
         * {@link ConnectionPoolDataSource} und {@link DataSource}
         * implementiert.
         * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekte erzeugt werden k&ouml;nnen.
         */
        @SuppressWarnings("unchecked")
        public T4 wrapConnectionPoolDataSourceHybrid(final T4 delegate) throws Exception {
            final T4 wrapper = (T4) new ConnectionPoolDataSourceHybrid(wrapDataSource(delegate), wrapConnectionPoolDataSource(delegate));
            ensureOnlyWrappingOnce(delegate, wrapper);
            return wrapper;
        }

        /**
         * Dekoriert eine DataSource-Instanz mit einem Wrapper der das Interface
         * {@link DataSource} zusammen mit {@link XADataSource} und
         * {@link ConnectionPoolDataSource} implementiert.
         *
         * @param delegate die zugrundeliegende Instanz die gleichzeitig die Interfaces
         *                 Interfaces {@link ConnectionPoolDataSource}, {@link XADataSource} und
         *                 {@link DataSource} implementiert.
         * @return eine DataSource-Instanz die gleichzeitig die Interfaces
         * {@link ConnectionPoolDataSource}, {@link XADataSource} und
         * {@link DataSource} implementiert.
         * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekte erzeugt werden k&ouml;nnen.
         */
        @SuppressWarnings("unchecked")
        public T3 wrapConnectionPoolXADataSourceHybrid(final T3 delegate) throws Exception {
            final T3 wrapper = (T3) new ConnectionPoolXADataSourceHybrid(wrapDataSource(delegate), wrapConnectionPoolDataSource(delegate), wrapXADataSource(delegate));
            ensureOnlyWrappingOnce(delegate, wrapper);
            return wrapper;
        }

        /**
         * Dekoriert eine DataSource-Instanz mit einem Wrapper der das Interface
         * {@link DataSource} zusammen mit {@link XADataSource} implementiert.
         *
         * @param delegate die zugrundeliegende Instanz die gleichzeitig die Interfaces
         *                 {@link XADataSource} und {@link DataSource} implementiert.
         * @return eine DataSource-Instanz die gleichzeitig die Interfaces
         * {@link XADataSource} und {@link DataSource} implementiert.
         * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekte erzeugt werden k&ouml;nnen.
         */
        @SuppressWarnings("unchecked")
        public T2 wrapXADataSourceHybrid(final T2 delegate) throws Exception {
            final T2 wrapper = (T2) new XADataSourceHybrid(wrapDataSource(delegate), wrapXADataSource(delegate));
            ensureOnlyWrappingOnce(delegate, wrapper);
            return wrapper;
        }

        /**
         * Dekoriert eine DataSource-Instanz mit einem Wrapper der das Interface
         * {@link DataSource} zusammen mit {@link XADataSource} oder / und
         * {@link ConnectionPoolDataSource} implementiert.
         *
         * @param delegate die zugrundeliegende Instanz die gleichzeitig die Interfaces
         *                 {@link XADataSource} und {@link DataSource} implementiert.
         * @return eine DataSource-Instanz die das Interface {@link DataSource} zusammen mit
         * {@link XADataSource} oder / und {@link ConnectionPoolDataSource} implementiert.
         * @throws Exception wird ausgel&ouml;st wenn keine Wrapper-Objekte erzeugt werden k&ouml;nnen.
         * @see ConnectionPoolDataSourceHybrid
         * @see ConnectionPoolXADataSourceHybrid
         * @see XADataSourceHybrid
         */
        @SuppressWarnings("unchecked")
        public T1 wrapAutoType(final T1 delegate) throws Exception {
            if ((delegate instanceof DataSource) && (delegate instanceof XADataSource) && (delegate instanceof ConnectionPoolDataSource)) {
                return (T1) wrapConnectionPoolXADataSourceHybrid((T3) delegate);
            } else if ((delegate instanceof DataSource) && (delegate instanceof XADataSource)) {
                return (T1) wrapXADataSourceHybrid((T2) delegate);
            } else if ((delegate instanceof DataSource) && (delegate instanceof ConnectionPoolDataSource)) {
                return (T1) wrapConnectionPoolDataSourceHybrid((T4) delegate);
            } else {
                throw new IllegalArgumentException(delegate.getClass() + " is not implementing DataSource together with XADataSource or ConnectionPoolDataSource.");
            }
        }

    }
}
