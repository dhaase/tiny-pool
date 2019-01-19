package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.ResultSet;

public abstract class AbstractXAConnectionProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, XAConnection> implements ValidState, XAConnection {

    private final XADataSource xaDataSource;

    protected AbstractXAConnectionProxy(final XAConnection delegate, final XADataSource xaDataSource, final Object[] argumentArray) {
        super(XAConnection.class, delegate);
        this.xaDataSource = xaDataSource;
    }

    /**
     * Liefert das {@link XADataSource}-Objekt (das dieses Objekt erzeugt hat),
     * welches wahrscheinlich auch ein Proxy-Objekt ist.
     *
     * @return das zugrundeliegende {@link XADataSource}-Objekt.
     */
    public XADataSource getXADataSourceProxy() {
        return xaDataSource;
    }

    /**
     * Dekoriert ein {@link Connection}-Objekt, das bedeutet: es wird in ein anderes
     * Objekt eingepackt (welches selbst das Interface {@link Connection} implementiert).
     * <p>
     * Die Implementation dieser Methode wird generiert und muss daher nicht implementiert
     * werden.
     *
     * @param delegate      das interne {@link Connection}-Objekt das dekoriert werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte {@link Connection}-Objekt.
     */
    protected abstract <T extends Connection> T wrapConnection(Connection delegate, Object... argumentArray);

    /**
     * Dekoriert ein {@link XAResource}-Objekt, das bedeutet: es wird in ein anderes
     * Objekt eingepackt (welches selbst das Interface {@link XAResource} implementiert).
     * <p>
     * Die Implementation dieser Methode wird generiert und muss daher nicht implementiert
     * werden.
     *
     * @param delegate      das interne {@link XAResource}-Objekt das dekoriert werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte {@link XAResource}-Objekt.
     */
    protected abstract <T extends XAResource> T wrapXAResource(XAResource delegate, Object... argumentArray);

}
