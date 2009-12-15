/**
 * 
 */
package com.zy.jdbclib.core;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @version 1.0
 * @since 1.0
 */
public interface ConnectionCallback<T extends Object> {

	public T doInConnection(Connection conn) throws SQLException;

}
