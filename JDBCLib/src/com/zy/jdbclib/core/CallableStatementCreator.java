/**
 * 
 */
package com.zy.jdbclib.core;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @version 1.0
 * @since 1.0
 */
public interface CallableStatementCreator {

	public CallableStatement createCallableStatement(Connection conn) throws SQLException;

}
