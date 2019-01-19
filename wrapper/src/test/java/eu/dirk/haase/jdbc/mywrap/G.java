package eu.dirk.haase.jdbc.mywrap;

import eu.dirk.haase.jdbc.proxy.AbstractDataSourceProxy;

import javax.sql.DataSource;

public abstract class G extends AbstractDataSourceProxy {
    protected G(DataSource delegate) {
        super(delegate);
    }
}
