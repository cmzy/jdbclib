/**
 * 
 */
package com.zy.jdbclib.core;

/**
 * @version 1.0
 * @since 1.0
 */
public class JDBCException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2387478767513182201L;

	/**
	 * 
	 */
	public JDBCException() {
	}

	/**
	 * @param message
	 */
	public JDBCException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public JDBCException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public JDBCException(String message, Throwable cause) {
		super(message, cause);
	}

}
