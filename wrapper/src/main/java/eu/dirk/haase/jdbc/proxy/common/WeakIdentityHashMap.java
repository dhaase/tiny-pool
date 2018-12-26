package eu.dirk.haase.jdbc.proxy.common;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;
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
public class WeakIdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_SIZE = 16;
    /**
     * Denoting a factor of one thousandth (1/1000)
     */
    private static final int MILLI = 1000;
    private final int loadFactorMillis;
    private final ReferenceQueue<K> referenceQueue;
    private Entry<K, V>[] bucketArray;
    private int entryCount;
    private transient Set<Map.Entry<K, V>> entrySet;
    private boolean isEqualityByIdentity = true;
    private boolean isSoftReference = false;
    private transient Set<K> keySet;
    private volatile int modificationCount;
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
    public WeakIdentityHashMap(int capacity, float loadFactor) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity < 0: " + capacity);
        }
        if (loadFactor <= 0) {
            throw new IllegalArgumentException("loadFactor <= 0: " + loadFactor);
        }
        entryCount = 0;
        bucketArray = newBucketArray(capacity == 0 ? 1 : capacity);
        this.loadFactorMillis = (int) (loadFactor * MILLI);
        this.threshold = computeThreshold();
        referenceQueue = new ReferenceQueue<K>();
    }

    /**
     * Constructs a new {@code WeakIdentityHashMap} instance containing the mappings
     * from the specified map.
     *
     * @param map the mappings to add.
     */
    public WeakIdentityHashMap(Map<? extends K, ? extends V> map) {
        this(map.size() * 2);
        putAll(map);
    }

    private static boolean equalsKey(boolean isEqualityByIdentity, Object thisKey, Object thatKey) {
        boolean isEqual = (thisKey == thatKey);
        if (!isEqual && !isEqualityByIdentity) {
            isEqual = (thisKey != null ? thisKey.equals(thatKey) : thisKey == thatKey);
        }
        return isEqual;
    }

    private static int keyHash(boolean isEqualityByIdentity, Object thisKey) {
        if (thisKey == null) {
            return 0;
        }
        return isEqualityByIdentity ? System.identityHashCode(thisKey) : thisKey.hashCode();
    }

    // Simple utility method to isolate unchecked cast for array creation
    @SuppressWarnings("unchecked")
    private static <K, V> Entry<K, V>[] newBucketArray(int minSize) {
        int length = minSize * 2;
        length = (length > 0 ? length : 1);
        return new Entry[length];
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
            modificationCount++;
            while (referenceQueue.poll() != null) {
                // do nothing
            }
        }
    }

    private int computeThreshold() {
        return (int) ((long) bucketArray.length * loadFactorMillis / MILLI);
    }

    public boolean containsEntry(Object object) {
        if (object instanceof Map.Entry) {
            Entry<?, ?> entry = getEntry(((Map.Entry<?, ?>) object)
                    .getKey());
            if (entry != null) {
                Object key = entry.getKey();
                if (key != null || entry.isNull()) {
                    return object.equals(entry);
                }
            }
        }
        return false;
    }

    /**
     * Returns whether this map contains the specified key.
     *
     * @param key the key to search for.
     * @return {@code true} if this map contains the specified key,
     * {@code false} otherwise.
     */
    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * Returns whether this map contains the specified value.
     *
     * @param value the value to search for.
     * @return {@code true} if this map contains the specified value,
     * {@code false} otherwise.
     */
    @Override
    public boolean containsValue(Object value) {
        purge();
        if (value != null) {
            for (int i = bucketArray.length; --i >= 0; ) {
                Entry<K, V> entry = bucketArray[i];
                while (entry != null) {
                    K key = entry.getKey();
                    if ((key != null || entry.isNull())
                            && value.equals(entry.getValue())) {
                        return true;
                    }
                    entry = entry.getNext();
                }
            }
        } else {
            for (int i = bucketArray.length; --i >= 0; ) {
                Entry<K, V> entry = bucketArray[i];
                while (entry != null) {
                    K key = entry.getKey();
                    if ((key != null || entry.isNull()) && entry.getValue() == null) {
                        return true;
                    }
                    entry = entry.getNext();
                }
            }
        }
        return false;
    }

    private Entry createEntry(K key, V object, ReferenceQueue<K> queue) {
        if (isSoftReference) {
            return new SoftEntry<>(key, object, queue, isEqualityByIdentity);
        } else {
            return new WeakEntry<>(key, object, queue, isEqualityByIdentity);
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
        purge();
        if (entrySet == null) {
            entrySet = new InnerSet(()->new HashIterator<>(entry -> entry.getKey()), (e)->containsEntry(e), (e)->removeEntry(e));
        }
        return entrySet;
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
        Entry<K, V> entry = getEntry(key);
        return entry != null ? entry.getValue() : null;
    }

    private Entry<K, V> getEntry(Object key) {
        purge();
        if (key != null) {
            int index = bucketIndex(keyHash(isEqualityByIdentity, key));
            Entry<K, V> entry = bucketArray[index];
            while (entry != null) {
                boolean isEqual = equalsKey(isEqualityByIdentity, key, entry.getKey());
                if (isEqual) {
                    return entry;
                }
                entry = entry.getNext();
            }
            return null;
        }
        Entry<K, V> entry = bucketArray[0];
        while (entry != null) {
            if (entry.isNull()) {
                return entry;
            }
            entry = entry.getNext();
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
        purge();
        if (keySet == null) {
            keySet = new InnerSet<>(()->new HashIterator<>(entry -> entry.getKey()), (k)->containsKey(k), (k)->removeKey(k));
        }
        return keySet;
    }

    @SuppressWarnings("unchecked")
    public void purge() {
        int lastElementCount = entryCount;
        Entry<K, V> toRemove;
        while ((toRemove = (Entry<K, V>) referenceQueue.poll()) != null) {
            removeEntry(toRemove);
        }
        reclaimedEntryCount += (lastElementCount - entryCount);
    }

    /**
     * Maps the specified key to the specified value.
     *
     * @param key   the key.
     * @param value the value.
     * @return the value of any previous mapping with the specified key or
     * {@code null} if there was no mapping.
     */
    @Override
    public V put(K key, V value) {
        purge();
        int index = 0;
        Entry<K, V> entry;
        if (key != null) {
            index = bucketIndex(keyHash(isEqualityByIdentity, key));
            entry = bucketArray[index];
            while (entry != null && !equalsKey(isEqualityByIdentity, key, entry.getKey())) {
                entry = entry.getNext();
            }
        } else {
            entry = bucketArray[0];
            while (entry != null && !entry.isNull()) {
                entry = entry.getNext();
            }
        }
        if (entry == null) {
            modificationCount++;
            if (++entryCount > threshold) {
                rehash();
                index = bucketIndex(keyHash(isEqualityByIdentity, key));
            }
            entry = createEntry(key, value, referenceQueue);
            entry.setNext(bucketArray[index]);
            bucketArray[index] = entry;
            return null;
        } else {
            return entry.setValue(value);
        }
    }

    private void rehash() {
        assert bucketArray.length > 0;
        Entry<K, V>[] newData = newBucketArray(bucketArray.length);
        for (Entry<K, V> entry : bucketArray) {
            while (entry != null) {
                int index = entry.isNull() ? 0 : bucketIndex(entry.hash());
                Entry<K, V> next = entry.getNext();
                entry.setNext(newData[index]);
                newData[index] = entry;
                entry = next;
            }
        }
        bucketArray = newData;
        this.threshold = computeThreshold();
    }

    /**
     * Removes the mapping with the specified key from this map.
     *
     * @param key the key of the mapping to remove.
     * @return the value of the removed mapping or {@code null} if no mapping
     * for the specified key was found.
     */
    @Override
    public V remove(Object key) {
        purge();
        int index = 0;
        Entry<K, V> entry, last = null;
        if (key != null) {
            index = bucketIndex(keyHash(isEqualityByIdentity, key));
            entry = bucketArray[index];
            while (entry != null && !(key == entry.getKey())) {
                last = entry;
                entry = entry.getNext();
            }
        } else {
            entry = bucketArray[0];
            while (entry != null && !entry.isNull()) {
                last = entry;
                entry = entry.getNext();
            }
        }
        if (entry != null) {
            modificationCount++;
            if (last == null) {
                bucketArray[index] = entry.getNext();
            } else {
                last.setNext(entry.getNext());
            }
            entryCount--;
            return entry.getValue();
        }
        return null;
    }

    private boolean removeEntry(Entry<K, V> toRemove) {
        Entry<K, V> entry, last = null;
        int index = bucketIndex(toRemove.hash());
        entry = bucketArray[index];
        // Ignore queued entries which cannot be found, the user could
        // have removed them before they were queued, i.e. using clear()
        while (entry != null) {
            if (toRemove == entry) {
                modificationCount++;
                if (last == null) {
                    bucketArray[index] = entry.getNext();
                } else {
                    last.setNext(entry.getNext());
                }
                entryCount--;
                break;
            }
            last = entry;
            entry = entry.getNext();
        }

        return (last != null);
    }

    private boolean removeEntry(Object entry) {
        if (containsEntry(entry)) {
            WeakIdentityHashMap.this
                    .remove(((Map.Entry<?, ?>) entry).getKey());
            return true;
        }
        return false;
    }

    private boolean removeKey(Object key) {
        if (containsKey(key)) {
            WeakIdentityHashMap.this.remove(key);
            return true;
        }
        return false;
    }

    /**
     * @return the number of elements in this map.
     */
    @Override
    public int size() {
        purge();
        return entryCount;
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
        purge();
        if (valuesCollection == null) {
            valuesCollection = new ValueCollection<>();
        }
        return valuesCollection;
    }


    private interface Entry<K, V> extends Map.Entry<K, V> {

        default boolean equalsEntry(Object other) {
            if (!(other instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) other;
            Object key = this.getKey();
            return (equalsKey(isEqualityByIdentity(), key, entry.getKey()))
                    && (this.getValue() == null ? this.getValue() == entry.getValue() : this.getValue().equals(entry.getValue()));
        }

        Entry<K, V> getNext();

        void setNext(Entry<K, V> entry);

        int hash();

        boolean isEqualityByIdentity();

        boolean isNull();

    }

    private static final class SoftEntry<K, V> extends SoftReference<K> implements Entry<K, V> {
        final int hash;
        boolean isEqualityByIdentity;
        boolean isNull;
        Entry<K, V> next;
        V value;

        SoftEntry(K key, V object, ReferenceQueue<K> queue, boolean isEqualityByIdentity) {
            super(key, queue);
            this.isNull = key == null;
            this.hash = keyHash(isEqualityByIdentity, key);
            this.value = object;
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
        public boolean isNull() {
            return isNull;
        }

        @Override
        public V setValue(V object) {
            V result = value;
            value = object;
            return result;
        }

        @Override
        public String toString() {
            return super.get() + "=" + value;
        }

    }

    private static final class WeakEntry<K, V> extends WeakReference<K> implements Entry<K, V> {
        final int hash;
        boolean isEqualityByIdentity;
        boolean isNull;
        Entry<K, V> next;
        V value;

        WeakEntry(K key, V object, ReferenceQueue<K> queue, boolean isEqualityByIdentity) {
            super(key, queue);
            this.isNull = key == null;
            this.hash = keyHash(isEqualityByIdentity, key);
            this.value = object;
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
        public boolean isNull() {
            return isNull;
        }

        @Override
        public V setValue(V object) {
            V result = value;
            value = object;
            return result;
        }

        @Override
        public String toString() {
            return super.get() + "=" + value;
        }

    }

    private class HashIterator<R> implements Iterator<R> {
        private final Function<Entry<K, V>, R> entryFuntion;
        private Entry<K, V> currentEntry, nextEntry;
        private K nextKey;
        private int position = 0, expectedModCount;

        HashIterator(Function<Entry<K, V>, R> entryFuntion) {
            this.entryFuntion = entryFuntion;
            expectedModCount = modificationCount;
        }

        @Override
        public boolean hasNext() {
            if (nextEntry != null && (nextKey != null || nextEntry.isNull())) {
                return true;
            }
            while (true) {
                if (nextEntry == null) {
                    while (position < bucketArray.length) {
                        if ((nextEntry = bucketArray[position++]) != null) {
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
            if (expectedModCount == modificationCount) {
                if (hasNext()) {
                    currentEntry = nextEntry;
                    nextEntry = currentEntry.getNext();
                    R result = entryFuntion.apply(currentEntry);
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
            if (expectedModCount == modificationCount) {
                if (currentEntry != null) {
                    removeEntry(currentEntry);
                    currentEntry = null;
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

    class InnerSet<E> extends AbstractSet<E> {

        final Predicate<Object> containsPredicate;
        final Supplier<Iterator<E>> hashIteratorSupplier;
        final Function<Object, Boolean> removeFunction;

        InnerSet(Supplier<Iterator<E>> hashIteratorSupplier, final Predicate<Object> containsPredicate, Function<Object, Boolean> removeFunction) {
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
            return new HashIterator<E>(entry -> (E) entry.getValue());
        }

        @Override
        public int size() {
            return WeakIdentityHashMap.this.size();
        }
    }


}
