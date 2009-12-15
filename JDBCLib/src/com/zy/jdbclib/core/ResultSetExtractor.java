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
public interface ResultSetExtractor<T extends Object> {

	public T extractData(ResultSet rs)  throws SQLException ;
	
}
