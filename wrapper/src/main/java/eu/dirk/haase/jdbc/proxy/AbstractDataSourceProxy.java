package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;

public abstract class AbstractDataSourceProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, DataSource> implements ValidState {


    protected AbstractDataSourceProxy(final DataSource delegate) {
        super(delegate);
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
    protected abstract <T extends Connection> T  wrapConnection(Connection delegate, Object... argumentArray);

}
