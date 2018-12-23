package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.XADataSource;
import java.sql.SQLException;

public abstract class XADataSourceProxy extends JdbcProxy<XADataSource> {


    protected XADataSourceProxy(final XADataSource delegate) {
        super(delegate);
    }

}
