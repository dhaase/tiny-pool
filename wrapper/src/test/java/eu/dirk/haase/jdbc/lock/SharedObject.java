package eu.dirk.haase.jdbc.lock;

public class SharedObject {


    private static final int WAITING_MILLIS = 200;

    private volatile int counter;
    private volatile int expectedModCount;
    private volatile int modificationCounter;

    public SharedObject() {
    }

    public static int getWaitingMillis() {
        return WAITING_MILLIS;
    }

    public int count() throws InterruptedException {
        modificationCounter++;
        Thread.yield();
        Thread.sleep(WAITING_MILLIS);
        counter++;
        return counter;
    }

    public int getCount() throws InterruptedException {
        Thread.yield();
        Thread.sleep(WAITING_MILLIS);
        return counter;
    }

    public boolean postProcess() throws InterruptedException {
        Thread.yield();
        Thread.sleep(WAITING_MILLIS);
        return (expectedModCount != modificationCounter);
    }

    public int preProcess() throws InterruptedException {
        expectedModCount = modificationCounter + 1;
        Thread.yield();
        Thread.sleep(WAITING_MILLIS);
        return counter;
    }

}
