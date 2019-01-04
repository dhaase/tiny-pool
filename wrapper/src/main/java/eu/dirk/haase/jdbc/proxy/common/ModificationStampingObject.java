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
 * sind, ist es m&ouml;glich fein granulare Sperren einzusetzen, wie zum
 * Beispiel optimistische Sperren (siehe {@link StampedLock}) oder Lese-/Schreib-Sperren
 * (siehe {@link ReentrantReadWriteLock}).
 * <p>
 * Zur genauen Semantik des Stempel-Wertes siehe die Beschreibung von der Methode
 * {@link #modificationStamp()}.
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
     * Verhaltens&auml;nderung des Objektes f&uuml;hrt, muss mit einem eindeutigen
     * Stempel-Wert angezeigt werden.
     * <p>
     * Das bedeutet insbesondere, das sich kein Stempel-Wert wiederholen darf.
     * <p>
     * Andererseits bedeutet ver&auml;nderter Stempel-Wert nicht zwangsl&auml;ufig,
     * das sich der interne Zustand des Objektes genau indem Zeitpunkt ver&auml;ndert
     * hat.
     * Ein ver&auml;nderter Stempel-Wert zeigt daher nur an, das eine &Auml;nderung
     * des Objektzustandes unmittelbar bevor steht.
     * <p>
     * Hintergrund f&uuml;r diese Einschr&auml;nkung ist:
     * Ohne diese Einschr&auml;nkung w&auml;re eine Implementation nur mit dem
     * Einsatz von Sperren m&ouml;glich, da Zustands&auml;nderungen dann nur
     * atomar durchgef&uuml;hrt werden d&uuml;rften. Mit der Konsequenz das
     * auch der Sinn dieses Interfaces konterkariert werden w&uuml;rde.
     * <p>
     * Zusammenfassend liefert der Stempel-Wert folgende Zusicherung:
     * <ul>
     * <li>bei einem <b>unver&auml;nderten</b> Stempel-Wert hat sich der Zustand
     * des Objektes nicht ver&auml;ndert.</li>
     * <li>und bei einem <b>ver&auml;nderten</b> Stempel-Wert steht eine
     * Zustands&auml;nderung des Objektes unmittelbar bevor oder ist
     * bereits durchgef&uuml;hrt worden.</li>
     * </ul>
     * <p>
     * Der Stempel-Wert hat, abgesehen von seiner Eindeutigkeit, keine weitere
     * Eigenschaften. Auch wenn der Stempel-Wert intern meist dadurch gebildet
     * wird, indem er monoton inkrementiert wird, darf deswegen dem Stempel-Wert
     * keine weitere Bedeutungen zugewiesen werden. Es k&ouml;nnen daher auch
     * keine weiteren Aussagen, wie zum Beispiel zeitliche Abfolgen -
     * vorher / nachher, aus dem Stempel-Wert abgelesen werden.
     *
     * @return ein neuer Stempel-Wert bei einer neuen Zustands&auml;nderung.
     */
    long modificationStamp();

}
