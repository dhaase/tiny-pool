package eu.dirk.haase.jdbc.pool.util;

import eu.dirk.haase.jdbc.proxy.base.DataSourceWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class WrapperTest {

    @Test
    public void test_dummy_datasource() throws SQLException {
        DummyDataSource dummyDataSource = new DummyDataSource(false);
        DataSource dataSource = dummyDataSource.newDataSource();
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        assertThat(connection1).isNotSameAs(connection2);
    }

    @Test
    public void test_dummy_datasource_singleton() throws SQLException {
        DummyDataSource dummyDataSource = new DummyDataSource(true);
        DataSource dataSource = dummyDataSource.newDataSource();
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        assertThat(connection1).isSameAs(connection2);
    }

    @Test
    public void test_wrapper_datasource_singleton() throws Exception {
        DummyDataSource dummyDataSource = new DummyDataSource(true);
        DataSourceWrapper dsw = new DataSourceWrapper();
        DataSource dataSource = dsw.wrapDataSource(dummyDataSource.newDataSource());
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        assertThat(connection1).isSameAs(connection2);
    }

    @Test
    public void test_wrapper_datasource() throws Exception {
        DummyDataSource dummyDataSource = new DummyDataSource(false);
        DataSourceWrapper dsw = new DataSourceWrapper();
        DataSource dataSource = dsw.wrapDataSource(dummyDataSource.newDataSource());
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        assertThat(connection1).isNotSameAs(connection2);
    }

}
