package eu.dirk.haase.jdbc.proxy.hybrid;

import eu.dirk.haase.jdbc.proxy.base.JdbcWrapper;

import javax.sql.DataSource;

/**
 * Basis-Klasse f&uuml;r den Sonderfall das ein und dieselbe DataSource-Instanz
 * gleichzeitig mehrere die Interfaces implementieren.
 * <p>
 * Alle Hybrid-Instanzen implementieren zumindest das {@link DataSource}-Interface.
 * Infolgedessen werden die Methoden {@link #equals(Object)}, {@link #hashCode()},
 * {@link #isWrapperFor(Class)} und {@link #unwrap(Class)} nur an Hand
 * des gemeinsamen {@link DataSource}-Objektes ausgef&uuml;hrt.
 */
abstract class AbstractDataSourceHybrid implements JdbcWrapper {

    private final DataSource dataSourceProxy;

    AbstractDataSourceHybrid(final DataSource dataSourceProxy) {
        this.dataSourceProxy = dataSourceProxy;
    }

    /**
     * Liefert {@code true} wenn das Argument-Objekt exakt von derselben Klassen
     * abstammt und das gleiche JDBC-Objekt wie dieses Wrapper-Objekt enth&auml;hlt.
     *
     * @param thatObj das andere Objekt das gepr&uuml;ft werden soll.
     * @return {@code true} wenn dieses Wrapper-Objekt gegen&uuml;ber dem Argument
     * das gleiche JDBC-Objekt enth&auml;hlt.
     */
    @Override
    public final boolean equals(Object thatObj) {
        if (this == thatObj) return true;
        if (thatObj.getClass() != this.getClass()) return false;

        AbstractDataSourceHybrid that = (AbstractDataSourceHybrid) thatObj;

        return dataSourceProxy != null ? dataSourceProxy.equals(that.dataSourceProxy) : that.dataSourceProxy == null;
    }

    public final DataSource getDataSourceProxy() {
        return dataSourceProxy;
    }

    /**
     * Liefert den Hash-Code des JDBC-Objekts, das dieses Wrapper-Objekt enth&auml;hlt.
     *
     * @return den Hash-Code des JDBC-Objekts, das dieses Wrapper-Objekt enth&auml;hlt.
     */
    @Override
    public final int hashCode() {
        return dataSourceProxy != null ? dataSourceProxy.hashCode() : 0;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "{" +
                "delegate=" + dataSourceProxy +
                '}';
    }


}
