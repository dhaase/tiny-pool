package eu.dirk.haase.jdbc.pool.util;

import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class WeakIdentityMapTest {

    @Test
    public void test() throws InterruptedException {
        WeakIdentityHashMap<MyKey, String> map = new WeakIdentityHashMap<>();
        map.setSoftReference(false);
        map.setEqualityByIdentity(false);

        final MyKey key1 = new MyKey();
        final MyKey key2 = new MyKey();
        map.put(new MyKey(), "hallo");
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        Thread.sleep(1000L);

        System.out.println(map.get(key2));
        System.out.println(map.size());
        System.out.println(map.getReclaimedEntryCount());
    }


}
