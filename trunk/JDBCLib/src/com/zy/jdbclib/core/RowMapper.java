/**
 * 
 */
package com.zy.jdbclib.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @version 1.0
 * @since 1.0
 */
public interface RowMapper<T extends Object> {
	
	public T mapRow(ResultSet rs, int rowNum) throws SQLException;
	
}
