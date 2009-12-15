/**
 * 
 */

package com.zy.jdbclib.example;

import com.zy.jdbclib.JdbcDaoSupport;
import com.zy.jdbclib.utils.BeanPropertyRowMapper;

import java.util.List;

import javax.sql.DataSource;

/**
 * @version 1.0
 * @since 1.0
 */
public class AminDAO extends JdbcDaoSupport {

    /**
     * 
     */
    public AminDAO() {
    }

    /**
     * @param dataSource
     */
    public AminDAO(DataSource dataSource) {
        super(dataSource);
    }

    public List<Admin> findAll() {
        return getJdbcTemplate().query("select * from admin",
                new BeanPropertyRowMapper<Admin>(Admin.class));
    }

    public int deleteById(int id) {
        return getJdbcTemplate().update("delete from admin where id=?", id);
    }

}
