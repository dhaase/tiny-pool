package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.ConcurrentFactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;
import eu.dirk.haase.jdbc.proxy.common.WeakIdentityHashMap;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.Connection;

public abstract class AbstractXADataSourceProxy extends ConcurrentFactoryJdbcProxy<WeakIdentityHashMap<Object, Object>, XADataSource> implements ValidState {


    protected AbstractXADataSourceProxy(final XADataSource delegate) {
        super(XADataSource.class, delegate);
    }

    /**
     * Dekoriert ein {@link XAConnection}-Objekt, das bedeutet: es wird in ein anderes
     * Objekt eingepackt (welches selbst das Interface {@link XAConnection} implementiert).
     * <p>
     * Die Implementation dieser Methode wird generiert und muss daher nicht implementiert
     * werden.
     *
     * @param delegate      das interne {@link XAConnection}-Objekt das dekoriert werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte {@link XAConnection}-Objekt.
     */
    protected abstract <T extends XAConnection> T wrapXAConnection(XAConnection delegate, Object... argumentArray);

}
