package eu.dirk.haase.jdbc.proxy;

import eu.dirk.haase.jdbc.proxy.base.CloseState;
import eu.dirk.haase.jdbc.proxy.base.FactoryJdbcProxy;
import eu.dirk.haase.jdbc.proxy.base.ValidState;

import javax.sql.DataSource;
import javax.sql.PooledConnection;
import java.sql.*;

public abstract class AbstractConnectionProxy extends FactoryJdbcProxy<Connection> implements CloseState, ValidState {

    private final DataSource dataSource;
    private final Connection delegate;

    protected AbstractConnectionProxy(final Connection delegate, final DataSource dataSource, final Object[] argumentArray) throws SQLException {
        super(delegate);
        this.dataSource = dataSource;
        this.delegate = delegate;
        if (this.delegate.getAutoCommit()) {
            this.delegate.setAutoCommit(false);
        }
    }

    /**
     * Liefert das {@link DataSource}-Objekt (das dieses Objekt erzeugt hat),
     * welches wahrscheinlich auch ein Proxy-Objekt ist.
     *
     * @return das zugrundeliegende {@link DataSource}-Objekt.
     */
    public final DataSource getDataSourceProxy() {
        return dataSource;
    }

    public final void setAutoCommit(boolean autoCommit) throws SQLException {
        throw new SQLException("AutoCommit is not allowed for a transaction managed Connection.");
    }

    /**
     * Dekoriert ein {@link CallableStatement}-Objekt, das bedeutet: es wird in ein anderes
     * Objekt eingepackt (welches selbst das Interface {@link CallableStatement} implementiert).
     * <p>
     * Die Implementation dieser Methode wird generiert und muss daher nicht implementiert
     * werden.
     *
     * @param delegate      das interne {@link CallableStatement}-Objekt das dekoriert werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte {@link CallableStatement}-Objekt.
     */
    protected abstract <T extends CallableStatement> T wrapCallableStatement(CallableStatement delegate, Object... argumentArray);

    /**
     * Dekoriert ein {@link PreparedStatement}-Objekt, das bedeutet: es wird in ein anderes
     * Objekt eingepackt (welches selbst das Interface {@link PreparedStatement} implementiert).
     * <p>
     * Die Implementation dieser Methode wird generiert und muss daher nicht implementiert
     * werden.
     *
     * @param delegate      das interne {@link PreparedStatement}-Objekt das dekoriert werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte {@link PreparedStatement}-Objekt.
     */
    protected abstract <T extends PreparedStatement> T wrapPreparedStatement(PreparedStatement delegate, Object... argumentArray);

    /**
     * Dekoriert ein {@link Statement}-Objekt, das bedeutet: es wird in ein anderes
     * Objekt eingepackt (welches selbst das Interface {@link Statement} implementiert).
     * <p>
     * Die Implementation dieser Methode wird generiert und muss daher nicht implementiert
     * werden.
     *
     * @param delegate      das interne {@link Statement}-Objekt das dekoriert werden soll.
     * @param argumentArray alle Parameter die urspr&uuml;nglich zum
     *                      Erzeugen des internen Objektes verwendet wurden.
     * @return das dekorierte {@link Statement}-Objekt.
     */
    protected abstract <T extends Statement> T wrapStatement(Statement delegate, Object... argumentArray);

}
