package eu.dirk.haase.jdbc.proxy.common;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * WeakIdentityHashMap is an implementation of IdentityHashMap with keys which are WeakReferences. A
 * key/value mapping is removed when the key is no longer referenced. All
 * optional operations (adding and removing) are supported. Keys and values can
 * be any objects. Note that the garbage collector acts similar to a second
 * thread on this collection, possibly removing keys.
 *
 * @see java.util.IdentityHashMap
 * @see java.util.WeakHashMap
 * @see java.lang.ref.WeakReference
 * @see java.lang.ref.SoftReference
 */
public class WeakIdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, ModificationStampingObject {
    private static final double DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_SIZE = 16;
    private static final int MILLI = 1000;
    private final ConcurrentMapFunktions<WeakIdentityHashMap<K, V>, K, V> concurrentMapFunktions;
    private final int loadFactorMillis;
    private final AtomicLong modificationCount;
    private final ReferenceQueue<K> referenceQueue;
    private Entry<K, V>[] bucketArray;
    private int entryCount;
    private transient Set<Map.Entry<K, V>> entrySet;
    private boolean isEqualityByIdentity = true;
    private boolean isSoftReference = false;
    private transient Set<K> keySet;
    private int reclaimedEntryCount;
    private int threshold;
    private transient Collection<V> valuesCollection;

    /**
     * Constructs a new empty {@code WeakIdentityHashMap} instance.
     */
    public WeakIdentityHashMap() {
        this(DEFAULT_SIZE);
    }

