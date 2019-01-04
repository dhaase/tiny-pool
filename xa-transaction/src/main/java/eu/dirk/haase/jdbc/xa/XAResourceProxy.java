package eu.dirk.haase.jdbc.xa;

import eu.dirk.haase.jdbc.proxy.AbstractXAResourceProxy;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public abstract class XAResourceProxy extends AbstractXAResourceProxy implements XAResource {

    protected XAResourceProxy(XAResource delegate, XAConnection xaConnection, Object[] argumentArray) {
        super(delegate, xaConnection, argumentArray);
    }

}
