package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;

import javax.sql.XADataSource;

public abstract class XADataSourceProxy extends ConcurrentFactoryJdbcProxy<XADataSource> {


    protected XADataSourceProxy(final XADataSource delegate) {
        super(delegate);
    }

}
