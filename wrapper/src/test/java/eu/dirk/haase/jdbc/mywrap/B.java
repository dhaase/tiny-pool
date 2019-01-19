package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class B extends AbstractDataSourceProxy {
    protected B(DataSource delegate) {
        super(delegate);
    }
}
