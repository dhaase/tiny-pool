package eu.dirk.haase.jdbc.proxy.common;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.StampedLock;
import java.util.function.*;

/**
 * Implementiert einige Map-Methoden, wie zum Beispiel
 * {@link Map#computeIfAbsent(Object, Function)},
 * um sie nebenl&auml;ufig ausf&uuml;hren zu k&ouml;nnen.
 * <p>
 * Hinweis: Auch wenn diese Methoden im nebenl&auml;ufigen
 * Kontext mit einer {@link HashMap} fehlerfrei funktionieren,
 * ist es unter normalen Umst&auml;nden wesentlich performanter
 * eine {@link ConcurrentHashMap} einzusetzen.
 * <p>
 * Diese Methoden sind insbesondere f&uuml;r spezielle
 * {@link Map}-Implementationen gedacht (zum Beispiel
 * {@link IdentityHashMap} oder {@link WeakHashMap}), f&uuml;r
 * die es keine Implementationen gibt, die nebenl&auml;ufig
 * ausgef&uuml;hrt werden k&ouml;nnen.
 *
 * @param <M> der generische Typ einer Map die gleichzeitig auch das Interface
 *            {@link ModificationStampingObject} implementiert.
 * @param <K> der generische Typ des Schl&uuml;ssels.
 * @param <V> der generische Typ des Wertes.
 * @see Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
 * @see Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
 * @see Map#putIfAbsent(java.lang.Object, java.lang.Object)
 * @see Map#remove(java.lang.Object, java.lang.Object)
 * @see Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
 * @see Map#replace(java.lang.Object, java.lang.Object)
 * @see Map#replaceAll(java.util.function.BiFunction)
 * @see Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
 */
public class ConcurrentMapFunktions<M extends Map<K, V> & ModificationStampingObject, K, V> {

    private static final long INVALID_STAMP = 0L;
    private static final long LOCK_TIMEOUT_SECONDS = 60L;
    private static final int RETRIES = 5;

    private final M delegate;
    private final long timeoutSeconds;

    /**
     * Erzeugt ein neues {@link ConcurrentMapFunktions}-Object.
     *
     * @param delegate das {@link Map}-Objekt f&uuml;r das die Methoden
     *                 nebenl&auml;ufig ausgef&uuml;hrt werden sollen.
     */
    public ConcurrentMapFunktions(final M delegate) {
        this(delegate, LOCK_TIMEOUT_SECONDS);
    }

