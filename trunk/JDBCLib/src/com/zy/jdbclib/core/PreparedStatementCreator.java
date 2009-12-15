/**
 * 
 */
package com.zy.jdbclib.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @version 1.0
 * @since 1.0
 */
public interface PreparedStatementCreator {
	
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException;
	
}
