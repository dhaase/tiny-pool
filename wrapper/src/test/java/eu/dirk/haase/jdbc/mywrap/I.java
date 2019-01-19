package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class I extends AbstractDataSourceProxy {
    protected I(DataSource delegate) {
        super(delegate);
    }
}
