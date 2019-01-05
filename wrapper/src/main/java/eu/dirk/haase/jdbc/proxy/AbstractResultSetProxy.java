package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.JdbcProxy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public abstract class AbstractResultSetProxy extends JdbcProxy<ResultSet> implements CloseState {

    private final ResultSet delegate;
    private final Statement statement;

    protected AbstractResultSetProxy(final ResultSet delegate, final Statement statement, final Object[] argumentArray) {
        super(delegate);
        this.statement = statement;
        this.delegate = delegate;
    }

    /**
     * Liefert das {@link Statement}-Objekt (das dieses Objekt erzeugt hat),
     * welches wahrscheinlich auch ein Proxy-Objekt ist.
     *
     * @return das zugrundeliegende {@link Statement}-Objekt.
     */
    public final Statement getStatementProxy() {
        return statement;
    }


}
