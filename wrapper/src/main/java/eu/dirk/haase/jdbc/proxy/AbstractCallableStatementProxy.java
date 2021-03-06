package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;

public abstract class AbstractCallableStatementProxy extends FactoryJdbcProxy<CallableStatement> implements CloseState, CallableStatement {

    private final Connection connection;
    private final CallableStatement delegate;

    protected AbstractCallableStatementProxy(CallableStatement delegate, Connection connection, final Object[] argumentArray) {
        super(CallableStatement.class, delegate);
        this.connection = connection;
        this.delegate = delegate;
    }

    /**
     * Liefert das {@link Connection}-Objekt (das dieses Objekt erzeugt hat),
     * welches wahrscheinlich auch ein Proxy-Objekt ist.
     *
     * @return das zugrundeliegende {@link Connection}-Objekt.
     */
    public final Connection getConnectionProxy() {
        return connection;
    }

    /**
     * Dekoriert ein {@link ResultSet}-Objekt, das bedeutet: es wird in ein anderes
     * Objekt eingepackt (welches selbst das Interface {@link ResultSet} implementiert).
     * <p>
     * Die Implementation dieser Methode wird generiert und muss daher nicht implementiert
     * werden.
     *
     * @param delegate      das interne {@link ResultSet}-Objekt das dekoriert werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte {@link ResultSet}-Objekt.
     */
    protected abstract <T extends ResultSet> T wrapResultSet(ResultSet delegate, Object... argumentArray);

}
