package eu.dirk.haase.jdbc.proxy.common;

import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

/**
 * Wird von einem Objekt implementiert um seine interne Zustands&auml;nderung
 * f&uuml;r andere Objekten sichtbar zu machen.
 * <p>
 * Dieses Interface wird meist von komplexen Datenstrukturen wie Trees oder
 * Hash-Maps implementiert, da bei diesen Objekten die internen Zustands&auml;nderungen
 * von Aussen nicht oder nur sehr Aufw&auml;ndig ermittelbar sind.
 * <p>
 * Da interne Zustands&auml;nderungen mit diesem Interface sehr leicht abfragbar
 * sind, ist es m&ouml;glich geworden fein granulare Sperren einzusetzen, wie zum
 * Beispiel optimistische Sperren (siehe {@link StampedLock}) oder Lese-/Schreib-Sperren
 * (siehe {@link ReentrantReadWriteLock}).
 * <p>
 * Zum besseren Verst&auml;ndnis welchen Nutzen dieses Interface bietet,
 * hier ein typischer Algorithmus an Hand einer hypothetischen Methode:
 * <pre><code>
 * // Setzt den Wert atomar auf den angegebenen aktualisierten Wert
 * // wenn der aktuelle Wert gleich dem erwarteten Wert ist.
 * public boolean compareAndSet(final Lock readLock, final Lock writeLock, V expect, V update) {
 *     Lock currLock = null;
 *     try {
 *         readLock.lock();
 *         currLock = readLock;
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
     * <p>
     * Jede interne Zustands&auml;nderung, die zu einer von Aussen erkennbaren
     * Verhaltens&auml;nderung des Objektes f&uuml;hrt, muss in einem eindeutigen
     * Stempel-Wert wiedergespiegelt werden.
     * <p>
     * Das bedeutet insbesondere, das sich kein Stempel-Wert wiederholen darf.
     * <p>
     * Der Stempel-Wert hat abgesehen von seiner Eindeutigkeit keine weiteren
     * Eigenschaften. Auch wenn der Stempel-Wert intern meist dadurch gebildet
     * wird indem er monoton inkrementiert wird, darf dem Stempel-Wert keine
     * weitere Bedeutungen zugewiesen werden. Es k&ouml;nnen daher auch keine
     * weiteren Aussagen, wie zum Beispiel zeitliche Abfolgen - vorher / nachher,
     * aus dem Stempel-Wert abgelesen werden.
     *
     * @return ein neuer Stempel-Wert bei einer neuen Zustands&auml;nderung.
     */
    long modificationStamp();

}
