package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class D extends AbstractDataSourceProxy {
    protected D(DataSource delegate) {
        super(delegate);
    }
}
