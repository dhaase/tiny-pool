package eu.dirk.haase.jdbc.proxy.common;

/**
 * Wird von einem Objekt implementiert um seine interne Zustands&auml;nderung
 * f&uuml;r andere Objekten sichtbar zu machen.
 */
public interface ModificationStampingObject {

    /**
     * Liefert bei jeder Zustandsver&auml;nderung einen neuen Stempel-Wert.
     *
     * @return der neuen Stempel-Wert bei einer neuen Zustands&auml;nderung.
     */
    long modificationStamp();

}
