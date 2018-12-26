package eu.dirk.haase.jdbc.proxy.factory;

import eu.dirk.haase.jdbc.proxy.hybrid.ConnectionPoolDataSourceHybrid;
import eu.dirk.haase.jdbc.proxy.hybrid.ConnectionPoolXADataSourceHybrid;
import eu.dirk.haase.jdbc.proxy.hybrid.XADataSourceHybrid;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.lang.reflect.Constructor;
import java.util.Map;

public class DataSourceWrapper {

    private final Map<String, Object> interfaceToClassMap;
    private Constructor<?> connectionPoolDataSourceConstructor;
    private Constructor<?> dataSourceConstructor;
    private Constructor<?> xaDataSourceConstructor;

    public DataSourceWrapper(final Map<String, Object> interfaceToClassMap) throws Exception {
        this.interfaceToClassMap = interfaceToClassMap;
        this.dataSourceConstructor = getDataSourceConstructor(null);
        this.xaDataSourceConstructor = getXADataSourceConstructor(null);
        this.connectionPoolDataSourceConstructor = getXADataSourceConstructor(null);
    }

    private Constructor<?> getConnectionPoolDataSourceConstructor(Class<?> delegateClass) throws ClassNotFoundException {
        Class<?> dataSourceClass = loadClass(ConnectionPoolDataSource.class);
        if (dataSourceClass != delegateClass) {
            if (connectionPoolDataSourceConstructor != null) {
                final Constructor<?>[] declaredConstructors = dataSourceClass.getDeclaredConstructors();
                connectionPoolDataSourceConstructor = declaredConstructors[0];
            }
            return connectionPoolDataSourceConstructor;
        } else {
            throw new IllegalStateException("Can not wrap twice: " + dataSourceClass);
        }
    }

    private Constructor<?> getDataSourceConstructor(Class<?> delegateClass) throws ClassNotFoundException {
        Class<?> dataSourceClass = loadClass(DataSource.class);
        if (dataSourceClass != delegateClass) {
            if (this.dataSourceConstructor == null) {
                final Constructor<?>[] declaredConstructors = dataSourceClass.getDeclaredConstructors();
                this.dataSourceConstructor = declaredConstructors[0];
            }
            return this.dataSourceConstructor;
        } else {
            throw new IllegalStateException("Can not wrap twice: " + dataSourceClass);
        }
    }

    private Constructor<?> getXADataSourceConstructor(Class<?> delegateClass) throws ClassNotFoundException {
        Class<?> dataSourceClass = loadClass(XADataSource.class);
        if (dataSourceClass != delegateClass) {
            if (this.xaDataSourceConstructor == null) {
                final Constructor<?>[] declaredConstructors = dataSourceClass.getDeclaredConstructors();
                this.xaDataSourceConstructor = declaredConstructors[0];
            }
            return this.xaDataSourceConstructor;
        } else {
            throw new IllegalStateException("Can not wrap twice: " + dataSourceClass);
        }
    }

    private Class<?> loadClass(final Class<?> iface) throws ClassNotFoundException {
        final Object proxyClass = interfaceToClassMap.get(iface.getName());
        if (proxyClass instanceof String) {
            return Class.forName(proxyClass.toString());
        }
        return (Class<?>) proxyClass;
    }

    public ConnectionPoolDataSource wrapConnectionPoolDataSource(final DataSource delegate) throws Exception {
        return (ConnectionPoolDataSource) getConnectionPoolDataSourceConstructor(delegate.getClass()).newInstance(delegate);
    }

    public <T extends ConnectionPoolDataSource & DataSource> T wrapConnectionPoolDataSourceHybrid(final T delegate) throws Exception {
        return (T) new ConnectionPoolDataSourceHybrid(wrapDataSource(delegate), wrapConnectionPoolDataSource(delegate));
    }

    public <T extends ConnectionPoolDataSource & XADataSource & DataSource> T wrapConnectionPoolXADataSourceHybrid(final T delegate) throws Exception {
        return (T) new ConnectionPoolXADataSourceHybrid(wrapConnectionPoolDataSourceHybrid(delegate), wrapDataSource(delegate), wrapXADataSource(delegate));
    }

    public DataSource wrapDataSource(final DataSource delegate) throws Exception {
        return (DataSource) getDataSourceConstructor(delegate.getClass()).newInstance(delegate);
    }

    public XADataSource wrapXADataSource(final DataSource delegate) throws Exception {
        return (XADataSource) getXADataSourceConstructor(delegate.getClass()).newInstance(delegate);
    }

    public <T extends XADataSource & DataSource> T wrapXADataSourceHybrid(final T delegate) throws Exception {
        return (T) new XADataSourceHybrid(wrapDataSource(delegate), wrapXADataSource(delegate));
    }

}
