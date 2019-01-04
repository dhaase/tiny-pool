package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractPooledConnectionProxy;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public abstract class PooledConnectionProxy extends AbstractPooledConnectionProxy implements PooledConnection {

    protected PooledConnectionProxy(PooledConnection delegate, ConnectionPoolDataSource dataSource, Object[] argumentArray) {
        super(delegate, dataSource, argumentArray);
    }

}
