package com.zy.jdbclib.utils;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zy.jdbclib.core.JDBCException;

/**
 * Generic utility methods for working with JDBC. Mainly for internal use within
 * the framework, but also useful for custom JDBC access code.
 * 
 * @version 1.0
 * @since 1.0
 */
public abstract class JdbcUtils {

	private static final Log log = LogFactory.getLog(JdbcUtils.class);

	/**
	 * Close the given JDBC Connection and ignore any thrown exception. This is
	 * useful for typical finally blocks in manual JDBC code.
	 * 
	 * @param con
	 *            the JDBC Connection to close (may be <code>null</code>)
	 */
	public static void closeConnection(Connection con) {
		if (con != null) {
			try {
				con.close();
			} catch (SQLException ex) {
				log.debug("Could not close JDBC Connection", ex);
			} catch (Throwable ex) {
				// We don't trust the JDBC driver: It might throw
				// RuntimeException or Error.
				log
						.debug(
								"Unexpected exception on closing JDBC Connection",
								ex);
			}
		}
	}

	public static String lookupColumnName(ResultSetMetaData resultSetMetaData,
			int columnIndex) throws SQLException {
		String name = resultSetMetaData.getColumnLabel(columnIndex);
		if (name == null || name.length() < 1) {
			name = resultSetMetaData.getColumnName(columnIndex);
		}
		return name;
	}

	public static Object getResultSetValue(ResultSet rs, int index)
			throws SQLException {
		Object obj = rs.getObject(index);
		String className = null;
		if (obj != null) {
			className = obj.getClass().getName();
		}
		if (obj instanceof Blob) {
			obj = rs.getBytes(index);
		} else if (obj instanceof Clob) {
			obj = rs.getString(index);
		} else if (className != null
				&& ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ"
						.equals(className))) {
			obj = rs.getTimestamp(index);
		} else if (className != null && className.startsWith("oracle.sql.DATE")) {
			String metaDataClassName = rs.getMetaData().getColumnClassName(
					index);
			if ("java.sql.Timestamp".equals(metaDataClassName)
					|| "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
				obj = rs.getTimestamp(index);
			} else {
				obj = rs.getDate(index);
			}
		} else if (obj != null && obj instanceof java.sql.Date) {
			if ("java.sql.Timestamp".equals(rs.getMetaData()
					.getColumnClassName(index))) {
				obj = rs.getTimestamp(index);
			}
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	public static Object getResultSetValue(ResultSet rs, int index,
			Class requiredType) throws SQLException {
		if (requiredType == null) {
			return getResultSetValue(rs, index);
		}

		Object value = null;
		boolean wasNullCheck = false;

		// Explicitly extract typed value, as far as possible.
		if (String.class.equals(requiredType)) {
			value = rs.getString(index);
		} else if (boolean.class.equals(requiredType)
				|| Boolean.class.equals(requiredType)) {
			value = Boolean.valueOf(rs.getBoolean(index));
			wasNullCheck = true;
		} else if (byte.class.equals(requiredType)
				|| Byte.class.equals(requiredType)) {
			value = new Byte(rs.getByte(index));
			wasNullCheck = true;
		} else if (short.class.equals(requiredType)
				|| Short.class.equals(requiredType)) {
			value = new Short(rs.getShort(index));
			wasNullCheck = true;
		} else if (int.class.equals(requiredType)
				|| Integer.class.equals(requiredType)) {
			value = new Integer(rs.getInt(index));
			wasNullCheck = true;
		} else if (long.class.equals(requiredType)
				|| Long.class.equals(requiredType)) {
			value = new Long(rs.getLong(index));
			wasNullCheck = true;
		} else if (float.class.equals(requiredType)
				|| Float.class.equals(requiredType)) {
			value = new Float(rs.getFloat(index));
			wasNullCheck = true;
		} else if (double.class.equals(requiredType)
				|| Double.class.equals(requiredType)
				|| Number.class.equals(requiredType)) {
			value = new Double(rs.getDouble(index));
			wasNullCheck = true;
		} else if (byte[].class.equals(requiredType)) {
			value = rs.getBytes(index);
		} else if (java.sql.Date.class.equals(requiredType)) {
			value = rs.getDate(index);
		} else if (java.sql.Time.class.equals(requiredType)) {
			value = rs.getTime(index);
		} else if (java.sql.Timestamp.class.equals(requiredType)
				|| java.util.Date.class.equals(requiredType)) {
			value = rs.getTimestamp(index);
		} else if (BigDecimal.class.equals(requiredType)) {
			value = rs.getBigDecimal(index);
		} else if (Blob.class.equals(requiredType)) {
			value = rs.getBlob(index);
		} else if (Clob.class.equals(requiredType)) {
			value = rs.getClob(index);
		} else {
			// Some unknown type desired -> rely on getObject.
			value = getResultSetValue(rs, index);
		}

		// Perform was-null check if demanded (for results that the
		// JDBC driver returns as primitives).
		if (wasNullCheck && value != null && rs.wasNull()) {
			value = null;
		}
		return value;
	}

	/**
	 * Close the given JDBC Statement and ignore any thrown exception. This is
	 * useful for typical finally blocks in manual JDBC code.
	 * 
	 * @param stmt
	 *            the JDBC Statement to close (may be <code>null</code>)
	 */
	public static void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException ex) {
				log.debug("Could not close JDBC Statement", ex);
			} catch (Throwable ex) {
				// We don't trust the JDBC driver: It might throw
				// RuntimeException or Error.
				log.debug("Unexpected exception on closing JDBC Statement", ex);
			}
		}
	}

	/**
	 * Close the given JDBC ResultSet and ignore any thrown exception. This is
	 * useful for typical finally blocks in manual JDBC code.
	 * 
	 * @param rs
	 *            the JDBC ResultSet to close (may be <code>null</code>)
	 */
	public static void closeResultSet(ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException ex) {
				log.debug("Could not close JDBC ResultSet", ex);
			} catch (Throwable ex) {
				// We don't trust the JDBC driver: It might throw
				// RuntimeException or Error.
				log.debug("Unexpected exception on closing JDBC ResultSet", ex);
			}
		}
	}

	public static boolean supportsBatchUpdates(Connection con) {
		try {
			DatabaseMetaData dbmd = con.getMetaData();
			if (dbmd != null) {
				if (dbmd.supportsBatchUpdates()) {
					log.debug("JDBC driver supports batch updates");
					return true;
				} else {
					log.debug("JDBC driver does not support batch updates");
				}
			}
		} catch (SQLException ex) {
			log
					.debug(
							"JDBC driver 'supportsBatchUpdates' method threw exception",
							ex);
		} catch (AbstractMethodError err) {
			log
					.debug(
							"JDBC driver does not support JDBC 2.0 'supportsBatchUpdates' method",
							err);
		}
		return false;
	}

	public static <T extends Object> T requiredSingleResult(
			Collection<T> results) throws JDBCException {
		int size = (results != null ? results.size() : 0);
		if (size == 0) {
			throw new JDBCException("The result is null.");
		}
		if (results.size() > 1) {
			throw new JDBCException("The result'size is not 1.");
		}
		return results.iterator().next();
	}

}
