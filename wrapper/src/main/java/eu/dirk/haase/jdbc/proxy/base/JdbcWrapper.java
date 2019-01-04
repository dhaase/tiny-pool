package eu.dirk.haase.jdbc.proxy.base;

import java.sql.Wrapper;

/**
 * Marker-Interface um alle Wrapper-Klassen eindeutig als Wrapper-Klassen
 * zu kennzeichnen.
 * <p>
 * Unterst&uuml;tzt in besonderer Weise das Interface {@link Wrapper}.
 * Siehe dazu die Beschreibung von der Methode {@link JdbcProxy#isWrapperFor(Class)}.
 */
public interface JdbcWrapper extends Wrapper {
}
