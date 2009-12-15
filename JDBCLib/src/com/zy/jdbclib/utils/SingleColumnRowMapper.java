package com.zy.jdbclib.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.zy.jdbclib.core.JDBCException;
import com.zy.jdbclib.core.RowMapper;

/**
 * 
 * @version 1.0
 * @since 1.0
 */
public class SingleColumnRowMapper<T extends Object> implements RowMapper<T> {

	private Class<T> requiredType;

	// public SingleColumnRowMapper() {
	// }

	public SingleColumnRowMapper(Class<T> requiredType) {
		this.requiredType = requiredType;
	}

	@SuppressWarnings("unchecked")
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		// Validate column count.
		ResultSetMetaData rsmd = rs.getMetaData();
		int nrOfColumns = rsmd.getColumnCount();
		if (nrOfColumns != 1) {
			throw new JDBCException(
					"the number of column of ResultSet must be 1");
		}

		// Extract column value from JDBC ResultSet.
		Object result = getColumnValue(rs, 1, this.requiredType);
		if (result != null && this.requiredType != null
				&& !this.requiredType.isInstance(result)) {
			// Extracted value does not match already: try to convert it.
			try {
				return convertValueToRequiredType(result, this.requiredType);
			} catch (IllegalArgumentException ex) {
				throw new JDBCException(
						"Type mismatch affecting row number " + rowNum
								+ " and column type '"
								+ rsmd.getColumnTypeName(1) + "': "
								+ ex.getMessage());
			}
		}
		return (T) result;
	}

	protected Object getColumnValue(ResultSet rs, int index,
			Class<T> requiredType) throws SQLException {
		if (requiredType != null) {
			return JdbcUtils.getResultSetValue(rs, index, requiredType);
		} else {
			// No required type specified -> perform default extraction.
			return JdbcUtils.getResultSetValue(rs, index);
		}
	}

	@SuppressWarnings("unchecked")
	protected T convertValueToRequiredType(Object value, Class<T> requiredType) {
		if (String.class.equals(requiredType)) {
			return (T) value.toString();
		} else if (Number.class.isAssignableFrom(requiredType)) {
			if (value instanceof Number) {
				// Convert original Number to target Number class.
				return (T) convertNumberToTargetClass(((Number) value),
						requiredType);
			} else {
				// Convert stringified value to target Number class.
				return (T) parseNumber(value.toString(), requiredType);
			}
		} else {
			throw new IllegalArgumentException("Value [" + value
					+ "] is of type [" + value.getClass().getName()
					+ "] and cannot be converted to required type ["
					+ requiredType.getName() + "]");
		}
	}

	@SuppressWarnings("unchecked")
	public Number convertNumberToTargetClass(Number number, Class targetClass)
			throws IllegalArgumentException {

		if (targetClass.isInstance(number)) {
			return number;
		} else if (targetClass.equals(Byte.class)) {
			long value = number.longValue();
			if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return new Byte(number.byteValue());
		} else if (targetClass.equals(Short.class)) {
			long value = number.longValue();
			if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return new Short(number.shortValue());
		} else if (targetClass.equals(Integer.class)) {
			long value = number.longValue();
			if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return new Integer(number.intValue());
		} else if (targetClass.equals(Long.class)) {
			return new Long(number.longValue());
		} else if (targetClass.equals(BigInteger.class)) {
			if (number instanceof BigDecimal) {
				// do not lose precision - use BigDecimal's own conversion
				return ((BigDecimal) number).toBigInteger();
			} else {
				// original value is not a Big* number - use standard long
				// conversion
				return BigInteger.valueOf(number.longValue());
			}
		} else if (targetClass.equals(Float.class)) {
			return new Float(number.floatValue());
		} else if (targetClass.equals(Double.class)) {
			return new Double(number.doubleValue());
		} else if (targetClass.equals(BigDecimal.class)) {
			// always use BigDecimal(String) here to avoid unpredictability of
			// BigDecimal(double)
			// (see BigDecimal javadoc for details)
			return new BigDecimal(number.toString());
		} else {
			throw new IllegalArgumentException("Could not convert number ["
					+ number + "] of type [" + number.getClass().getName()
					+ "] to unknown target class [" + targetClass.getName()
					+ "]");
		}
	}

	@SuppressWarnings("unchecked")
	private void raiseOverflowException(Number number, Class targetClass) {
		throw new IllegalArgumentException("Could not convert number ["
				+ number + "] of type [" + number.getClass().getName()
				+ "] to target class [" + targetClass.getName() + "]: overflow");
	}

	@SuppressWarnings("unchecked")
	public Number parseNumber(String text, Class targetClass) {

		String trimmed = trimAllWhitespace(text);

		if (targetClass.equals(Byte.class)) {
			return (isHexNumber(trimmed) ? Byte.decode(trimmed) : Byte
					.valueOf(trimmed));
		} else if (targetClass.equals(Short.class)) {
			return (isHexNumber(trimmed) ? Short.decode(trimmed) : Short
					.valueOf(trimmed));
		} else if (targetClass.equals(Integer.class)) {
			return (isHexNumber(trimmed) ? Integer.decode(trimmed) : Integer
					.valueOf(trimmed));
		} else if (targetClass.equals(Long.class)) {
			return (isHexNumber(trimmed) ? Long.decode(trimmed) : Long
					.valueOf(trimmed));
		} else if (targetClass.equals(BigInteger.class)) {
			return (isHexNumber(trimmed) ? decodeBigInteger(trimmed)
					: new BigInteger(trimmed));
		} else if (targetClass.equals(Float.class)) {
			return Float.valueOf(trimmed);
		} else if (targetClass.equals(Double.class)) {
			return Double.valueOf(trimmed);
		} else if (targetClass.equals(BigDecimal.class)
				|| targetClass.equals(Number.class)) {
			return new BigDecimal(trimmed);
		} else {
			throw new IllegalArgumentException("Cannot convert String [" + text
					+ "] to target class [" + targetClass.getName() + "]");
		}
	}

	private BigInteger decodeBigInteger(String value) {
		int radix = 10;
		int index = 0;
		boolean negative = false;

		// Handle minus sign, if present.
		if (value.startsWith("-")) {
			negative = true;
			index++;
		}

		// Handle radix specifier, if present.
		if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
			index += 2;
			radix = 16;
		} else if (value.startsWith("#", index)) {
			index++;
			radix = 16;
		} else if (value.startsWith("0", index) && value.length() > 1 + index) {
			index++;
			radix = 8;
		}

		BigInteger result = new BigInteger(value.substring(index), radix);
		return (negative ? result.negate() : result);
	}

	private boolean isHexNumber(String value) {
		int index = (value.startsWith("-") ? 1 : 0);
		return (value.startsWith("0x", index) || value.startsWith("0X", index) || value
				.startsWith("#", index));
	}

	private String trimAllWhitespace(String str) {
		if (str == null || str.length() <= 0) {
			return str;
		}
		StringBuffer buf = new StringBuffer(str);
		int index = 0;
		while (buf.length() > index) {
			if (Character.isWhitespace(buf.charAt(index))) {
				buf.deleteCharAt(index);
			} else {
				index++;
			}
		}
		return buf.toString();
	}

}
