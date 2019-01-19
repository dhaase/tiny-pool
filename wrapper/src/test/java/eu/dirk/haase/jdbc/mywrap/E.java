package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class E extends AbstractDataSourceProxy {
    protected E(DataSource delegate) {
        super(delegate);
    }
}
