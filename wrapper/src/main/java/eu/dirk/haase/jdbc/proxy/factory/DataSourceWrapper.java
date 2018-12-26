package eu.dirk.haase.jdbc.proxy.factory;

import eu.dirk.haase.jdbc.proxy.base.*;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.BiFunction;

public class DataSourceWrapper {

    private Constructor<?> connectionPoolDataSourceConstructor;
    private Constructor<?> dataSourceConstructor;
    private Constructor<?> xaDataSourceConstructor;

    private final Map<String, String> interfaceToClassMap;

    public DataSourceWrapper(final Map<String, String> interfaceToClassMap) throws Exception {
        this.interfaceToClassMap = interfaceToClassMap;
        this.dataSourceConstructor = getDataSourceConstructor();
        this.xaDataSourceConstructor = getXADataSourceConstructor();
        this.connectionPoolDataSourceConstructor = getXADataSourceConstructor();
    }

    private Constructor<?> getConnectionPoolDataSourceConstructor() throws ClassNotFoundException {
        if (connectionPoolDataSourceConstructor != null) {
            Class<?> dataSourceClass = loadClass(ConnectionPoolDataSource.class);
            final Constructor<?>[] declaredConstructors = dataSourceClass.getDeclaredConstructors();
            connectionPoolDataSourceConstructor = declaredConstructors[0];
        }
        return connectionPoolDataSourceConstructor;
    }

    private Class<?> loadClass(final Class<?> iface) throws ClassNotFoundException {
        final String className = interfaceToClassMap.get(iface.getName());
        return Class.forName(className);
    }

    private Constructor<?> getDataSourceConstructor() throws ClassNotFoundException {
        if (this.dataSourceConstructor == null) {
            Class<?> dataSourceClass = loadClass(DataSource.class);
            final Constructor<?>[] declaredConstructors = dataSourceClass.getDeclaredConstructors();
            this.dataSourceConstructor = declaredConstructors[0];
        }
        return this.dataSourceConstructor;
    }

    private Constructor<?> getXADataSourceConstructor() throws ClassNotFoundException {
        if (this.xaDataSourceConstructor == null) {
            Class<?> dataSourceClass = loadClass(XADataSource.class);
            final Constructor<?>[] declaredConstructors = dataSourceClass.getDeclaredConstructors();
            this.xaDataSourceConstructor = declaredConstructors[0];
        }
        return this.xaDataSourceConstructor;
    }

    public ConnectionPoolDataSource wrapConnectionPoolDataSource(final DataSource delegate) throws Exception {
        return (ConnectionPoolDataSource) getConnectionPoolDataSourceConstructor().newInstance(delegate);
    }

    public <T extends ConnectionPoolDataSource & DataSource> T wrapConnectionPoolDataSourceHybrid(final T delegate) throws Exception {
        return (T) new ConnectionPoolDataSourceHybrid(wrapDataSource(delegate), wrapConnectionPoolDataSource(delegate));
    }

    public <T extends ConnectionPoolDataSource & XADataSource & DataSource> T wrapConnectionPoolXADataSourceHybrid(final T delegate) throws Exception {
        return (T) new ConnectionPoolXADataSourceHybrid(wrapConnectionPoolDataSourceHybrid(delegate), wrapDataSource(delegate), wrapXADataSource(delegate));
    }

    public DataSource wrapDataSource(final DataSource delegate) throws Exception {
        return (DataSource) getDataSourceConstructor().newInstance(delegate);
    }

    public XADataSource wrapXADataSource(final DataSource delegate) throws Exception {
        return (XADataSource) getXADataSourceConstructor().newInstance(delegate);
    }

    public <T extends XADataSource & DataSource> T wrapXADataSourceHybrid(final T delegate) throws Exception {
        return (T) new XADataSourceHybrid(wrapDataSource(delegate), wrapXADataSource(delegate));
    }

}