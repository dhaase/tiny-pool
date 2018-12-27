package eu.dirk.haase.jdbc.proxy.base;

import java.sql.SQLException;

public interface CloseState {

    boolean isClosed() throws SQLException;

}
