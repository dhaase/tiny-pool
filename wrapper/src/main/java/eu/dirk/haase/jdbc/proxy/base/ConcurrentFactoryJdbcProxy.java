package eu.dirk.haase.jdbc.proxy.base;

import javax.sql.*;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Basis-Klasse f&uuml;r JDBC-Klassen die eingepackt werden sollen und
 * selbst wiederum Factory-Methoden f&uuml;r weitere JDBC-Klassen bieten
 * die (auch) nebenl&auml;fig aufgerufen werden k&ouml;nnen.
 * <p>
 * Zu den JDBC-Klassen mit Factory-Methoden, die nebenl&auml;fig aufgerufen
 * werden k&ouml;nnen, geh&ouml;ren alle Klasse die nicht mittelbar oder
 * unmittelbar mit einer {@link Connection}-Instanz verbunden sind:
 * <ol>
 * <li>{@link ConnectionPoolDataSource}</li>
 * <li>{@link DataSource}</li>
 * <li>{@link PooledConnection}</li>
 * <li>{@link XADataSource}</li>
 * <li>{@link XAConnection}</li>
 * <li>{@link XAResource}</li>
 * </ol>
 *
 * @param <T1> der Typ der jeweiligen abgeleiteten JDBC-Klasse.
 */
public abstract class ConcurrentFactoryJdbcProxy<T1> extends FactoryJdbcProxy<T1> {

    private static final int WAITING_SECONDS = 10;
    private final Lock lock;

    protected ConcurrentFactoryJdbcProxy(T1 delegate) {
        super(delegate);
        this.lock = new ReentrantLock(true);
    }

    protected final <T2> T2 wrapConcurrent(T2 delegate, Function<T2, T2> objectMaker) {
        try {
            if (this.lock.tryLock(WAITING_SECONDS, TimeUnit.SECONDS)) {
                return wrap(delegate, objectMaker);
            } else {
                throw new IllegalStateException("Waiting time of " + WAITING_SECONDS + " sec is elapsed before the lock was acquired in class " + getClass());
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while acquiring the lock in class " + getClass(), e);
        } finally {
            this.lock.unlock();
        }
    }

}
