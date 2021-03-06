package eu.dirk.haase.jdbc.pool.util;

import eu.dirk.haase.jdbc.proxy.factory.DataSourceWrapperFactory;
import eu.dirk.haase.jdbc.proxy.generate.JavassistProxyFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.XAConnection;
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
        interfaceToClassMap.put(DataSource.class, "eu.dirk.haase.jdbc.proxy.WAbstractDataSourceProxy");
        interfaceToClassMap.put(XADataSource.class, "eu.dirk.haase.jdbc.proxy.WAbstractXADataSourceProxy");
        interfaceToClassMap.put(ConnectionPoolDataSource.class, "eu.dirk.haase.jdbc.proxy.WAbstractConnectionPoolDataSourceProxy");
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
        DataSourceWrapperFactory dsw = new DataSourceWrapperFactory(interfaceToClassMap);
        DataSource dataSource = dsw.wrapDataSource(dummyDataSource.newDataSource());
        Connection connection1 = dataSource.getConnection();
        Connection connection2 = dataSource.getConnection();
        assertThat(connection1).isNotSameAs(connection2);
    }

    @Test
    public void test_wrapper_datasource_singleton() throws Exception {
        DummyDataSource dummyDataSource = new DummyDataSource(true);
        DataSourceWrapperFactory dsw = new DataSourceWrapperFactory(interfaceToClassMap);
        DataSource dataSource_a = dsw.wrapDataSource(dummyDataSource.newDataSource());
        Connection connection1_a = dataSource_a.getConnection();
        Connection connection2_a = dataSource_a.getConnection();
        assertThat(connection1_a).isSameAs(connection2_a);
        connection2_a.createStatement();
    }

    @Test
    public void test_wrapper_datasource_h2() throws Exception {
        JdbcDataSource h2Ds = new JdbcDataSource();
        h2Ds.setUrl("jdbc:h2:mem:test");
        DataSourceWrapperFactory dsw = new DataSourceWrapperFactory(interfaceToClassMap);
        XADataSource dataSource_a = dsw.wrapXADataSource(h2Ds);
        XAConnection connection1_a = dataSource_a.getXAConnection();
        connection1_a.getXAResource();
    }

    @Test
    public void test_wrapper_h2() throws Exception {
        JdbcDataSource h2Ds = new JdbcDataSource();
        h2Ds.setUrl("jdbc:h2:mem:test");
        DataSourceWrapperFactory dsw = new DataSourceWrapperFactory(interfaceToClassMap);
        XADataSource dataSource_a = dsw.wrapXADataSource(h2Ds);
        XAConnection connection1_a = dataSource_a.getXAConnection();
        connection1_a.getXAResource();
    }

}
