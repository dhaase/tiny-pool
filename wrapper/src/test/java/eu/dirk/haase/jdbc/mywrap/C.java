package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class C extends AbstractDataSourceProxy {
    protected C(DataSource delegate) {
        super(delegate);
    }
}
