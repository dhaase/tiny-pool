package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class H extends AbstractDataSourceProxy {
    protected H(DataSource delegate) {
        super(delegate);
    }
}
