package eu.dirk.haase.jdbc.pool.util;

import eu.dirk.haase.jdbc.mywrap.*;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class CustomWrapperTest {

    private Map<Class<?>, Object> interfaceToClassMap;
    private Generator generator = new Generator();

    @Before
    public void setUp() {
        final Map<Class<?>, Class<?>> iface2CustomClassMap = newClassClassMap(MyWrapDataSource.class, MyWrapConnection.class);
        interfaceToClassMap = generator.generate(iface2CustomClassMap);
        interfaceToClassMap = generator.generate(iface2CustomClassMap);
    }

    private Map<Class<?>, Class<?>> newClassClassMap(Class<?> dataSource, Class<?> connection) {
        final Map<Class<?>, Class<?>> iface2CustomClassMap = new HashMap<>();
        iface2CustomClassMap.put(DataSource.class, dataSource);
        iface2CustomClassMap.put(Connection.class, connection);
        return iface2CustomClassMap;
    }

    @Test
    public void test_wrapper_datasource() throws Exception {
        DummyDataSource dummyDataSource = new DummyDataSource(true);
        DataSourceWrapper dsw = new DataSourceWrapper(interfaceToClassMap);
        DataSource dataSource_a = dsw.wrapDataSource(dummyDataSource.newDataSource());
        Connection connection1_a = dataSource_a.getConnection();
        assertThat(connection1_a).isInstanceOf(MyWrapConnection.class);
    }

    @Test
    public void test_wrapper_10_levels() throws Exception {
        DummyDataSource dummyDataSource = new DummyDataSource(true);
        DataSource bottom = createDataSource(MyWrapDataSource.class, MyWrapConnection.class, dummyDataSource.newDataSource());
        DataSource a = createDataSource(A.class, AA.class, bottom);
        DataSource b = createDataSource(B.class, BB.class, a);
        DataSource c = createDataSource(C.class, CC.class, b);
        DataSource d = createDataSource(D.class, DD.class, c);
        DataSource e = createDataSource(E.class, EE.class, d);
        DataSource f = createDataSource(F.class, FF.class, e);
        DataSource g = createDataSource(G.class, GG.class, f);
        DataSource h = createDataSource(H.class, HH.class, g);
        DataSource i = createDataSource(I.class, II.class, h);
        DataSource j = createDataSource(J.class, JJ.class, i);
        DataSource k = createDataSource(K.class, KK.class, j);

        Connection connection = k.getConnection();
        assertThat(connection).isInstanceOf(KK.class);

        assertThat(connection.unwrap(JJ.class)).isInstanceOf(JJ.class);
        assertThat(connection.unwrap(II.class)).isInstanceOf(II.class);
        assertThat(connection.unwrap(HH.class)).isInstanceOf(HH.class);
        assertThat(connection.unwrap(GG.class)).isInstanceOf(GG.class);
        assertThat(connection.unwrap(FF.class)).isInstanceOf(FF.class);
        assertThat(connection.unwrap(EE.class)).isInstanceOf(EE.class);
        assertThat(connection.unwrap(DD.class)).isInstanceOf(DD.class);
        assertThat(connection.unwrap(CC.class)).isInstanceOf(CC.class);
        assertThat(connection.unwrap(BB.class)).isInstanceOf(BB.class);
        assertThat(connection.unwrap(AA.class)).isInstanceOf(AA.class);

        long start = System.nanoTime();
        for (int o = 0; 1000 > o; ++o) {
            //final String endStr = connection.nativeSQL("");
            k.getConnection();
        }
        long end = System.nanoTime();
        System.out.println("duration: " + TimeUnit.NANOSECONDS.toMillis(end - start) + "ms");
        System.out.println(k.getClass().getProtectionDomain().getCodeSource().getLocation());
    }

    private DataSource createDataSource(Class<?> dataSourceClass, Class<?> connectionClass, DataSource dataSource) throws Exception {
        final Map<Class<?>, Class<?>> iface2CustomClassMap = newClassClassMap(dataSourceClass, connectionClass);
        final Map<Class<?>, Object> ifaceToClassMap = generator.generate(iface2CustomClassMap);
        DataSourceWrapper dsw = new DataSourceWrapper(ifaceToClassMap);
        return dsw.wrapDataSource(dataSource);
    }
}
