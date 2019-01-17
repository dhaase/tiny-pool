package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import java.sql.Connection;

public abstract class AbstractPooledConnectionProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, PooledConnection> implements ValidState {

    private final ConnectionPoolDataSource connectionPoolDataSource;

    protected AbstractPooledConnectionProxy(final PooledConnection delegate, final ConnectionPoolDataSource dataSource, final Object[] argumentArray) {
        super(PooledConnection.class, delegate);
        this.connectionPoolDataSource = dataSource;
    }

    /**
     * Liefert das {@link ConnectionPoolDataSource}-Objekt (das dieses Objekt erzeugt hat),
     * welches wahrscheinlich auch ein Proxy-Objekt ist.
     *
     * @return das zugrundeliegende {@link ConnectionPoolDataSource}-Objekt.
     */
    public final ConnectionPoolDataSource getConnectionPoolDataSourceProxy() {
        return connectionPoolDataSource;
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

}
