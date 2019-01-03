package eu.dirk.haase.jdbc.proxy.common;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementiert einige Map-Methoden die nebenl&auml;fig ausgef&uuml;hrt werden k&ouml;nnen.
 *
 * @param <M> der generische Typ einer Map die gleichzeitig auch das Interface
 *            {@link ModificationStampingObject} implementiert.
 * @param <K> der generische Typ des Schl&uuml;ssels.
 * @param <V> der generische Typ des Wertes.
 */
public class ConcurrentMapFunktions<M extends Map<K, V> & ModificationStampingObject, K, V> {

    private static final long INVALID_STAMP = 0L;
    private static final long LOCK_TIMEOUT_SECONDS = 60L;
    private static final int RETRIES = 5;

    private final M delegate;
    private final long timeoutSeconds;

    public ConcurrentMapFunktions(final M delegate) {
        this(delegate, LOCK_TIMEOUT_SECONDS);
    }

    public ConcurrentMapFunktions(final M delegate, final long timeoutSeconds) {
        this.delegate = delegate;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Eine Implementation von {@link Map#computeIfAbsent(Object, Function)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in dieser Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * Diese Methode ist &auml;quivalent zu dem Folgenden Code implementiert:
     * <pre><code>
     * public V computeIfAbsent(final StampedLock stampedLock,
     *                          final K key,
     *                          final Function&lt;? super K, ? extends V&gt; mappingFunction) throws InterruptedException {
     *     Objects.requireNonNull(mappingFunction);
     *     V currValue;
     *     V newValue;
     *     return ((currValue = get(key)) == null &&
     *             (newValue = mappingFunction.apply(key)) != null &&
     *             (currValue = putIfAbsent(stampedLock, key, newValue)) == null) ? newValue : currValue;
     * }
     * </code></pre>
     * <p>
     * Hinweis: Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz eines Read- oder Write-Lock sein.
     *
     * @param stampedLock     die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key             der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param mappingFunction die Funktion die den Wert passend zum Schl&uuml;ssel liefert.
     * @return der Wert mit dem der Schl&uuml;ssel zugeordnet wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public V computeIfAbsent(final StampedLock stampedLock,
                             final K key,
                             final Function<? super K, ? extends V> mappingFunction) throws InterruptedException, TimeoutException {
        Objects.requireNonNull(mappingFunction);
        long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            V currValue;
            V newValue;
            return ((currValue = delegate.get(key)) == null &&
                    (newValue = mappingFunction.apply(key)) != null &&
                    (currValue = putIfAbsent(stampedLock, inOutStamp, key, newValue)) == null) ? newValue : currValue;
        } finally {
            if (inOutStamp[0] != INVALID_STAMP) {
                stampedLock.unlock(inOutStamp[0]); // Read- oder Write-Lock
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#computeIfPresent(Object, BiFunction)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in dieser Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * Hinweis: Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz eines Read- oder Write-Lock sein. Andernfalls
     *
     * @param stampedLock       die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key               der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param remappingFunction die Funktion die den Wert passend zum Schl&uuml;ssel liefert.
     * @return der alte Wert der dem Schl&uuml;ssel zugeordnet war.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public V computeIfPresent(final StampedLock stampedLock,
                              final K key,
                              final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws InterruptedException, TimeoutException {
        Objects.requireNonNull(remappingFunction);
        long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            V oldValue;
            while ((oldValue = delegate.get(key)) != null) {
                final V newValue = remappingFunction.apply(key, oldValue);
                if (newValue != null) {
                    if (replace(stampedLock, inOutStamp, key, oldValue, newValue)) {
                        return newValue;
                    }
                } else if (remove(stampedLock, inOutStamp, key, oldValue)) {
                    return null;
                }
            }
            return oldValue;
        } finally {
            if (inOutStamp[0] != INVALID_STAMP) {
                stampedLock.unlock(inOutStamp[0]); // Read- oder Write-Lock
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#putIfAbsent(Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in dieser Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * Hinweis: Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz eines Read- oder Write-Lock sein.
     *
     * @param stampedLock die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key         der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param newValue    der neue Wert die dem Schl&uuml;ssel zugeordnet werden soll wenn (noch) kein
     *                    Wert zu dem angegebenen Schl&uuml;ssel existiert.
     * @return der Wert mit dem der Schl&uuml;ssel zugeordnet wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public V putIfAbsent(final StampedLock stampedLock, K key, V newValue) throws InterruptedException, TimeoutException {
        long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            return putIfAbsent(stampedLock, inOutStamp, key, newValue);
        } finally {
            if (inOutStamp[0] != INVALID_STAMP) {
                stampedLock.unlock(inOutStamp[0]); // Read- oder Write-Lock
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#putIfAbsent(Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode wird intern von {@link WeakIdentityHashMap#putIfAbsent(StampedLock, Object, Object)}
     * aufgerufen und erwartet das bei Aufruf bereits eine Lese-Sperre gesetzt ist.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in dieser Map bereits
     * ein entsprechender Wert befindet.
     *
     * @param stampedLock die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param inOutStamp  ein Array mit einem Lock-Stempel der bei Aufruf einen Read-Stempel enthalten
     *                    muss und nach der R&uuml;ckkehr einen m&ouml;glicherweise neuen Lock-Stempel
     *                    enth&auml;lt der anschliessend freigegeben werden muss.
     * @param key         der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param newValue    der neue Wert die dem Schl&uuml;ssel zugeordnet werden soll wenn (noch) kein
     *                    Wert zu dem angegebenen Schl&uuml;ssel existiert.
     * @return der Wert mit dem der Schl&uuml;ssel zugeordnet wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    private V putIfAbsent(final StampedLock stampedLock, final long[] inOutStamp, K key, V newValue) throws InterruptedException, TimeoutException {
        V currValue = delegate.get(key);
        if (currValue == null) {
            int retry = 0;
            while (true) {
                final long writeStamp = stampedLock.tryConvertToWriteLock(inOutStamp[0]);
                if (writeStamp != INVALID_STAMP) {
                    inOutStamp[0] = writeStamp;
                    return delegate.put(key, newValue);
                } else if (retry++ >= RETRIES) {
                    // Fallback: Die Konvertierung der Lese-Sperre zu einer
                    // Schreib-Sperre hat nicht funktioniert.
                    final long expectedModStamp = delegate.modificationStamp();
                    // Werde daher einen exklusiven Schreib-Sperre anfordern.
                    // Dazu muss aber zuerst die Lese-Sperre freigegeben werden:
                    stampedLock.unlockRead(inOutStamp[0]);
                    // Hier entsteht eine Luecke und damit eine Race-Condition,
                    // da nun keine Sperre, weder Lese- noch Schreib-Sperre, gesetzt
                    // ist.
                    inOutStamp[0] = tryWriteLock(stampedLock);
                    // Die exklusive Schreib-Sperre ist jetzt gesetzt. Jetzt muss
                    // nachfolgend nochmal geprueft werden ob sich zwischenzeitlich
                    // die Map veraendert hat (wegen der Race-Condition, siehe oben):
                    if ((expectedModStamp != delegate.modificationStamp()) && ((currValue = delegate.get(key)) != null)) {
                        // Die Map hat sich zwischenzeitlich veraendert
                        // und zu dem angegebenen Schluessel gibt es bereits
                        // einen Wert:
                        return currValue;
                    }
                }
            }
        }
        return currValue;
    }

    /**
     * Eine Implementation von {@link Map#remove(Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in dieser Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * Hinweis: Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz eines Read- oder Write-Lock sein.
     *
     * @param stampedLock die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key         der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param value       der Wert der dem Schl&uuml;ssel in dieser Map zugeordnet sein soll.
     * @return {@code true} wenn der Eintrag entfernt wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public boolean remove(final StampedLock stampedLock, Object key, Object value) throws InterruptedException, TimeoutException {
        long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            return remove(stampedLock, inOutStamp, key, value);
        } finally {
            if (inOutStamp[0] != INVALID_STAMP) {
                stampedLock.unlock(inOutStamp[0]); // Read- oder Write-Lock
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#remove(Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode wird intern von {@link WeakIdentityHashMap#remove(StampedLock, Object, Object)}
     * aufgerufen und erwartet das bei Aufruf bereits eine Lese-Sperre gesetzt ist.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in dieser Map bereits
     * ein entsprechender Wert befindet.
     *
     * @param stampedLock die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param inOutStamp  ein Array mit einem Lock-Stempel der bei Aufruf einen Read-Stempel enthalten
     *                    muss und nach der R&uuml;ckkehr einen m&ouml;glicherweise neuen Lock-Stempel
     *                    enth&auml;lt der anschliessend freigegeben werden muss.
     * @param key         der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param value       der Wert der dem Schl&uuml;ssel in dieser Map zugeordnet sein soll.
     * @return {@code true} wenn der Eintrag entfernt wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    private boolean remove(final StampedLock stampedLock, final long[] inOutStamp, final Object key, final Object value) throws InterruptedException, TimeoutException {
        Object currValue = delegate.get(key);
        if (!Objects.equals(currValue, value) ||
                (currValue == null && !delegate.containsKey(key))) {
            return false;
        }
        int retry = 0;
        while (true) {
            final long writeStamp = stampedLock.tryConvertToWriteLock(inOutStamp[0]);
            if (writeStamp != INVALID_STAMP) {
                inOutStamp[0] = writeStamp;
                delegate.remove(key);
                return true;
            } else if (retry++ >= RETRIES) {
                // Fallback: Die Konvertierung der Lese-Sperre zu einer
                // Schreib-Sperre hat nicht funktioniert.
                long expectedModStamp = delegate.modificationStamp();
                // Werde daher einen exklusiven Schreib-Sperre anfordern.
                // Dazu muss aber zuerst die Lese-Sperre freigegeben werden:
                stampedLock.unlockRead(inOutStamp[0]);
                // Hier entsteht eine Luecke und damit eine Race-Condition,
                // da nun keine Sperre, weder Lese- noch Schreib-Sperre, gesetzt
                // ist.
                inOutStamp[0] = tryWriteLock(stampedLock);
                // Die exklusive Schreib-Sperre ist jetzt gesetzt. Jetzt muss
                // nachfolgend nochmal geprueft werden ob sich zwischenzeitlich
                // die Map veraendert hat (wegen der Race-Condition, siehe oben):
                if ((expectedModStamp != delegate.modificationStamp())) {
                    // Die Map hat sich zwischenzeitlich veraendert
                    // und zu dem angegebenen Schluessel gibt es bereits
                    // einen Wert:
                    currValue = delegate.get(key);
                    if (!Objects.equals(currValue, value) ||
                            (currValue == null && !delegate.containsKey(key))) {
                        return false;
                    }
                }
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#replace(Object, Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in dieser Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * Hinweis: Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz eines Read- oder Write-Lock sein.
     *
     * @param stampedLock der Lock mit dem die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key         der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param oldValue    der alte Wert der dem Schl&uuml;ssel bereits zugeordnet sein soll.
     * @param newValue    der neue Wert die dem Schl&uuml;ssel zugeordnet werden soll wenn entweder
     *                    der alte Wert {@code null} ist oder sich von dem neuen Wert unterscheidet.
     * @return {@code true} wenn der alte Wert ersetzt wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public boolean replace(final StampedLock stampedLock, K key, V oldValue, V newValue) throws InterruptedException, TimeoutException {
        long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            return replace(stampedLock, inOutStamp, key, oldValue, newValue);
        } finally {
            if (inOutStamp[0] != INVALID_STAMP) {
                stampedLock.unlock(inOutStamp[0]); // Read- oder Write-Lock
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#replace(Object, Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode wird intern von {@link WeakIdentityHashMap#replace(StampedLock, Object, Object, Object)}
     * aufgerufen und erwartet das bei Aufruf bereits eine Lese-Sperre gesetzt ist.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in dieser Map bereits
     * ein entsprechender Wert befindet.
     *
     * @param stampedLock der Lock mit dem die Schreib-/ Lese-Synchronisation erfolgt.
     * @param inOutStamp  ein Array mit einem Sperren-Stempel der bei Aufruf einen Lese-Stempel enthalten
     *                    muss und nach der R&uuml;ckkehr einen m&ouml;glicherweise neuen Sperren-Stempel
     *                    enth&auml;lt der anschliessend wieder freigegeben werden muss.
     * @param key         der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param oldValue    der alte Wert der dem Schl&uuml;ssel bereits zugeordnet sein soll.
     * @param newValue    der neue Wert die dem Schl&uuml;ssel zugeordnet werden soll wenn entweder
     *                    der alte Wert {@code null} ist oder sich von dem neuen Wert unterscheidet.
     * @return {@code true} wenn der alte Wert ersetzt wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    private boolean replace(final StampedLock stampedLock, final long[] inOutStamp, K key, V oldValue, V newValue) throws InterruptedException, TimeoutException {
        V currValue = delegate.get(key);
        if (!Objects.equals(currValue, oldValue) ||
                (currValue == null && !delegate.containsKey(key))) {
            return false;
        } else {
            int retry = 0;
            while (true) {
                final long writeStamp = stampedLock.tryConvertToWriteLock(inOutStamp[0]);
                if (writeStamp != INVALID_STAMP) {
                    inOutStamp[0] = writeStamp;
                    delegate.put(key, newValue);
                    return true;
                } else if (retry++ >= RETRIES) {
                    // Fallback: Die Konvertierung der Lese-Sperre zu einer
                    // Schreib-Sperre hat nicht funktioniert.
                    long expectedModStamp = delegate.modificationStamp();
                    // Werde daher einen exklusiven Schreib-Sperre anfordern.
                    // Dazu muss aber zuerst die Lese-Sperre freigegeben werden:
                    stampedLock.unlockRead(inOutStamp[0]);
                    // Hier entsteht eine Luecke und damit eine Race-Condition,
                    // da nun keine Sperre, weder Lese- noch Schreib-Sperre, gesetzt
                    // ist.
                    inOutStamp[0] = tryWriteLock(stampedLock);
                    // Die exklusive Schreib-Sperre ist jetzt gesetzt. Jetzt muss
                    // nachfolgend nochmal geprueft werden ob sich zwischenzeitlich
                    // die Map veraendert hat (wegen der Race-Condition, siehe oben):
                    if ((expectedModStamp != delegate.modificationStamp())) {
                        // Die Map hat sich zwischenzeitlich veraendert
                        // und zu dem angegebenen Schluessel gibt es bereits
                        // einen Wert:
                        currValue = delegate.get(key);
                        if (!Objects.equals(currValue, oldValue) ||
                                (currValue == null && !delegate.containsKey(key))) {
                            return false;
                        }
                    }
                }
            }
        }
    }

    /**
     * Fordert eine Lese-Sperre an, wenn sie in gegebener Zeit verf&uuml;gbar
     * ist und der aktuelle Thread nicht unterbrochen wurde.
     *
     * @param stampedLock der Lock mit dem der Lese-Lock angefordert werden soll.
     * @return des Stempelwertes f&uuml;r eine Lese-Sperre.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()}
     *                              unterbrochen wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit
     *                              angefordert werden.
     */
    private long tryReadLock(final StampedLock stampedLock) throws InterruptedException, TimeoutException {
        long readStamp = stampedLock.tryReadLock(timeoutSeconds, TimeUnit.SECONDS);
        if (readStamp == INVALID_STAMP) {
            throw new TimeoutException("Unable to acquire a read lock within " + timeoutSeconds + " seconds.");
        }
        return readStamp;
    }

    /**
     * Fordert exklusiv eine Schreib-Sperre an, wenn sie in gegebener Zeit verf&uuml;gbar
     * ist und der aktuelle Thread nicht unterbrochen wurde.
     *
     * @param stampedLock der Sperre mit dem der Schreib-Sperre angefordert werden soll.
     * @return des Stempelwertes f&uuml;r eine Schreib-Sperre.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()}
     *                              unterbrochen wurde.
     * @throws TimeoutException     die Sperre konnte nicht rechtzeitig in vorgegebener Zeit
     *                              angefordert werden.
     */
    private long tryWriteLock(final StampedLock stampedLock) throws InterruptedException, TimeoutException {
        long writeStamp = stampedLock.tryWriteLock(timeoutSeconds, TimeUnit.SECONDS);
        if (writeStamp == INVALID_STAMP) {
            throw new TimeoutException("Unable to acquire a write lock within " + timeoutSeconds + " seconds.");
        }
        return writeStamp;
    }

}
