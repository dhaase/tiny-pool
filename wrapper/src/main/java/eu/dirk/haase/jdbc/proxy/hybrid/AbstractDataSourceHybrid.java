package eu.dirk.haase.jdbc.proxy.hybrid;

import javax.sql.DataSource;

abstract class AbstractDataSourceHybrid {
    private final DataSource dataSourceProxy;

    AbstractDataSourceHybrid(final DataSource dataSourceProxy) {
        this.dataSourceProxy = dataSourceProxy;
    }

    @Override
    public final boolean equals(Object thatObj) {
        if (this == thatObj) return true;
        if (thatObj.getClass() != this.getClass()) return false;

        AbstractDataSourceHybrid that = (AbstractDataSourceHybrid) thatObj;

        return dataSourceProxy != null ? dataSourceProxy.equals(that.dataSourceProxy) : that.dataSourceProxy == null;
    }

    public final DataSource getDataSourceProxy() {
        return dataSourceProxy;
    }

    @Override
    public final int hashCode() {
        return dataSourceProxy != null ? dataSourceProxy.hashCode() : 0;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "{" +
                "delegate=" + dataSourceProxy +
                '}';
    }


}
