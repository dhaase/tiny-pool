package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.lang.reflect.Constructor;
import java.util.function.BiFunction;

public class DataSourceWrapper {

    private static final String prefix = "W";
    private static final BiFunction<String, Class<?>, String> CLASS_NAME_FUN = (cn, iface) -> cn.replaceAll("(.+)\\.(\\w+)", "$1." + prefix + "$2");

    private final BiFunction<String, Class<?>, String> classNameFun;
    private Constructor<?> connectionPoolDataSourceConstructor;
    private Constructor<?> dataSourceConstructor;
    private Constructor<?> xaDataSourceConstructor;

    public DataSourceWrapper() throws Exception {
        this.classNameFun = CLASS_NAME_FUN;
        this.dataSourceConstructor = getDataSourceConstructor();
        this.xaDataSourceConstructor = getXADataSourceConstructor();
        this.connectionPoolDataSourceConstructor = getXADataSourceConstructor();
    }

    private Constructor<?> getConnectionPoolDataSourceConstructor() throws ClassNotFoundException {
        if (connectionPoolDataSourceConstructor != null) {
            final String newClassName = classNameFun.apply(DataSourceProxy.class.getName(), ConnectionPoolDataSource.class);
            Class<?> dataSourceClass = Class.forName(newClassName);
            final Constructor<?>[] declaredConstructors = dataSourceClass.getDeclaredConstructors();
            connectionPoolDataSourceConstructor = declaredConstructors[0];
        }
        return connectionPoolDataSourceConstructor;
    }

    private Constructor<?> getDataSourceConstructor() throws ClassNotFoundException {
        if (this.dataSourceConstructor == null) {
            final String newClassName = classNameFun.apply(DataSourceProxy.class.getName(), DataSource.class);
            Class<?> dataSourceClass = Class.forName(newClassName);
            final Constructor<?>[] declaredConstructors = dataSourceClass.getDeclaredConstructors();
            this.dataSourceConstructor = declaredConstructors[0];
        }
        return this.dataSourceConstructor;
    }

    private Constructor<?> getXADataSourceConstructor() throws ClassNotFoundException {
        if (this.xaDataSourceConstructor == null) {
            final String newClassName = classNameFun.apply(DataSourceProxy.class.getName(), XADataSource.class);
            Class<?> dataSourceClass = Class.forName(newClassName);
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
