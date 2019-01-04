package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractXAConnectionProxy;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

public abstract class XAConnectionProxy extends AbstractXAConnectionProxy implements XAConnection {

    protected XAConnectionProxy(XAConnection delegate, XADataSource xaDataSource, Object[] argumentArray) {
        super(delegate, xaDataSource, argumentArray);
    }

}
