
package com.zy.jdbclib.dataSource;

import java.io.PrintWriter;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractDataSource implements DataSource {

    /** Logger available to subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Returns 0, indicating the default system timeout is to be used.
     */
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    /**
     * Setting a login timeout is not supported.
     */
    public void setLoginTimeout(int timeout) throws SQLException {
        throw new UnsupportedOperationException("setLoginTimeout");
    }

    /**
     * LogWriter methods are not supported.
     */
    public PrintWriter getLogWriter() {
        throw new UnsupportedOperationException("getLogWriter");
    }

    /**
     * LogWriter methods are not supported.
     */
    public void setLogWriter(PrintWriter pw) throws SQLException {
        throw new UnsupportedOperationException("setLogWriter");
    }

}