    /**
     * Constructs a new {@code WeakIdentityHashMap} instance with the specified
     * capacity.
     *
     * @param capacity the initial capacity of this map.
     * @throws IllegalArgumentException if the capacity is less than zero.
     */
    public WeakIdentityHashMap(int capacity) {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs a new {@code WeakIdentityHashMap} instance with the specified capacity
     * and load factor.
     *
     * @param capacity   the initial capacity of this map.
     * @param loadFactor the initial load factor.
     * @throws IllegalArgumentException if the capacity is less than zero or the load factor is less
     *                                  or equal to zero.
     */
    public WeakIdentityHashMap(int capacity, double loadFactor) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("loadFactor <= 0: " + loadFactor);
        }
        this.concurrentMapFunktions = new ConcurrentMapFunktions<>(this);
        this.modificationCount = new AtomicLong(0);
        this.entryCount = 0;
        this.loadFactorMillis = (int) (loadFactor * MILLI);
        this.referenceQueue = new ReferenceQueue<>();
        final int minBucketSize = (capacity == 0 ? 1 : capacity);
        expandBucketArray(minBucketSize);
    }

    /**
     * Constructs a new {@code WeakIdentityHashMap} instance containing the mappings
     * from the specified map.
     *
     * @param map the mappings to add.
     */
    public WeakIdentityHashMap(Map<? extends K, ? extends V> map) {
        this(map.size());
        putAll(map);
    }

    private static boolean equalsKey(boolean isEqualityByIdentity, Object thisKey, Object thatKey) {
        if (isEqualityByIdentity) {
            return (thisKey == thatKey);
        } else {
            return (thisKey == null ? thatKey == null : thisKey.equals(thatKey));
        }
    }

    private static int keyHash(boolean isEqualityByIdentity, Object thisKey) {
        if (thisKey == null) {
            return 0;
        }
        return isEqualityByIdentity ? System.identityHashCode(thisKey) : thisKey.hashCode();
    }

    private int bucketIndex(int hash) {
        return (hash > 0 ? (hash & 0x7FFFFFFF) % bucketArray.length : 0);
    }

    /**
     * Removes all mappings from this map, leaving it empty.
     *
     * @see #isEmpty()
     * @see #size()
     */
    @Override
    public void clear() {
        if (entryCount > 0) {
            entryCount = 0;
            Arrays.fill(bucketArray, null);
            modificationCount.incrementAndGet();
            while (referenceQueue.poll() != null) {
                // do nothing
            }
        }
        reclaimedEntryCount = 0;
    }

    /**
     * Eine Implementation von {@link Map#compute(Object, BiFunction)} die nebenl&auml;fig
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
    public V compute(final StampedLock stampedLock,
                     final K key,
                     final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws InterruptedException, TimeoutException {
        return this.concurrentMapFunktions.compute(stampedLock, key, remappingFunction);
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
        return this.concurrentMapFunktions.computeIfAbsent(stampedLock, key, mappingFunction);
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
        return this.concurrentMapFunktions.computeIfPresent(stampedLock, key, remappingFunction);
    }

    private int computeThreshold(int bucketSize) {
        return (int) ((long) bucketSize * loadFactorMillis / MILLI);
    }

    public boolean containsEntry(Object entryObj) {
        if (entryObj instanceof Entry) {
            final Entry<K, V> currEntry = getEntryOfKey(((Map.Entry<K, V>) entryObj).getKey());
            if ((currEntry != null) && !currEntry.isReclaimed()) {
                K key = currEntry.getKey();
                if (key != null || currEntry.isNull()) {
                    return currEntry.equals(entryObj);
                }
            }
        }
        return false;
    }

    /**
     * Returns whether this map contains the specified key.
     *
     * @param keyObj the key to search for.
     * @return {@code true} if this map contains the specified key,
     * {@code false} otherwise.
     */
    @Override
    public boolean containsKey(Object keyObj) {
        return getEntryOfKey(keyObj) != null;
    }

    /**
     * Returns whether this map contains the specified value.
     *
     * @param valueObj the value to search for.
     * @return {@code true} if this map contains the specified value,
     * {@code  false} otherwise.
     */
    @Override
    public boolean containsValue(Object valueObj) {
        for (int i = bucketArray.length; --i >= 0; ) {
            Entry<K, V> currEntry = bucketArray[i];
            while ((currEntry != null) && !currEntry.isReclaimed()) {
                final K currKey = currEntry.getKey();
                if (((currKey != null) || currEntry.isNull()) && (currEntry.getValue() == null) && valueObj.equals(currEntry.getValue())) {
                    return true;
                }
                currEntry = currEntry.getNext();
            }
        }
        return false;
    }

    private Entry createEntry(K key, V object, ReferenceQueue<K> queue) {
        if (key != null) {
            if (isSoftReference) {
                return new SoftEntry<>(key, object, queue, isEqualityByIdentity);
            } else {
                return new WeakEntry<>(key, object, queue, isEqualityByIdentity);
            }
        } else {
            return new NullEntry<>(object, isEqualityByIdentity);
        }
    }

    /**
     * Returns a set containing all of the mappings in this map. Each mapping is
     * an instance of {@link Map.Entry}. As the set is backed by this map,
     * changes in one will be reflected in the other. It does not support adding
     * operations.
     *
     * @return a set of the mappings.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new InnerSet<>(() -> new HashIterator<>(entry -> entry), (e) -> containsEntry(e), (e) -> removeEntry(e));
        }
        return entrySet;
    }

    private void expandBucketArray(int minBucketSize) {
        final Entry<K, V>[] newBucketArray = new Entry[minBucketSize * 2];
        this.bucketArray = rehash(newBucketArray);
        this.threshold = computeThreshold(newBucketArray.length);
    }

    /**
     * Returns the value of the mapping with the specified key.
     *
     * @param key the key.
     * @return the value of the mapping with the specified key, or {@code null}
     * if no mapping for the specified key is found.
     */
    @Override
    public V get(Object key) {
        Entry<K, V> entry = getEntryOfKey(key);
        return entry != null ? entry.getValue() : null;
    }

    private Entry<K, V> getEntryOfKey(Object keyObj) {
        if (entryCount > 0) {
            if (keyObj != null) {
                final int index = bucketIndex(keyHash(isEqualityByIdentity, keyObj));
                Entry<K, V> currEntry = bucketArray[index];
                while ((currEntry != null) && !currEntry.isReclaimed()) {
                    final boolean isEqual = equalsKey(isEqualityByIdentity, keyObj, currEntry.getKey());
                    if (isEqual) {
                        return currEntry;
                    }
                    currEntry = currEntry.getNext();
                }
            } else {
                // Key-Argument is null
                Entry<K, V> currEntry = bucketArray[0];
                while ((currEntry != null) && !currEntry.isReclaimed()) {
                    if (currEntry.isNull()) {
                        return currEntry;
                    }
                    currEntry = currEntry.getNext();
                }
            }
        }
        return null;
    }

    public int getReclaimedEntryCount() {
        return reclaimedEntryCount;
    }

    /**
     * @return <code>true</code> if this map is empty. <code>false</code> otherwise.
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean isEqualityByIdentity() {
        return isEqualityByIdentity;
    }

    public void setEqualityByIdentity(boolean isEqualityByIdentity) {
        if (isEmpty()) {
            this.isEqualityByIdentity = isEqualityByIdentity;
        } else {
            throw new IllegalStateException("Equality by identity can only be changed on an empty map.");
        }
    }

    public boolean isSoftReference() {
        return isSoftReference;
    }

    public void setSoftReference(boolean softReference) {
        isSoftReference = softReference;
    }

    /**
     * Returns a set of the keys contained in this map. The set is backed by
     * this map so changes to one are reflected by the other. The set does not
     * support adding.
     *
     * @return a set of the keys.
     */
    @Override
    public Set<K> keySet() {
        if (keySet == null) {
            keySet = new InnerSet<>(() -> new HashIterator<>(entry -> entry.getKey()), (k) -> containsKey(k), (k) -> removeKey(k));
        }
        return keySet;
    }

    /**
     * Eine Implementation von {@link Map#merge(Object, Object, BiFunction)} die nebenl&auml;fig
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
    public V merge(final StampedLock stampedLock,
                   final K key,
                   final V value,
                   final BiFunction<? super V, ? super V, ? extends V> remappingFunction) throws InterruptedException, TimeoutException {
        return this.concurrentMapFunktions.merge(stampedLock, key, value, remappingFunction);
    }

    /**
     * Liefert bei jeder Zustandsver&auml;nderung einen neuen Stempel-Wert.
     *
     * @return der neuen Stempel-Wert bei einer neuen Zustandsver&auml;nderung.
     */
    @Override
    public long modificationStamp() {
        return modificationCount.get();
    }

    @SuppressWarnings("unchecked")
    public int purge() {
        final int lastElementCount = entryCount;
        Entry<K, V> entryToRemove;
        while ((entryToRemove = (Entry<K, V>) referenceQueue.poll()) != null) {
            removeEntry(entryToRemove);
        }
        reclaimedEntryCount += (lastElementCount - entryCount);
        return reclaimedEntryCount;
    }

    /**
     * Maps the specified key to the specified value.
     *
     * @param key      the key.
     * @param newValue the value.
     * @return the value of any previous mapping with the specified key or
     * {@code null} if there was no mapping.
     */
    @Override
    public V put(K key, V newValue) {
        final Entry<K, V> currEntry = getEntryOfKey(key);
        if (currEntry == null) {
            purge();
            final int index = bucketIndex(keyHash(isEqualityByIdentity, key));
            modificationCount.incrementAndGet();
            ;
            entryCount++;
            if (entryCount > threshold) {
                expandBucketArray(bucketArray.length);
            }
            final Entry<K, V> newEntry = createEntry(key, newValue, referenceQueue);
            newEntry.setNext(bucketArray[index]);
            bucketArray[index] = newEntry;
            return null;
        } else {
            return currEntry.setValue(newValue);
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
        return this.concurrentMapFunktions.putIfAbsent(stampedLock, key, newValue);
    }

    private Entry<K, V>[] rehash(final Entry<K, V>[] newBucketArray) {
        if (bucketArray != null) {
            for (Entry<K, V> entry : bucketArray) {
                while (entry != null) {
                    int index = entry.isNull() ? 0 : bucketIndex(entry.hash());
                    Entry<K, V> next = entry.getNext();
                    entry.setNext(newBucketArray[index]);
                    newBucketArray[index] = entry;
                    entry = next;
                }
            }
        }
        return newBucketArray;
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
        return this.concurrentMapFunktions.remove(stampedLock, key, value);
    }

    /**
     * Removes the mapping with the specified key from this map.
     *
     * @param keyToRemove the key of the mapping to remove.
     * @return the value of the removed mapping or {@code null} if no mapping
     * for the specified key was found.
     */
    @Override
    public V remove(Object keyToRemove) {
        purge();
        final int index = bucketIndex(keyHash(isEqualityByIdentity, keyToRemove));
        Entry<K, V> currEntry;
        Entry<K, V> lastEntry = null;
        if (keyToRemove != null) {
            currEntry = bucketArray[index];
            while (currEntry != null && !equalsKey(isEqualityByIdentity, keyToRemove, currEntry.getKey())) {
                lastEntry = currEntry;
                currEntry = currEntry.getNext();
            }
        } else {
            currEntry = bucketArray[0];
            while (currEntry != null && !currEntry.isNull()) {
                lastEntry = currEntry;
                currEntry = currEntry.getNext();
            }
        }
        if (currEntry != null) {
            modificationCount.incrementAndGet();
            ;
            if (lastEntry == null) {
                bucketArray[index] = currEntry.getNext();
            } else {
                lastEntry.setNext(currEntry.getNext());
            }
            entryCount--;
            return currEntry.getValue();
        }
        return null;
    }

    private boolean removeEntry(Entry<K, V> entryToRemove) {
        if (entryToRemove != null) {
            final int index = bucketIndex(entryToRemove.hash());
            Entry<K, V> currEntry = bucketArray[index];
            Entry<K, V> lastEntry = null;
            while (currEntry != null) {
                if (entryToRemove == currEntry) {
                    modificationCount.incrementAndGet();
                    ;
                    if (lastEntry == null) {
                        bucketArray[index] = currEntry.getNext();
                    } else {
                        lastEntry.setNext(currEntry.getNext());
                    }
                    entryCount--;
                    break;
                }
                lastEntry = currEntry;
                currEntry = currEntry.getNext();
            }

            return (lastEntry != null);
        }
        return false;
    }

    private boolean removeEntry(Object entryToRemove) {
        if (entryToRemove instanceof Entry) {
            return removeEntry((Entry<K, V>) entryToRemove);
        }
        return false;
    }

    private boolean removeKey(Object keyToRemove) {
        if (containsKey(keyToRemove)) {
            remove(keyToRemove);
            return true;
        }
        return false;
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
        return this.concurrentMapFunktions.replace(stampedLock, key, oldValue, newValue);
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
     * @param newValue    der neue Wert die dem Schl&uuml;ssel zugeordnet werden soll wenn entweder
     *                    der alte Wert {@code null} ist oder sich von dem neuen Wert unterscheidet.
     * @return der alte Wert der ersetzt wurde.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public V replace(final StampedLock stampedLock, K key, V newValue) throws InterruptedException, TimeoutException {
        return this.concurrentMapFunktions.replace(stampedLock, key, newValue);
    }

    /**
     * Eine Implementation von {@link Map#replaceAll(BiFunction)} die nebenl&auml;fig
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
     * @param remappingFunction die Funktion die den Wert passend zum Schl&uuml;ssel liefert.
     * @throws InterruptedException wenn der aktuelle Thread durch {@link Thread#interrupt()} unterbrochen
     *                              wurde.
     * @throws TimeoutException     der Lock konnte nicht rechtzeitig in vorgegebener Zeit angefordert werden.
     */
    public void replaceAll(final StampedLock stampedLock,
                           final BiFunction<? super K, ? super V, ? extends V> remappingFunction) throws InterruptedException, TimeoutException {
        this.concurrentMapFunktions.replaceAll(stampedLock, remappingFunction);
    }

    /**
     * @return the number of elements in this map.
     */
    @Override
    public int size() {
        return entryCount;
    }

    public void trimToSize() {
        purge();
        final int minBucketSize = (entryCount == 0 ? 1 : entryCount);
        if (minBucketSize < this.bucketArray.length) {
            Entry<K, V>[] newBucketArray = new Entry[minBucketSize];
            this.bucketArray = rehash(newBucketArray);
            this.threshold = computeThreshold(newBucketArray.length);
        }
    }

    /**
     * <p>
     * Returns a collection of the values contained in this map. The collection
     * is backed by this map so changes to one are reflected by the other. The
     * collection supports remove, removeAll, retainAll and clear operations,
     * and it does not support add or addAll operations.
     * </p>
     * <p>
     * This method returns a collection which is the subclass of
     * AbstractCollection. The iterator method of this subclass returns a
     * "wrapper object" over the iterator of map's entrySet(). The size method
     * wraps the map's size method and the contains method wraps the map's
     * containsValue method.
     * </p>
     * <p>
     * The collection is created when this method is called at first time and
     * returned in response to all subsequent calls. This method may return
     * different Collection when multiple calls to this method, since it has no
     * synchronization performed.
     * </p>
     *
     * @return a collection of the values contained in this map.
     */
    @Override
    public Collection<V> values() {
        if (valuesCollection == null) {
            valuesCollection = new ValueCollection<>();
        }
        return valuesCollection;
    }

    /**
     * Entry-API mit der die Eintr&auml;ge gespeichert werden.
     *
     * @param <K> generische Typ des Schl&uuml;ussels.
     * @param <V> generischer Typ des Wertes.
     */
    interface Entry<K, V> extends Map.Entry<K, V> {

        default boolean equalsEntry(Object thatObj) {
            if (!(thatObj instanceof Entry)) {
                return false;
            }
            final Entry<?, ?> that = (Entry<?, ?>) thatObj;
            return (this.isNull() == that.isNull()
                    && equalsKey(this.isEqualityByIdentity(), this.getKey(), that.getKey()))
                    && (this.getValue() == null ? that.getValue() == null : this.getValue().equals(that.getValue()));
        }

        Entry<K, V> getNext();

        void setNext(Entry<K, V> entry);

        int hash();

        boolean isEqualityByIdentity();

        /**
         * Liefert dann {@code true} wenn der Schl&uuml;ussel von vornherein
         * {@code null} war.
         * <p>
         * Je nach Implementation liefert diese Methode stets den gleichen Wert:
         * <ul>
         * <li>bei {@link NullEntry} wird stets true,</li>
         * <li>bei {@link WeakEntry} wird stets false und </li>
         * <li>bei {@link SoftEntry} wird stets false zur&uuml;ckgeliefert</li>
         * </ul>
         *
         * @return {@code true} wenn der Schl&uuml;ussel von vornherein
         * {@code null} war.
         */
        default boolean isNull() {
            return false;
        }

        /**
         * Liefert {@code true} wenn das Schl&uuml;ussel-Object durch den Garbage-Collector
         * wegger&auml;umt wurde.
         * <p>
         * Vom Garbage-Collector entfernt k&ouml;nnen nur Entr&auml;ge die entweder:
         * <ul>
         * <li>mit der Klasse {@link WeakEntry} oder </li>
         * <li>mit der {@link SoftEntry} implementiert wurden.</li>
         * </ul>
         *
         * @return {@code true} wenn das Schl&uuml;ussel-Object durch den Garbage-Collector
         * wegger&auml;umt wurde.
         */
        default boolean isReclaimed() {
            return getKey() == null;
        }
    }

    /**
     * Entry-Implementation f&uuml;r Schl&uuml;ssel die {@code null} sind.
     *
     * @param <K> generische Typ des Schl&uuml;ussels.
     * @param <V> generischer Typ des Wertes.
     */
    static final class NullEntry<K, V> implements Entry<K, V> {
        final boolean isEqualityByIdentity;
        Entry<K, V> next;
        V value;

        NullEntry(final V value, final boolean isEqualityByIdentity) {
            this.value = value;
            this.isEqualityByIdentity = isEqualityByIdentity;
        }

        @Override
        public boolean equals(Object other) {
            return equalsEntry(other);
        }

        @Override
        public K getKey() {
            return null;
        }

        @Override
        public Entry<K, V> getNext() {
            return next;
        }

        @Override
        public void setNext(Entry<K, V> entry) {
            next = entry;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public int hash() {
            return 0;
        }

        @Override
        public int hashCode() {
            return (value == null ? 0 : value.hashCode());
        }

        @Override
        public boolean isEqualityByIdentity() {
            return isEqualityByIdentity;
        }

        /**
         * Liefert stets {@code true} da der Schl&uuml;ssel bereits von Anfang an
         * {@code null} ist.
         *
         * @return stets {@code true}.
         */
        @Override
        public boolean isNull() {
            return true;
        }

        /**
         * Liefert stets {@code false} da der Schl&uuml;ssel die bereits von Anfang an
         * {@code null} ist vom Garbage-Collector nicht entfernt muss.
         *
         * @return stets {@code false}.
         */
        @Override
        public boolean isReclaimed() {
            return false;
        }

        @Override
        public V setValue(V newValue) {
            V lastValue = value;
            value = newValue;
            return lastValue;
        }

        @Override
        public String toString() {
            return "<null>=" + getValue();
        }
    }

    /**
     * Entry-Implementation f&uuml;r Schl&uuml;ssel die als {@link SoftReference}
     * gespeichert werden.
     *
     * @param <K> generische Typ des Schl&uuml;ussels.
     * @param <V> generischer Typ des Wertes.
     */
    static final class SoftEntry<K, V> extends SoftReference<K> implements Entry<K, V> {
        final int hash;
        final boolean isEqualityByIdentity;
        Entry<K, V> next;
        V value;

        SoftEntry(final K key, final V value, final ReferenceQueue<K> queue, final boolean isEqualityByIdentity) {
            super(key, queue);
            this.hash = keyHash(isEqualityByIdentity, key);
            this.value = value;
            this.isEqualityByIdentity = isEqualityByIdentity;
        }

        @Override
        public boolean equals(Object other) {
            return equalsEntry(other);
        }

        @Override
        public K getKey() {
            return super.get();
        }

        @Override
        public Entry<K, V> getNext() {
            return next;
        }

        @Override
        public void setNext(Entry<K, V> entry) {
            next = entry;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public int hash() {
            return hash;
        }

        @Override
        public int hashCode() {
            return hash + (value == null ? 0 : value.hashCode());
        }

        @Override
        public boolean isEqualityByIdentity() {
            return isEqualityByIdentity;
        }

        @Override
        public V setValue(V newValue) {
            V lastValue = value;
            value = newValue;
            return lastValue;
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    /**
     * Entry-Implementation f&uuml;r Schl&uuml;ssel die als {@link WeakReference}
     * gespeichert werden.
     *
     * @param <K> generische Typ des Schl&uuml;ssels.
     * @param <V> generischer Typ des Wertes.
     */
    static final class WeakEntry<K, V> extends WeakReference<K> implements Entry<K, V> {
        final int hash;
        final boolean isEqualityByIdentity;
        Entry<K, V> next;
        V value;

        WeakEntry(final K key, final V value, final ReferenceQueue<K> queue, final boolean isEqualityByIdentity) {
            super(key, queue);
            this.hash = keyHash(isEqualityByIdentity, key);
            this.value = value;
            this.isEqualityByIdentity = isEqualityByIdentity;
        }

        @Override
        public boolean equals(Object other) {
            return equalsEntry(other);
        }

        @Override
        public K getKey() {
            return super.get();
        }

        @Override
        public Entry<K, V> getNext() {
            return next;
        }

        @Override
        public void setNext(Entry<K, V> entry) {
            next = entry;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public int hash() {
            return hash;
        }

        @Override
        public int hashCode() {
            return hash + (value == null ? 0 : value.hashCode());
        }

        @Override
        public boolean isEqualityByIdentity() {
            return isEqualityByIdentity;
        }

        @Override
        public V setValue(V newValue) {
            V lastValue = value;
            value = newValue;
            return lastValue;
        }

        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }

    }

    /**
     * An die jeweilige {@link WeakIdentityHashMap}-Instanz gebundener Iterator f&uuml;r
     * die Collections die durch die Methoden {@link #entrySet()}, {@link #keySet()} und
     * {@link #values()} erzeugt werden.
     *
     * @param <R> der Typ der Elemente des Iterators.
     */
    class HashIterator<R> implements Iterator<R> {
        private final Function<Map.Entry<K, V>, R> entryFuntion;
        private Entry<K, V> currEntry;
        private long expectedModCount;
        private Entry<K, V> nextEntry;
        private K nextKey;
        private int position = 0;

        HashIterator(Function<Map.Entry<K, V>, R> entryFuntion) {
            purge();
            this.entryFuntion = entryFuntion;
            this.expectedModCount = WeakIdentityHashMap.this.modificationCount.get();
        }

        @Override
        public boolean hasNext() {
            if (nextEntry != null && (nextKey != null || nextEntry.isNull())) {
                return true;
            }
            while (true) {
                if (nextEntry == null) {
                    while (position < WeakIdentityHashMap.this.bucketArray.length) {
                        if ((nextEntry = WeakIdentityHashMap.this.bucketArray[position++]) != null) {
                            break;
                        }
                    }
                    if (nextEntry == null) {
                        return false;
                    }
                }
                // ensure key of next entry is not gc'ed
                nextKey = nextEntry.getKey();
                if (nextKey != null || nextEntry.isNull()) {
                    return true;
                }
                nextEntry = nextEntry.getNext();
            }
        }

        @Override
        public R next() {
            if (this.expectedModCount == WeakIdentityHashMap.this.modificationCount.get()) {
                if (hasNext()) {
                    currEntry = nextEntry;
                    nextEntry = currEntry.getNext();
                    final R result = entryFuntion.apply(currEntry);
                    // free the key
                    nextKey = null;
                    return result;
                }
                throw new NoSuchElementException("There is no next in map with " + size() + " entries.");
            }
            throw new ConcurrentModificationException("Concurrent change occurred while moving to next entry");
        }

        @Override
        public void remove() {
            if (expectedModCount == WeakIdentityHashMap.this.modificationCount.get()) {
                if (currEntry != null) {
                    WeakIdentityHashMap.this.removeEntry(currEntry);
                    currEntry = null;
                    expectedModCount++;
                    // cannot purge() as that would change the expectedModCount
                } else {
                    throw new IllegalStateException();
                }
            } else {
                throw new ConcurrentModificationException("Concurrent change occurred while removing");
            }
        }
    }

    /**
     * An die jeweilige {@link WeakIdentityHashMap}-Instanz gebundenes Set f&uuml;r
     * die Methoden {@link #entrySet()} und {@link #keySet()}.
     *
     * @param <E> der Typ der Elemente des Sets.
     */
    class InnerSet<E> extends AbstractSet<E> {

        final Predicate<Object> containsPredicate;
        final Supplier<Iterator<E>> hashIteratorSupplier;
        final Function<Object, Boolean> removeFunction;

        InnerSet(final Supplier<Iterator<E>> hashIteratorSupplier, final Predicate<Object> containsPredicate, final Function<Object, Boolean> removeFunction) {
            this.hashIteratorSupplier = hashIteratorSupplier;
            this.containsPredicate = containsPredicate;
            this.removeFunction = removeFunction;
        }

        @Override
        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        @Override
        public boolean contains(Object object) {
            return containsPredicate.test(object);
        }

        @Override
        public Iterator<E> iterator() {
            return hashIteratorSupplier.get();
        }

        @Override
        public boolean remove(Object key) {
            return removeFunction.apply(key);
        }

        @Override
        public int size() {
            return WeakIdentityHashMap.this.size();
        }
    }

    /**
     * An die jeweilige {@link WeakIdentityHashMap}-Instanz gebundene Collection f&uuml;r
     * die Methode {@link #values()}.
     *
     * @param <E> der Typ der Elemente des Sets.
     */
    class ValueCollection<E> extends AbstractCollection<E> {

        ValueCollection() {
        }

        @Override
        public void clear() {
            WeakIdentityHashMap.this.clear();
        }

        @Override
        public boolean contains(Object object) {
            return containsValue(object);
        }

        @Override
        public Iterator<E> iterator() {
            return WeakIdentityHashMap.this.new HashIterator<E>(entry -> (E) entry.getValue());
        }

        @Override
        public int size() {
            return WeakIdentityHashMap.this.size();
        }
    }


}
