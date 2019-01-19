package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class K extends AbstractDataSourceProxy {
    protected K(DataSource delegate) {
        super(delegate);
    }
}
