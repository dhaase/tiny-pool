package eu.dirk.haase.jdbc.pool.util;

import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.function.BiConsumer;

@RunWith(BlockJUnit4ClassRunner.class)
public class WeakIdentityMapTest {

    @Test
    public void test_object() throws InterruptedException {
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
        Thread.sleep(1500L);

        System.out.println(map.purge());
        System.out.println(map.get(key2));
        System.out.println(map.size());
        System.out.println(map.getReclaimedEntryCount());
    }

    @Test
    public void test_thread() throws InterruptedException {
        WeakIdentityHashMap<Thread, String> map = new WeakIdentityHashMap<>();
        map.setSoftReference(false);
        map.setEqualityByIdentity(false);

        map.put(new Thread(new MyRunnable(map)), "hallo");
        map.forEach((t, s) -> t.start());
        map.forEach((t, s) -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        Thread.sleep(1500L);

        System.out.println("0. final: " + map.purge());
        System.out.println("1. final: " + map.size());
        System.out.println("2. final: " + map.getReclaimedEntryCount());
    }

    static class MyComputeFun implements BiConsumer<Thread, String> {

        @Override
        public void accept(Thread thread, String s) {
            thread.start();
        }
    }

    static class MyRunnable implements Runnable {

        private WeakIdentityHashMap<Thread, String> map;

        MyRunnable(WeakIdentityHashMap<Thread, String> map) {
            this.map = map;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1500L);
                System.out.println("0. start: " + map.purge());
                System.out.println("1. start: " + map.size());
                System.out.println("2. start: " + map.getReclaimedEntryCount());
                System.gc();
                System.gc();
                System.gc();
                System.gc();
                System.gc();
                System.gc();
                Thread.sleep(1500L);
                System.out.println("0. end: " + map.purge());
                System.out.println("1. end: " + map.size());
                System.out.println("2. end: " + map.getReclaimedEntryCount());
            } catch (Exception e) {
                // ignore
            }
        }

    }
}
