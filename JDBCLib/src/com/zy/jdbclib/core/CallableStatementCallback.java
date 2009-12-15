/**
 * 
 */
package com.zy.jdbclib.core;

import java.sql.CallableStatement;
import java.sql.SQLException;

/**
 * @version 1.0
 * @since 1.0
 */
public interface CallableStatementCallback {

	public Object doInCallableStatement(CallableStatement cs)
			throws SQLException;

}
