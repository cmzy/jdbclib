/**
 * 
 */

package com.zy.jdbclib.core;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @version 1.0
 * @since 1.0
 */
public interface StatementCallback<T extends Object> {

    public T doInStatement(Statement action) throws SQLException;

}
