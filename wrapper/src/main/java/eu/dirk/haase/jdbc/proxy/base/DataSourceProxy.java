package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.DataSource;

public abstract class DataSourceProxy extends JdbcProxy<DataSource> {


    protected DataSourceProxy(final DataSource delegate) {
        super(delegate);
    }

}
