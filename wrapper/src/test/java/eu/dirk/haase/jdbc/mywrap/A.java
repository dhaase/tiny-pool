package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class A extends AbstractDataSourceProxy {
    protected A(DataSource delegate) {
        super(delegate);
    }
}