    /**
     * Erzeugt ein neues {@link ConcurrentMapFunktions}-Object.
     *
     * @param delegate       das {@link Map}-Objekt f&uuml;r das die Methoden
     *                       nebenl&auml;ufig ausgef&uuml;hrt werden sollen.
     * @param timeoutSeconds Anzahl der Sekunden die maximal bei der Anforderung
     *                       einer Sperre gewartet werden soll.
     */
    public ConcurrentMapFunktions(final M delegate, final long timeoutSeconds) {
        this.delegate = delegate;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Eine Implementation von {@link Map#compute(Object, BiFunction)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>1. Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
     * <p>
     * <b>2. Hinweis:</b> Die Mapping-Function (der dritte Parameter) wird vor paralleler Ausf&uuml;hrung
     * nicht gesch&uuml;tzt. Es ist daher m&ouml;glich, das die Mapping-Function gleichzeitig von
     * mehreren Threads ausgef&uuml;hrt wird.
     *
     * @param stampedLock       die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key               der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param remappingFunction die Funktion die den Wert passend zum Schl&uuml;ssel liefert.
     * @return der alte Wert der dem Schl&uuml;ssel zugeordnet war.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public V compute(final StampedLock stampedLock,
                     final K key,
                     final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws TimeoutException, InterruptedException {
        Objects.requireNonNull(remappingFunction);
        final long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            V oldValue = delegate.get(key);
            while (true) {
                final V newValue = remappingFunction.apply(key, oldValue);
                if (newValue == null) {
                    // delete mapping
                    if (oldValue != null || delegate.containsKey(key)) {
                        // something to remove
                        if (remove(stampedLock, inOutStamp, key, oldValue)) {
                            // removed the old value as expected
                            return null;
                        }
                        // some other value replaced old value. try again.
                        oldValue = delegate.get(key);
                    } else {
                        // nothing to do. Leave things as they were.
                        return null;
                    }
                } else {
                    // add or replace old mapping
                    if (oldValue != null) {
                        // replace
                        if (replace(stampedLock, inOutStamp, key, oldValue, newValue)) {
                            // replaced as expected.
                            return newValue;
                        }
                        // some other value replaced old value. try again.
                        oldValue = delegate.get(key);
                    } else {
                        // add (replace if oldValue was null)
                        if ((oldValue = computeIfAbsent(stampedLock, inOutStamp, key, (k) -> newValue, (v) -> true)) == null) {
                            // replaced
                            return newValue;
                        }
                        // some other value replaced old value. try again.
                    }
                }
            }
        } finally {
            if (inOutStamp[0] != INVALID_STAMP) {
                stampedLock.unlock(inOutStamp[0]); // Read- oder Write-Lock
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#computeIfAbsent(Object, Function)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
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
     * <b>1. Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
     * <p>
     * <b>2. Hinweis:</b> Die Mapping-Function (der dritte Parameter) wird nur dann ausgef&uuml;hrt wenn
     * eine exklusive Schreibsperre gesetzt werden konnte. Es ist daher sichergestellt, das die
     * Mapping-Function gleichzeitig stets nur von einem Threads ausgef&uuml;hrt wird.
     *
     * @param stampedLock     die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key             der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param mappingFunction die Funktion die den Wert passend zum Schl&uuml;ssel liefert.
     * @return der Wert mit dem der Schl&uuml;ssel zugeordnet wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht re chtzeitig in vorgegebener Zeit angefordert werden.
     */
    public V computeIfAbsent(final StampedLock stampedLock,
                             final K key,
                             final Function<? super K, ? extends V> mappingFunction) throws InterruptedException, TimeoutException {
        Objects.requireNonNull(mappingFunction);
        final long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            final V currValue = delegate.get(key);
            final Predicate<V> mapCondition = (v) -> v != null;
            return (currValue == null ? computeIfAbsent(stampedLock, inOutStamp, key, mappingFunction, mapCondition) : currValue);
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
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
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
        final long[] inOutStamp = {INVALID_STAMP};
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
     * Eine Implementation von {@link Map#merge(Object, Object, BiFunction)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
     *
     * @param stampedLock       die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key               der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param remappingFunction die Funktion die den Wert passend zum Schl&uuml;ssel liefert.
     * @return der alte Wert der dem Schl&uuml;ssel zugeordnet war.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public V merge(final StampedLock stampedLock, final K key, final V value,
                   final BiFunction<? super V, ? super V, ? extends V> remappingFunction) throws TimeoutException, InterruptedException {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        final long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            V oldValue = delegate.get(key);
            while (true) {
                if (oldValue != null) {
                    V newValue = remappingFunction.apply(oldValue, value);
                    if (newValue != null) {
                        if (replace(stampedLock, inOutStamp, key, oldValue, newValue)) {
                            return newValue;
                        }
                    } else if (remove(stampedLock, inOutStamp, key, oldValue)) {
                        return null;
                    }
                    oldValue = delegate.get(key);
                } else {
                    if ((oldValue = computeIfAbsent(stampedLock, inOutStamp, key, (k) -> value, (v) -> true)) == null) {
                        return value;
                    }
                }
            }
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
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
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
        final long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            return computeIfAbsent(stampedLock, inOutStamp, key, (k) -> newValue, (v) -> true);
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
     * Diese Methode wird intern von {@link #computeIfAbsent(StampedLock, Object, Function)}
     * aufgerufen und erwartet das bei Aufruf bereits eine Lese-Sperre gesetzt ist.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>1. Hinweis:</b> Die Mapping-Function (der dritte Parameter) wird nur dann ausgef&uuml;hrt wenn
     * eine exklusive Schreibsperre gesetzt werden konnte. Es ist daher sichergestellt, das die
     * Mapping-Function gleichzeitig stets nur von einem Threads ausgef&uuml;hrt wird.
     * <p>
     * <b>2. Hinweis:</b> Diese Methode nur ausgef&uuml;hrt werden wenn bereits einer Lesesperre gesetzt
     * wurde (siehe zweiter Parameter).
     *
     * @param stampedLock     die Sperre mit der die Schreib-/ Lese-Synchronisation erfolgt.
     * @param inOutStamp      ein Array mit einem Lock-Stempel der bei Aufruf einen Read-Stempel enthalten
     *                        muss und nach der R&uuml;ckkehr einen m&ouml;glicherweise neuen Lock-Stempel
     *                        enth&auml;lt der anschliessend freigegeben werden muss.
     * @param key             der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param mappingFunction die Funktion die den Wert passend zum Schl&uuml;ssel liefert und zugeordnet
     *                        werden soll wenn (noch) kein Wert zu dem angegebenen Schl&uuml;ssel existiert.
     * @param mapCondition    Bedingung die der neue Wert erf&uuml;llen muss damit er dem Schl&uuml;ssel
     *                        zugeordnet werden soll.
     * @return der Wert mit dem der Schl&uuml;ssel zugeordnet wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    private V computeIfAbsent(final StampedLock stampedLock, final long[] inOutStamp, K key, final Function<? super K, ? extends V> mappingFunction, final Predicate<V> mapCondition) throws InterruptedException, TimeoutException {
        V currValue = delegate.get(key);
        final Predicate<V> condition = (v) -> (v != null);
        // Zuerst eine Pruefung ob ein Schreib-Zugriff notwendig ist:
        if (condition.test(currValue)) {
            return currValue;
        } else {
            int retry = 0;
            while (true) {
                final long writeStamp = stampedLock.tryConvertToWriteLock(inOutStamp[0]);
                if (writeStamp != INVALID_STAMP) {
                    inOutStamp[0] = writeStamp;
                    final V newValue = mappingFunction.apply(key);
                    if (mapCondition.test(newValue)) {
                        delegate.put(key, newValue);
                        return newValue;
                    } else {
                        return null;
                    }
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
                    if (expectedModStamp != delegate.modificationStamp()) {
                        // Die Map hat sich zwischenzeitlich veraendert.
                        currValue = delegate.get(key);
                        // Daher wird die Pruefung hier wiederholt (identisch
                        // mit der Eingangs-Pruefung):
                        if (condition.test(currValue)) {
                            return currValue;
                        }
                    }
                }
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#remove(Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
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
        final long[] inOutStamp = {INVALID_STAMP};
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
     * Diese Methode wird intern von {@link #remove(StampedLock, Object, Object)}
     * aufgerufen und erwartet das bei Aufruf bereits eine Lese-Sperre gesetzt ist.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Diese Methode nur ausgef&uuml;hrt werden wenn bereits einer Lesesperre gesetzt
     * wurde (siehe zweiter Parameter).
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
        V currValue = delegate.get(key);
        final TriPredicate<V, Object, Object> condition = (a, b, c) -> (!Objects.equals(a, b) || (a == null && !delegate.containsKey(c)));
        // Zuerst eine Pruefung ob ein Schreib-Zugriff notwendig ist:
        if (condition.test(currValue, value, key)) {
            return false;
        } else {
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
                        // Daher wird die Pruefung hier wiederholt (identisch
                        // mit der Eingangs-Pruefung):
                        if (condition.test(currValue, value, key)) {
                            return false;
                        }
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
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
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
        final long[] inOutStamp = {INVALID_STAMP};
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
     * Eine Implementation von {@link Map#replace(Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
     *
     * @param stampedLock der Lock mit dem die Schreib-/ Lese-Synchronisation erfolgt.
     * @param key         der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param newValue    der neue Wert die dem Schl&uuml;ssel zugeordnet werden soll wenn entweder
     *                    der alte Wert {@code null} ist oder sich von dem neuen Wert unterscheidet.
     * @return der alte Wert der ersetzt wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public V replace(final StampedLock stampedLock, K key, V newValue) throws InterruptedException, TimeoutException {
        final long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            return replace(stampedLock, inOutStamp, key, newValue);
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
     * Diese Methode wird intern von {@link #replace(StampedLock, Object, Object, Object)}
     * aufgerufen und erwartet das bei Aufruf bereits eine Lese-Sperre gesetzt ist.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Diese Methode nur ausgef&uuml;hrt werden wenn bereits einer Lesesperre gesetzt
     * wurde (siehe zweiter Parameter).
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
        final TriPredicate<V, Object, Object> condition = (a, b, c) -> (!Objects.equals(a, b) || ((a == null) && !delegate.containsKey(c)));
        // Zuerst eine Pruefung ob ein Schreib-Zugriff notwendig ist:
        if (condition.test(currValue, oldValue, key)) {
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
                        // Die Map hat sich zwischenzeitlich veraendert.
                        currValue = delegate.get(key);
                        // Daher wird die Pruefung hier wiederholt (identisch
                        // mit der Eingangs-Pruefung):
                        if (condition.test(currValue, oldValue, key)) {
                            return false;
                        }
                    }
                }
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#replace(Object, Object)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode wird intern von {@link #replace(StampedLock, Object, Object)}
     * aufgerufen und erwartet das bei Aufruf bereits eine Lese-Sperre gesetzt ist.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>Hinweis:</b> Diese Methode nur ausgef&uuml;hrt werden wenn bereits einer Lesesperre gesetzt
     * wurde (siehe zweiter Parameter).
     *
     * @param stampedLock der Lock mit dem die Schreib-/ Lese-Synchronisation erfolgt.
     * @param inOutStamp  ein Array mit einem Sperren-Stempel der bei Aufruf einen Lese-Stempel enthalten
     *                    muss und nach der R&uuml;ckkehr einen m&ouml;glicherweise neuen Sperren-Stempel
     *                    enth&auml;lt der anschliessend wieder freigegeben werden muss.
     * @param key         der Schl&uuml;ssel mit dem der Wert aus der Funktion zugeordnet werden soll.
     * @param newValue    der neue Wert die dem Schl&uuml;ssel zugeordnet werden soll wenn entweder
     *                    der alte Wert {@code null} ist oder sich von dem neuen Wert unterscheidet.
     * @return der alte Wert der ersetzt wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    private V replace(final StampedLock stampedLock, final long[] inOutStamp, K key, V newValue) throws InterruptedException, TimeoutException {
        V currValue = delegate.get(key);
        final BiPredicate<V, Object> condition = (a, b) -> (a == null && !delegate.containsKey(b));
        // Zuerst eine Pruefung ob ein Schreib-Zugriff notwendig ist:
        if (condition.test(currValue, key)) {
            return null;
        } else {
            int retry = 0;
            while (true) {
                final long writeStamp = stampedLock.tryConvertToWriteLock(inOutStamp[0]);
                if (writeStamp != INVALID_STAMP) {
                    inOutStamp[0] = writeStamp;
                    return delegate.put(key, newValue);
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
                        // Die Map hat sich zwischenzeitlich veraendert.
                        currValue = delegate.get(key);
                        // Daher wird die Pruefung hier wiederholt (identisch
                        // mit der Eingangs-Pruefung):
                        if (condition.test(currValue, key)) {
                            return null;
                        }
                    }
                }
            }
        }
    }

    /**
     * Eine Implementation von {@link Map#replaceAll(BiFunction)} die nebenl&auml;fig
     * aufgerufen werden kann.
     * <p>
     * Diese Methode ist optimiert f&uuml;r Situationen wo die Lese-Operationen im Verh&auml;ltnis
     * zu den Schreib-Operationen h&auml;ufiger erfolgreich aufgerufen werden k&ouml;nnen.
     * Wenn also &uuml;berwiegend zu dem angegebenen Schl&uuml;ssel sich in der Map bereits
     * ein entsprechender Wert befindet.
     * <p>
     * <b>1. Hinweis:</b> Der Thread der diese Methode ausf&uuml;hrt darf nicht schon bereits
     * in Besitz einer Lese- oder Schreibsperre sein (siehe ersten Parameter).
     * <p>
     * <b>2. Hinweis:</b> Die Mapping-Function (der dritte Parameter) wird vor paralleler Ausf&uuml;hrung
     * nicht gesch&uuml;tzt. Es ist daher m&ouml;glich, das die Mapping-Function gleichzeitig von
     * mehreren Threads ausgef&uuml;hrt wird.
     *
     * @param stampedLock       der Lock mit dem die Schreib-/ Lese-Synchronisation erfolgt.
     * @param remappingFunction die Funktion die den neuen Wert passend zum Schl&uuml;ssel liefert.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public void replaceAll(final StampedLock stampedLock, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws TimeoutException, InterruptedException {
        Objects.requireNonNull(remappingFunction);
        final long[] inOutStamp = {INVALID_STAMP};
        try {
            inOutStamp[0] = tryReadLock(stampedLock);
            delegate.forEach(new BiConsumer<K, V>() {

                @Override
                public void accept(K k, V v) {
                    while (!replace(stampedLock, inOutStamp, k, v, remappingFunction.apply(k, v))) {
                        // value hat sich geaendert oder key ist nicht mehr in der Map
                        if (!delegate.containsKey(k)) {
                            // key ist nicht mehr in der Map
                            break;
                        }
                    }
                }

                boolean replace(final StampedLock stampedLock, final long[] inOutStamp, K key, V oldValue, V newValue) {
                    try {
                        return ConcurrentMapFunktions.this.replace(stampedLock, inOutStamp, key, oldValue, newValue);
                    } catch (InterruptedException | TimeoutException ex) {
                        // Maskiert die Checked-Exceptions in eine Runtime-Exception
                        throw new IllegalStateException(ex);
                    }
                }
            });
        } catch (IllegalStateException ise) {
            // Die maskierte Checked-Exceptions wird wieder offengelegt:
            final Throwable cause = ise.getCause();
            if (cause instanceof InterruptedException) {
                throw ((InterruptedException) cause);
            } else if (cause instanceof TimeoutException) {
                throw ((TimeoutException) cause);
            } else {
                throw ise;
            }
        } finally {
            if (inOutStamp[0] != INVALID_STAMP) {
                stampedLock.unlock(inOutStamp[0]); // Read- oder Write-Lock
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
        final long readStamp = stampedLock.tryReadLock(timeoutSeconds, TimeUnit.SECONDS);
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
        final long writeStamp = stampedLock.tryWriteLock(timeoutSeconds, TimeUnit.SECONDS);
        if (writeStamp == INVALID_STAMP) {
            throw new TimeoutException("Unable to acquire a write lock within " + timeoutSeconds + " seconds.");
        }
        return writeStamp;
    }

    /**
     * Repr&auml;sentiert ein Pr&auml;dikat (boolesche Funktion) mit drei Argumenten.
     *
     * @param <A> der generische Typ des ersten Agruments.
     * @param <B> der generische Typ des zweiten Agruments.
     * @param <C> der generische Typ des dritten Agruments.
     */
    @FunctionalInterface
    interface TriPredicate<A, B, C> {
        boolean test(A a, B b, C c);
    }
}
