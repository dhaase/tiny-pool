package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class F extends AbstractDataSourceProxy {
    protected F(DataSource delegate) {
        super(delegate);
    }
}
