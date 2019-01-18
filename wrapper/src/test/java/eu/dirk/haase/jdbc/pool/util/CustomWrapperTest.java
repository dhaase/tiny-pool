package eu.dirk.haase.jdbc.pool.util;

import eu.dirk.haase.jdbc.mywrap.MyWrapConnection;
import eu.dirk.haase.jdbc.mywrap.MyWrapDataSource;
import eu.dirk.haase.jdbc.proxy.factory.DataSourceWrapper;
import eu.dirk.haase.jdbc.proxy.generate.Generator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class CustomWrapperTest {

    private Map<Class<?>, Object> interfaceToClassMap;

    @Before
    public void setUp() {
        Generator generator = new Generator();
        final Map<Class<?>, Class<?>> iface2CustomClassMap = new HashMap<>();
        iface2CustomClassMap.put(DataSource.class, MyWrapDataSource.class);
        iface2CustomClassMap.put(Connection.class, MyWrapConnection.class);
        interfaceToClassMap = generator.generate(iface2CustomClassMap);
        interfaceToClassMap = generator.generate(iface2CustomClassMap);
    }

    @Test
    public void test_wrapper_datasource() throws Exception {
        DummyDataSource dummyDataSource = new DummyDataSource(true);
        DataSourceWrapper dsw = new DataSourceWrapper(interfaceToClassMap);
        DataSource dataSource_a = dsw.wrapDataSource(dummyDataSource.newDataSource());
        Connection connection1_a = dataSource_a.getConnection();
        assertThat(connection1_a).isInstanceOf(MyWrapConnection.class);
    }

}
