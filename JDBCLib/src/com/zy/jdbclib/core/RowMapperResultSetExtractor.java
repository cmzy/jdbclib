package com.zy.jdbclib.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.zy.jdbclib.utils.Assert;

/**
 * 
 * @version 1.0
 * @since 1.0
 */
public class RowMapperResultSetExtractor<T extends Object> implements
		ResultSetExtractor<List<T>> {

	private final RowMapper<T> rowMapper;

	private final int rowsExpected;

	public RowMapperResultSetExtractor(RowMapper<T> rowMapper) {
		this(rowMapper, 0);
	}

	public RowMapperResultSetExtractor(RowMapper<T> rowMapper, int rowsExpected) {
		Assert.notNull(rowMapper, "RowMapper is required");
		this.rowMapper = rowMapper;
		this.rowsExpected = rowsExpected;
	}

	public List<T> extractData(ResultSet rs) throws SQLException {
		List<T> results = (this.rowsExpected > 0 ? new ArrayList<T>(
				this.rowsExpected) : new ArrayList<T>());
		int rowNum = 0;
		while (rs.next()) {
			results.add(this.rowMapper.mapRow(rs, rowNum++));
		}
		return results;
	}

}
