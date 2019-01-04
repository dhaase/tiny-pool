package eu.dirk.haase.jdbc.proxy.base;

import java.sql.SQLException;

public interface ValidState {

    boolean isValid(int timeoutSeconds) throws SQLException;

}
