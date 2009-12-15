
package com.zy.jdbclib;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;

/**
 * 这个类提供DAO支持，和Spring中的JdbcDaoSupport类似。子类继承它即可。
 * 
 * @author zhangyong
 * @version 1.0
 * @since 1.0
 */
public abstract class JdbcDaoSupport {

    private JdbcTemplate jdbcTemplate;

    protected Log log = LogFactory.getLog(getClass());

    public JdbcDaoSupport() {
    }

    public JdbcDaoSupport(DataSource dataSource) {
        this.setDataSource(dataSource);
    }

    /**
     * 设置dataSource，为DAO提供数据源
     * 
     * @param dataSource 数据源
     */
    public final void setDataSource(DataSource dataSource) {
        if (this.jdbcTemplate == null || dataSource != this.jdbcTemplate.getDataSource()) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
            initTemplateConfig();
        }
    }

    /**
     * 返回该DAO正在使用的数据源
     * 
     * @return 该DAO正在使用的数据源
     */
    public final DataSource getDataSource() {
        return (this.jdbcTemplate != null ? this.jdbcTemplate.getDataSource() : null);
    }

    /**
     * 设置该jdbcTemplate
     * 
     * @param jdbcTemplate
     */
    public final void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initTemplateConfig();
    }

    /**
     * 返回该DAO正在使用的JdbcTemplate对象
     * 
     * @return 正在使用的JdbcTemplate对象
     */
    public final JdbcTemplate getJdbcTemplate() {
        return this.jdbcTemplate;
    }

    protected void initTemplateConfig() {
    }
}
