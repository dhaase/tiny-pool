package eu.dirk.haase.jdbc.proxy.generate;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Generiert JDBC-Wrapper Klassen die von den angegebenen abstrakten Klassen
 * abgeleitet werden sollen (siehe {@link #generate(java.util.Map)}).
 */
public interface Generator {

    /**
     * Liefert die Standard-Instanz des Generators.
     *
     * @return die Standard-Instanz des Generators.
     */
    static Generator instance() {
        return GeneratorJavassist.getSingleton();
    }

    /**
     * Generiert JDBC-Wrapper Klassen die von den angegebenen abstrakten Klassen abgeleitet werden.
     * <p>
     * <b>Hinweis:</b> Diese Methode kann nebenl&auml;ufig ausgef&uuml;hrt werden.
     *
     * @param iface2CustomClassMap eine Map mit abstrakten Klassen von denen die JDBC-Wrapper
     *                             Klassen abgeleitet werden sollen.
     * @param classNameFun         Funktion um die neuen vollqualifizierten Klassennamen zu erzeugen.
     * @return eine Map mit generierten konkreten JDBC-Wrapper Klassen.
     * @see #generate(java.util.Map)
     */
    Map<Class<?>, Object> generate(final Map<Class<?>, Class<?>> iface2CustomClassMap, final BiFunction<String, Class<?>, String> classNameFun);

    /**
     * Generiert JDBC-Wrapper Klassen die von den angegebenen abstrakten Klassen abgeleitet werden.
     * <p>
     * Die generierten Klassen werden in ein Subpackage '{@code gen}' unter dem Package
     * der Super-Klasse von der jeweilig generierten Klasse gesetzt.
     * <p>
     * <b>Hinweis:</b> Diese Methode kann nebenl&auml;ufig ausgef&uuml;hrt werden.
     *
     * @param iface2CustomClassMap eine Map mit abstrakten Klassen von denen die JDBC-Wrapper
     *                             Klassen abgeleitet werden sollen.
     * @return eine Map mit generierten konkreten JDBC-Wrapper Klassen.
     * @see #generate(java.util.Map, java.util.function.BiFunction)
     */
    Map<Class<?>, Object> generate(final Map<Class<?>, Class<?>> iface2CustomClassMap);

}
