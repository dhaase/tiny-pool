package eu.dirk.haase.jdbc.proxy.factory;

import eu.dirk.haase.jdbc.proxy.hybrid.ConnectionPoolDataSourceHybrid;
import eu.dirk.haase.jdbc.proxy.hybrid.ConnectionPoolXADataSourceHybrid;
import eu.dirk.haase.jdbc.proxy.hybrid.XADataSourceHybrid;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DataSourceWrapper implements Serializable {

    private final Map<Class<?>, Object> interfaceToClassMap;

    private transient volatile Constructor<ConnectionPoolDataSource> connectionPoolDataSourceConstructor;
    private transient volatile Constructor<DataSource> dataSourceConstructor;
    private transient volatile Constructor<XADataSource> xaDataSourceConstructor;

    public DataSourceWrapper(final Map<Class<?>, Object> interfaceToClassMap) throws Exception {
        this.interfaceToClassMap = Collections.unmodifiableMap(new HashMap<>(interfaceToClassMap));
        this.dataSourceConstructor = getDataSourceConstructor(null);
        this.xaDataSourceConstructor = getXADataSourceConstructor(null);
        this.connectionPoolDataSourceConstructor = getConnectionPoolDataSourceConstructor(null);
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
                throw new IllegalStateException("Can not wrap twice: " + connectionPoolDataSourceProxyClass);
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
                throw new IllegalStateException("Can not wrap twice: " + dataSourceProxyClass);
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
                throw new IllegalStateException("Can not wrap twice: " + xaDataSourceProxyClass);
            }
        }
        return null;
    }

    private Class<?> loadClass(Map<Class<?>, Object> ifaceToClassMap, final Class<?> iface) throws ClassNotFoundException {
        Object proxyClass = ifaceToClassMap.get(iface);
        if (proxyClass instanceof String) {
            return Class.forName((String) proxyClass);
        }
        return (Class<?>) proxyClass;
    }

    public ConnectionPoolDataSource wrapConnectionPoolDataSource(final ConnectionPoolDataSource delegate) throws Exception {
        return getConnectionPoolDataSourceConstructor(delegate.getClass()).newInstance(delegate);
    }

    public DataSource wrapDataSource(final DataSource delegate) throws Exception {
        return getDataSourceConstructor(delegate.getClass()).newInstance(delegate);
    }

    public XADataSource wrapXADataSource(final XADataSource delegate) throws Exception {
        return getXADataSourceConstructor(delegate.getClass()).newInstance(delegate);
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
    public class Hybrid {

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
        public <T extends ConnectionPoolDataSource & DataSource> T wrapConnectionPoolDataSourceHybrid(final T delegate) throws Exception {
            return (T) new ConnectionPoolDataSourceHybrid(wrapDataSource(delegate), wrapConnectionPoolDataSource(delegate));
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
        public <T extends ConnectionPoolDataSource & XADataSource & DataSource> T wrapConnectionPoolXADataSourceHybrid(final T delegate) throws Exception {
            return (T) new ConnectionPoolXADataSourceHybrid(wrapDataSource(delegate), wrapConnectionPoolDataSource(delegate), wrapXADataSource(delegate));
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
        public <T extends XADataSource & DataSource> T wrapXADataSourceHybrid(final T delegate) throws Exception {
            return (T) new XADataSourceHybrid(wrapDataSource(delegate), wrapXADataSource(delegate));
        }

    }
}
