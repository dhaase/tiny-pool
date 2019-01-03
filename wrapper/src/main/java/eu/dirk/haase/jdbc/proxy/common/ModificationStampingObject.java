package eu.dirk.haase.jdbc.proxy.common;

/**
 * Wird von einem Objekt implementiert um seine interne Zustands&auml;nderung
 * f&uuml;r andere Objekten sichtbar zu machen.
 * <p>
 * Ein typischer Algorithmus an Hand einer hypothetischen Methode:
 * <pre><code>
 * // Setzt den Wert atomar auf den angegebenen aktualisierten Wert
 * // wenn der aktuelle Wert gleich dem erwarteten Wert ist.
 * public boolean compareAndSet(final Lock readLock, final Lock writeLock, V expect, V update) {
 *     Lock currLock = readLock;
 *     try {
 *         currLock.lock();
 *         // Lese den aktuellen Wert
 *         T currValue = readValue();
 *         if (expect.equals(currValue)) {
 *             long expectedModStamp = delegate.modificationStamp();
 *             readLock.unlock();
 *             currLock = null;
 *             // Hier entsteht eine Luecke und damit eine Race-Condition,
 *             // da nun keine Sperre, weder Lese- noch Schreib-Sperre, gesetzt
 *             // ist.
 *             writeLock.lock();
 *             currLock = writeLock;
 *             // Die exklusive Schreib-Sperre ist jetzt gesetzt. Jetzt muss
 *             // nachfolgend nochmal geprueft werden ob sich zwischenzeitlich
 *             // das Objekt veraendert hat (wegen der Race-Condition, siehe oben):
 *             if (expectedModStamp == delegate.modificationStamp()) {
 *                 // Ersetze den neuen Wert
 *                 updateValue(update);
 *                 return true;
 *             } else {
 *                 ...
 *             }
 *         }
 *         return false;
 *     } finally {
 *         if (currLock != null) {
 *             currLock.unlock();
 *         }
 *     }
 * }
 * </code></pre>
 */
public interface ModificationStampingObject {

    /**
     * Liefert bei jeder Zustandsver&auml;nderung einen neuen Stempel-Wert.
     *
     * @return der neuen Stempel-Wert bei einer neuen Zustands&auml;nderung.
     */
    long modificationStamp();

}
