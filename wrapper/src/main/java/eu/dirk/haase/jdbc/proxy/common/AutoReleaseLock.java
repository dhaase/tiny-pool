package eu.dirk.haase.jdbc.proxy.common;

import java.util.concurrent.locks.Lock;

public interface AutoReleaseLock extends Lock, AutoCloseable {
    void close();
}
