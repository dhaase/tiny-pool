package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class J extends AbstractDataSourceProxy {
    protected J(DataSource delegate) {
        super(delegate);
    }
}
