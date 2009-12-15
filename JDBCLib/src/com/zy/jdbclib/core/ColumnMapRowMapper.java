package com.zy.jdbclib.core;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @version 1.0
 * @since 1.0
 */
public class ColumnMapRowMapper implements RowMapper<Map<String, Object>> {

	public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Map<String, Object> mapOfColValues = new LinkedHashMap<String, Object>(columnCount);
		for (int i = 1; i <= columnCount; i++) {
			String key = lookupColumnName(rsmd, i);
			Object obj = getResultSetValue(rs, i);
			mapOfColValues.put(key, obj);
		}
		return mapOfColValues;
	}

	public String lookupColumnName(ResultSetMetaData resultSetMetaData,
			int columnIndex) throws SQLException {
		String name = resultSetMetaData.getColumnLabel(columnIndex);
		if (name == null || name.length() < 1) {
			name = resultSetMetaData.getColumnName(columnIndex);
		}
		return name;
	}

	public Object getResultSetValue(ResultSet rs, int index)
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

}
