package eu.dirk.haase.jdbc.pool.util;

import eu.dirk.haase.jdbc.proxy.factory.DataSourceWrapper;
import eu.dirk.haase.jdbc.proxy.generate.JavassistProxyFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class WrapperTest {

    private final Map<Class<?>, Object> interfaceToClassMap = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        JavassistProxyFactory.main();
        interfaceToClassMap.put(DataSource.class, "eu.dirk.haase.jdbc.proxy.WDataSourceProxy");
        interfaceToClassMap.put(XADataSource.class, "eu.dirk.haase.jdbc.proxy.WXADataSourceProxy");
        interfaceToClassMap.put(ConnectionPoolDataSource.class, "eu.dirk.haase.jdbc.proxy.WConnectionPoolDataSourceProxy");
    }

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
    public void test_wrapper_datasource() throws Exception {
        DummyDataSource dummyDataSource = new DummyDataSource(false);
        DataSourceWrapper dsw = new DataSourceWrapper(interfaceToClassMap);
        DataSource dataSource = dsw.wrapDataSource(dummyDataSource.newDataSource());
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        assertThat(connection1).isNotSameAs(connection2);
    }

    @Test
    public void test_wrapper_datasource_singleton() throws Exception {
        DummyDataSource dummyDataSource = new DummyDataSource(true);
        DataSourceWrapper dsw = new DataSourceWrapper(interfaceToClassMap);
        DataSource dataSource_a = dsw.wrapDataSource(dummyDataSource.newDataSource());
        Connection connection1_a = dataSource_a.getConnection();
        Connection connection2_a = dataSource_a.getConnection();
        assertThat(connection1_a).isSameAs(connection2_a);
//        DataSource dataSource_b = dsw.wrapDataSource(dataSource_a);
//        Connection connection1_b = dataSource_b.getConnection();
//        Connection connection2_b = dataSource_b.getConnection();
//        assertThat(connection1_b).isSameAs(connection2_b);
    }

}
