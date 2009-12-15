package com.zy.jdbclib.example;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.zy.jdbclib.JdbcTemplate;
import com.zy.jdbclib.core.JDBCException;
import com.zy.jdbclib.core.RowMapper;
import com.zy.jdbclib.utils.BeanPropertyRowMapper;

/**
 * @version 1.0
 * @since 1.0
 */
public class Test {

	/**
	 * @param args
	 * @throws ParseException
	 * @throws JDBCException
	 */
	public static void main(String[] args) throws JDBCException,
			ParseException {	
		
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource
				.setUrl("jdbc:mysql://127.0.0.1:3306/mydemo?characterEncoding=utf-8");
		dataSource.setUser("root");
		dataSource.setPassword("123456");

		class MainRowMapper implements RowMapper<Object> {

			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				int columnCount = rs.getMetaData().getColumnCount();
				for (int i = 1; i <= columnCount; i++) {
					System.out.print(rs.getString(i) + " ");
				}
				System.out.println();
				return null;
			}

		}

		JdbcTemplate template = new JdbcTemplate(dataSource);
		
		System.out.println("**************************************");
		System.out
				.println("public List query(String sql, RowMapper rowMapper):");
		template.query("select * from admin", new MainRowMapper());
		System.out.println("****************************************\n");
		
		

		System.out.println("**************************************");
		System.out.println("public List queryForList(String sql):");
		List<Map<String,Object>> list = template.queryForList("select * from admin");
		for (int i = 0; i < list.size(); i++) {
			System.out.println(i + "   " + list.get(i));
		}
		System.out.println("****************************************\n");
		

		System.out.println("**************************************");
		System.out.println("template.queryForInt(String sql):");
		System.out.println(template.queryForInt("select count(*) from admin"));

		System.out.println("public long queryForLong(String sql) :");
		System.out.println(template
				.queryForLong("select id from admin limit 1"));
		System.out.println("****************************************\n");
		
		

		System.out.println("**************************************");
		System.out
				.println("public Object queryForObject(String sql, RowMapper rowMapper) :");
		template.queryForObject("select * from admin limit 1",
				new MainRowMapper());
		System.out.println("****************************************\n");
		
		
		template.update("delete from admin where id = 14");
		

		System.out.println("**************************************");
		System.out
				.println("public List query(String sql, Object[] args, RowMapper  rowMapper):");
		template
				.query("select * from admin where id=?", new MainRowMapper(), 2);
		System.out.println("****************************************\n");
		
		

		System.out.println("**************************************");
		System.out
				.println("public List query(String sql, RowMapper rowMapper, Object... args) :");
		template.query("select * from admin where username=? and lastTime=?",
				new MainRowMapper(), "123", new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss").parse("2009-04-24 00:00:00"));
		System.out.println("****************************************\n");

		
		System.out.println("**************************************");
		System.out
				.println("public Object queryForObject(String sql, RowMapper rowMapper) :");
		Admin admin = (Admin) template.queryForObject(
				"select * from admin limit 1",
				new BeanPropertyRowMapper<Admin>(Admin.class));
		System.out.println(admin);
		System.out.println("****************************************\n");

		
		System.out.println("**************************************");
		List<Admin> adminList = (List<Admin>) template.query(
				"select * from admin where username=?",
				new BeanPropertyRowMapper<Admin>(Admin.class), "123");
		for (Admin admin2 : adminList) {
			System.out.println(admin2);
		}
		System.out.println("****************************************\n");

	
		System.out.println("**************************************");
		Map<String,Object> map = template.queryForMap("select * from admin limit 1");
		System.out.println(map);
		System.out.println("****************************************\n");

		
		System.out.println(template.queryForObject("select count(*) from admin", Integer.class));

	}
}
