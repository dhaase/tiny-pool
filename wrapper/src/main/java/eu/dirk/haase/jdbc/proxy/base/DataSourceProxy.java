package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.DataSource;
import java.sql.SQLException;

public abstract class DataSourceProxy extends JdbcProxy<DataSource> {


    protected DataSourceProxy(final DataSource delegate) {
        super(delegate);
    }

}
