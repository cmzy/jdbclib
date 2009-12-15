/**
 * 
 */
package com.zy.jdbclib.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @version 1.0
 * @since 1.0
 */
public interface PreparedStatementCallback<T extends Object> {
	
	public T doInPreparedStatement(PreparedStatement ps) throws SQLException;
	
}
