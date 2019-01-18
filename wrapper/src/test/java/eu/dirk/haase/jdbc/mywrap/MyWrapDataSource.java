package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class MyWrapDataSource extends AbstractDataSourceProxy implements DataSource {
    protected MyWrapDataSource(DataSource delegate) {
        super(delegate);
    }
}
