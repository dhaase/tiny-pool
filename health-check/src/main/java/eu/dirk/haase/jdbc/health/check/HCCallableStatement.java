package eu.dirk.haase.jdbc.health.check;

import eu.dirk.haase.jdbc.proxy.AbstractCallableStatementProxy;
import eu.dirk.haase.jdbc.proxy.AbstractPreparedStatementProxy;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class HCCallableStatement extends HCPreparedStatement implements CallableStatement {

    protected HCCallableStatement(CallableStatement delegate, Connection connection, Object[] argumentArray) {
        super(delegate, connection, argumentArray);
    }


}
