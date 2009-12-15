
package com.zy.jdbclib;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zy.jdbclib.core.ColumnMapRowMapper;
import com.zy.jdbclib.core.ConnectionCallback;
import com.zy.jdbclib.core.JDBCException;
import com.zy.jdbclib.core.PreparedStatementCallback;
import com.zy.jdbclib.core.PreparedStatementCreator;
import com.zy.jdbclib.core.PreparedStatementSetter;
import com.zy.jdbclib.core.ResultSetExtractor;
import com.zy.jdbclib.core.RowCallbackHandler;
import com.zy.jdbclib.core.RowMapper;
import com.zy.jdbclib.core.RowMapperResultSetExtractor;
import com.zy.jdbclib.core.StatementCallback;
import com.zy.jdbclib.utils.ArgPreparedStatementSetter;
import com.zy.jdbclib.utils.ArgTypePreparedStatementSetter;
import com.zy.jdbclib.utils.Assert;
import com.zy.jdbclib.utils.JdbcUtils;
import com.zy.jdbclib.utils.SingleColumnRowMapper;

/**
 * 一个模板类，提供了JDBC的一些基本操作模板。
 * 
 * @author zhangyong
 * @version 1.0
 * @since 1.0
 */
public class JdbcTemplate {

    /**
     * 日志记录
     */
    private Log log = LogFactory.getLog(JdbcTemplate.class);

    private int fetchSize = 0;

    private int maxRows = 0;

    private int queryTimeout = 0;

    private DataSource dataSource;

    /**
     * 返回该模板使用的数据源
     * 
     * @return the dataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * 设置数据源
     * 
     * @param dataSource the dataSource to set
     */
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public JdbcTemplate() {
    }

    public JdbcTemplate(DataSource dataSource) {
        setDataSource(dataSource);
    }

    // -------------------------------------------------------------------------
    // 使用ConnectionCallback回调接口获取java.sql.Connection的方法
    // -------------------------------------------------------------------------
    /**
     * SQL连接回调，利用ConnectionCallback接口。该方法的一个使用例子如下：
     * 
     * <pre>
     * ResultType result;
     * result = execute(new ConnectionCallback&lt;ResultType&gt;() {
     *     public ResultType doInConnection(Connection conn) throws SQLException {
     *         //do something
     *     }
     * });
     * </pre>
     * 
     * @param action ConnectionCallback回调接口
     * @return ConnectionCallback回调接口中 doInConnection(Connection conn)方法返回的对象
     * @throws JDBCException
     */
    public <T extends Object> T execute(final ConnectionCallback<T> action) throws JDBCException {
        // 检查参数
        Assert.notNull(action, "Callback object must not be null!");

        Connection con = null;
        try {
            con = getDataSource().getConnection();// 获取数据库连接
            return action.doInConnection(con);
        } catch (SQLException ex) {
            try {// 关闭数据库连接
                if (!con.getAutoCommit()) {
                    con.commit();
                }
                con.close();
            } catch (SQLException e) {
                log.debug("Could not close JDBC Connection!", e);
                // throw new DataAccessException(e); 在关闭连接时的异常不需要抛出
            }
            con = null;
            throw new JDBCException(ex);
        } finally {
            try {// 关闭数据库连接
                if (!con.getAutoCommit()) {
                    con.commit();
                }
                con.close();
            } catch (SQLException e) {
                log.debug("Could not close JDBC Connection!", e);
                // throw new DataAccessException(e); 在关闭连接时的异常不需要抛出
            }
        }

    }

    // -------------------------------------------------------------------------
    // 执行静态SQL的方法 (java.sql.Statement)
    // -------------------------------------------------------------------------
    /**
     * 该方法用于执行静态的sql语句。<br/>
     * 使用方式和
     * <code>public <T extends Object> T execute(final ConnectionCallback<T> action)<code>类似。
     */
    public <T extends Object> T execute(final StatementCallback<T> action) throws JDBCException {
        // 检查参数
        Assert.notNull(action, "Callback object must not be null!");

        return execute(new ConnectionCallback<T>() {

            public T doInConnection(Connection conn) throws SQLException {
                Statement stmt = null;
                try {
                    stmt = conn.createStatement();
                    T result = action.doInStatement(stmt);
                    handleWarnings(stmt.getWarnings());
                    return result;
                } catch (SQLException ex) {
                    JdbcUtils.closeStatement(stmt);
                    stmt = null;
                    throw new JDBCException(ex);
                } finally {
                    JdbcUtils.closeStatement(stmt);
                }
            }
        });
    }

    /**
     * 用Statement执行sql语句
     * 
     * @param sql sql语句
     * @throws JDBCException
     */
    public void execute(final String sql) throws JDBCException {

        log.debug("Executing SQL statement [" + sql + "]");

        class ExecuteStatementCallback implements StatementCallback<Object> {
            public Object doInStatement(Statement stmt) throws SQLException {
                stmt.execute(sql);
                return null;
            }
        }
        execute(new ExecuteStatementCallback());
    }

    /**
     * 用Statement执行sql查询，并用给定的ResultSetExtractor回调接口处理sql查询结果ResultSet。
     * 
     * @param sql sql查询语句
     * @param rse 处理sql查询结果ResultSet的回调接口
     * @return ResultSetExtractor回调接口中
     *         <code>public T extractData(ResultSet rs)  throws SQLException </code>
     *         方法返回的处理结果。
     * @throws JDBCException
     */
    public <T extends Object> T query(final String sql, final ResultSetExtractor<T> rse)
            throws JDBCException {
        // 检查参数
        Assert.notNull(sql, "SQL must not be null");
        Assert.notNull(rse, "ResultSetExtractor must not be null");

        log.debug("Executing SQL query [" + sql + "]");

        class QueryStatementCallback implements StatementCallback<T> {
            public T doInStatement(Statement stmt) throws SQLException {
                ResultSet rs = null;
                try {
                    rs = stmt.executeQuery(sql);
                    return rse.extractData(rs);
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }
            }
        }
        return execute(new QueryStatementCallback());
    }

    /**
     * 用Statement执行sql查询，并用给定的RowCallbackHandler回调接口处理sql查询结果ResultSet中的每一行数据。
     * 
     * @param sql sql查询语句
     * @param rch 处理sql查询结果ResultSet的回调接口
     * @throws JDBCException
     */
    public void query(String sql, RowCallbackHandler rch) throws JDBCException {
        query(sql, new RowCallbackHandlerResultSetExtractor(rch));
    }

    /**
     * 用Statement执行sql查询，并用给定的RowMapper回调接口处理sql查询结果ResultSet中的每一行数据。
     * 
     * @param sql sql查询语句
     * @param rowMapper 处理sql查询结果ResultSet的回调接口
     * @return RowMapper回调接口中
     *         <code>public T mapRow(ResultSet rs, int rowNum) throws SQLException</code>
     *         方法返回的处理结果。
     * @throws JDBCException
     */
    public <T extends Object> List<T> query(String sql, RowMapper<T> rowMapper)
            throws JDBCException {
        return query(sql, new RowMapperResultSetExtractor<T>(rowMapper));
    }

    /**
     * 用Statement执行sql查询，并用给定的RowMapper回调接口处理sql查询结果ResultSet中的每一行数据。<br/>
     * 注意:sql查询返回的数据行数必须为1
     * 
     * @param sql sql查询语句
     * @param rowMapper 处理sql查询结果ResultSet的回调接口
     * @return RowMapper回调接口中
     *         <code>public T mapRow(ResultSet rs, int rowNum) throws SQLException</code>
     *         方法返回的对象。
     * @throws JDBCException
     */
    public <T extends Object> T queryForObject(String sql, RowMapper<T> rowMapper)
            throws JDBCException {
        List<T> results = query(sql, rowMapper);
        return JdbcUtils.requiredSingleResult(results);
    }

    /**
     * 用Statement执行sql查询。<br/>
     * 注意:sql查询返回的数据行数和列数都必须为1.
     * 
     * <pre>
     * 该方法一般适合聚合统计查询，例如:
     *   select count(*) from table
     *   select max(field) from table
     *   select max(field) from table
     * </pre>
     * 
     * @param sql sql查询语句
     * @param requiredType 返回值的类型
     * @return 返回的查询结果
     * @throws JDBCException
     */
    public <T extends Object> T queryForObject(String sql, Class<T> requiredType)
            throws JDBCException {
        return queryForObject(sql, new SingleColumnRowMapper<T>(requiredType));
    }

    /**
     * 用Statement执行sql查询<br/>
     * 。它将返回一个Map对象，其中Map的key为数据表的字段名，value为字段的值 注意:sql查询返回的数据行数必须为1
     * 
     * @param sql sql查询语句
     * @return 查询结果
     * @throws JDBCException
     */
    public Map<String, Object> queryForMap(String sql) throws JDBCException {
        return queryForObject(sql, new ColumnMapRowMapper());
    }

    /**
     * 用Statement执行sql查询<br/>
     * 。返回long类型的值 注意:sql查询返回的数据行数和列数都必须为1.
     * 
     * @param sql sql查询语句
     * @return 查询结果
     * @throws JDBCException
     */
    public long queryForLong(String sql) throws JDBCException {
        Number number = (Number)queryForObject(sql, Long.class);
        return (number != null ? number.longValue() : 0);
    }

    public int queryForInt(String sql) throws JDBCException {
        Number number = (Number)queryForObject(sql, Integer.class);
        return (number != null ? number.intValue() : 0);
    }

    /**
     * 用Statement执行sql查询<br/>
     * 。返回一个List，其中为我们制定的类型，例如List<Integer>等 注意:sql查询返回的数据列数都必须为1.
     * 
     * @param sql sql查询语句
     * @param elementType List中的对象类型
     * @return 查询结果
     * @throws JDBCException
     */
    public <T extends Object> List<T> queryForList(String sql, Class<T> elementType)
            throws JDBCException {
        return query(sql, new SingleColumnRowMapper<T>(elementType));
    }

    public List<Map<String, Object>> queryForList(String sql) throws JDBCException {
        return query(sql, new ColumnMapRowMapper());
    }

    public int update(final String sql) throws JDBCException {
        Assert.notNull(sql, "SQL must not be null");
        log.debug("Executing SQL update [" + sql + "]");

        class UpdateStatementCallback implements StatementCallback<Integer> {
            public Integer doInStatement(Statement stmt) throws SQLException {
                int rows = stmt.executeUpdate(sql);
                log.debug("SQL update affected " + rows + " rows");
                return new Integer(rows);
            }
        }
        return execute(new UpdateStatementCallback()).intValue();
    }

    public int[] batchUpdate(final String[] sql) throws JDBCException {
        Assert.notNull(sql, "SQL array must not be null");
        log.debug("Executing SQL batch update of " + sql.length + " statements");

        class BatchUpdateStatementCallback implements StatementCallback<int[]> {
            public int[] doInStatement(Statement stmt) throws SQLException, JDBCException {
                int[] rowsAffected = new int[sql.length];
                if (JdbcUtils.supportsBatchUpdates(stmt.getConnection())) {
                    for (int i = 0; i < sql.length; i++) {
                        stmt.addBatch(sql[i]);
                    }
                    rowsAffected = stmt.executeBatch();
                } else {
                    for (int i = 0; i < sql.length; i++) {
                        if (!stmt.execute(sql[i])) {
                            rowsAffected[i] = stmt.getUpdateCount();
                        } else {
                            throw new JDBCException("Invalid batch SQL statement: " + sql[i]);
                        }
                    }
                }
                return rowsAffected;
            }
        }
        return execute(new BatchUpdateStatementCallback());
    }

    // -------------------------------------------------------------------------
    // 使用PreparedStatement执行预编译SQL语句的方法
    // -------------------------------------------------------------------------
    /**
     * 使用PreparedStatement执行预编译SQL语句。
     * 
     * @param psc PreparedStatementCreator回调接口。你可以在其
     *            <code>PreparedStatement createPreparedStatement(Connection con)</code>
     *            方法中用Connection创建PreparedStatement对象并返回。
     * @param action PreparedStatementCallback回调接口。在其
     *            <code>doInPreparedStatement(PreparedStatement ps)</code>
     *            方法中可以获得被模板管理的PreparedStatement实例
     * @return PreparedStatementCallback回调接口中方法
     *         <code>T doInPreparedStatement(PreparedStatement ps) throws SQLException</code>
     *         返回的对象
     * @throws JDBCException
     */
    public <T extends Object> T execute(final PreparedStatementCreator psc,
            final PreparedStatementCallback<T> action) throws JDBCException {
        Assert.notNull(psc, "PreparedStatementCreator must not be null");
        Assert.notNull(action, "Callback object must not be null");

        log.debug("Executing prepared SQL statement!");

        class ExecuteConnectionCallback implements ConnectionCallback<T> {

            public T doInConnection(Connection conn) throws SQLException {
                PreparedStatement ps = null;
                try {
                    ps = psc.createPreparedStatement(conn);
                    applyStatementSettings(ps);
                    T result = action.doInPreparedStatement(ps);
                    handleWarnings(ps.getWarnings());
                    return result;
                } catch (SQLException ex) {
                    JdbcUtils.closeStatement(ps);
                    ps = null;
                    throw new JDBCException(ex);
                } finally {
                    JdbcUtils.closeStatement(ps);
                }
            }
        }
        return execute(new ExecuteConnectionCallback());
    }

    /**
     * 使用PreparedStatement执行预编译SQL语句，并用PreparedStatementCallback回调接口处理结果
     * 
     * @param sql sql语句
     * @param action PreparedStatementCallback回调实例
     * @return PreparedStatementCallback回调接口中
     *         <code>public T doInPreparedStatement(PreparedStatement ps)</code>
     *         返回的处理结果
     * @throws JDBCException
     */
    public <T extends Object> T execute(String sql, PreparedStatementCallback<T> action)
            throws JDBCException {
        return execute(new SimplePreparedStatementCreator(sql), action);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句
     * 
     * @param psc 创建PreparedStatement回调
     * @param pss sql参数设置回调
     * @param rse 查询结果处理回调
     * @return 查询结果
     * @throws JDBCException
     */
    public <T extends Object> T query(PreparedStatementCreator psc,
            final PreparedStatementSetter pss, final ResultSetExtractor<T> rse)
            throws JDBCException {

        Assert.notNull(rse, "ResultSetExtractor must not be null");

        log.debug("Executing prepared SQL query");

        return execute(psc, new PreparedStatementCallback<T>() {
            public T doInPreparedStatement(PreparedStatement ps) throws SQLException {
                ResultSet rs = null;
                try {
                    if (pss != null) {
                        pss.setValues(ps);
                    }
                    rs = ps.executeQuery();
                    ResultSet rsToUse = rs;
                    return rse.extractData(rsToUse);
                } finally {
                    JdbcUtils.closeResultSet(rs);
                }
            }
        });
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句
     * 
     * @param psc 创建PreparedStatement回调
     * @param rse 查询结果处理回调
     * @return 查询结果
     * @throws JDBCException
     */
    public <T extends Object> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse)
            throws JDBCException {
        return query(psc, null, rse);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句
     * 
     * @param psc 创建PreparedStatement回调
     * @param rch 查询结果处理回调
     * @return 查询结果
     * @throws JDBCException
     */
    public void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws JDBCException {
        query(psc, new RowCallbackHandlerResultSetExtractor(rch));
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句
     * 
     * @param sql 预编译sql语句
     * @param pss sql参数设置器回调
     * @param rse 结果处理回调
     * @return 查询结果
     * @throws JDBCException
     */
    public <T extends Object> T query(String sql, PreparedStatementSetter pss,
            ResultSetExtractor<T> rse) throws JDBCException {
        return query(new SimplePreparedStatementCreator(sql), pss, rse);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param rse 结果处理回调
     * @param args sql参数数组
     * @param argTypes sql参数类型
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> T query(String sql, ResultSetExtractor<T> rse, Object[] args,
            int[] argTypes) throws JDBCException {
        return query(sql, new ArgTypePreparedStatementSetter(args, argTypes), rse);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *  args中的sql参数顺序必须与sql语句中的保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param rse 结果处理器回调
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> T query(String sql, ResultSetExtractor<T> rse, Object... args)
            throws JDBCException {
        return query(sql, new ArgPreparedStatementSetter(args), rse);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * @param sql 预编译sql语句
     * @param pss sql参数设置器回调
     * @param rch 结果处理器回调
     * @return sql查询结果
     * @throws JDBCException
     */
    public void query(String sql, PreparedStatementSetter pss, RowCallbackHandler rch)
            throws JDBCException {
        query(sql, pss, new RowCallbackHandlerResultSetExtractor(rch));
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param rch 结果处理器回调
     * @param args sql参数数组
     * @param argTypes sql参数类型数组
     * @return sql查询结果
     * @throws JDBCException
     */
    public void query(String sql, RowCallbackHandler rch, Object[] args, int[] argTypes)
            throws JDBCException {
        query(sql, new ArgTypePreparedStatementSetter(args, argTypes), rch);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   args中的sql参数顺序必须与sql语句中的保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param rch 结果处理器互动
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public void query(String sql, RowCallbackHandler rch, Object... args) throws JDBCException {
        query(sql, new ArgPreparedStatementSetter(args), rch);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * @param sql 预编译sql语句
     * @param pss 参数设置器回调
     * @param rowMapper 结果处理器回调
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> List<T> query(String sql, PreparedStatementSetter pss,
            RowMapper<T> rowMapper) throws JDBCException {
        return query(sql, pss, new RowMapperResultSetExtractor<T>(rowMapper));
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param rowMapper 结果处理回调
     * @param args sql参数数组
     * @param argTypes sql参数类型数组
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> List<T> query(String sql, RowMapper<T> rowMapper, Object[] args,
            int[] argTypes) throws JDBCException {
        return query(sql, new ArgTypePreparedStatementSetter(args, argTypes), rowMapper);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   args中的sql参数顺序必须与sql语句中的保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param rowMapper 结果处理回调
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> List<T> query(String sql, RowMapper<T> rowMapper, Object... args)
            throws JDBCException {
        return query(sql, new ArgPreparedStatementSetter(args), rowMapper);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     *   3、sql查询的返回结果有且只能有一行
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param rowMapper 结果处理回调
     * @param args sql参数数组
     * @param argTypes sql参数类型数组
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> T queryForObject(String sql, RowMapper<T> rowMapper, Object[] args,
            int[] argTypes) throws JDBCException {

        List<T> results = query(sql, new RowMapperResultSetExtractor<T>(rowMapper, 1), args,
                argTypes);
        return JdbcUtils.requiredSingleResult(results);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、sql查询的返回结果有且只能有一行
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param rowMapper 结果处理回调
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args)
            throws JDBCException {
        List<T> results = query(sql, new RowMapperResultSetExtractor<T>(rowMapper, 1), args);
        return JdbcUtils.requiredSingleResult(results);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT聚合、统计查询语句。如 select
     * count(*) from table
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     *   3、sql查询的返回结果有且只能有一行和一列
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param requiredType
     * @param args sql参数数组
     * @param argTypes sql参数类型数组
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> T queryForObject(String sql, Class<T> requiredType, Object[] args,
            int[] argTypes) throws JDBCException {
        return queryForObject(sql, new SingleColumnRowMapper<T>(requiredType), args, argTypes);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT聚合、统计查询语句。如 select
     * count(*) from table
     * 
     * <pre>
     * 注意：
     *   args中的sql参数顺序必须与sql语句中的保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param requiredType
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> T queryForObject(String sql, Class<T> requiredType, Object... args)
            throws JDBCException {
        return queryForObject(sql, new SingleColumnRowMapper<T>(requiredType), args);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     *   3、sql查询的返回参数有且只能有一列。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param elementType
     * @param args sql参数数组
     * @param argTypes sql参数类型数组
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> List<T> queryForList(String sql, Class<T> elementType, Object[] args,
            int[] argTypes) throws JDBCException {
        return query(sql, new SingleColumnRowMapper<T>(elementType), args, argTypes);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、sql查询的返回参数有且只能有一列。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param elementType
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public <T extends Object> List<T> queryForList(String sql, Class<T> elementType, Object... args)
            throws JDBCException {
        return query(sql, new SingleColumnRowMapper<T>(elementType), args);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数列表
     * @param argTypes
     * @return sql查询结果，返回的是一个item为Map的List
     * @throws JDBCException
     */
    public List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes)
            throws JDBCException {
        return query(sql, new ColumnMapRowMapper(), args, argTypes);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   args中的sql参数顺序必须与sql语句中的保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数列表
     * @return sql查询结果，返回的是一个item为Map的List
     * @throws JDBCException
     */
    public List<Map<String, Object>> queryForList(String sql, Object... args) throws JDBCException {
        return query(sql, new ColumnMapRowMapper(), args);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     *   3、sql查询的返回参数有且只能有一行。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数数组
     * @param argTypes sql参数类型数组
     * @return sql查询结果
     * @throws JDBCException
     */
    public Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes)
            throws JDBCException {
        return queryForObject(sql, new ColumnMapRowMapper(), args, argTypes);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT语句。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、sql查询的返回参数有且只能有一行。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public Map<String, Object> queryForMap(String sql, Object... args) throws JDBCException {
        return queryForObject(sql, new ColumnMapRowMapper(), args);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT 聚合，统计查询语句。例如 select
     * avg(greade) from table
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     *   3、sql查询的返回值必须为一个长整型。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数数组
     * @param argTypes sql参数类型数组
     * @return sql查询结果
     * @throws JDBCException
     */
    public long queryForLong(String sql, Object[] args, int[] argTypes) throws JDBCException {
        Number number = (Number)queryForObject(sql, Long.class, args, argTypes);
        return (number != null ? number.longValue() : 0);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT 聚合，统计查询语句。例如 select
     * avg(greade) from table
     * 
     * <pre>
     * 注意：
     *  args中的sql参数顺序必须与sql语句中的保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public long queryForLong(String sql, Object... args) throws JDBCException {
        Number number = (Number)queryForObject(sql, Long.class, args);
        return (number != null ? number.longValue() : 0);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT 聚合，统计查询语句。例如 select
     * avg(greade) from table
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     *   3、sql查询的返回值必须为一个整型。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数数组
     * @param argTypes sql参数类型数组
     * @return sql查询结果
     * @throws JDBCException
     */
    public int queryForInt(String sql, Object[] args, int[] argTypes) throws JDBCException {
        Number number = (Number)queryForObject(sql, Integer.class, args, argTypes);
        return (number != null ? number.intValue() : 0);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 查询语句，即一般只用于执行SELECT 聚合，统计查询语句。例如 select
     * avg(greade) from table
     * 
     * <pre>
     * 注意：
     *  1、args中的sql参数顺序必须与sql语句中的保持一致。
     *  2、sql查询的返回值必须为一个整型。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数列表
     * @return sql查询结果
     * @throws JDBCException
     */
    public int queryForInt(String sql, Object... args) throws JDBCException {
        Number number = (Number)queryForObject(sql, Integer.class, args);
        return (number != null ? number.intValue() : 0);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 更新语句，即一般只用于执行UPDATE,DELETE,INSERT等。
     * 
     * @param psc PreparedStatement创建器回调
     * @param pss sql参数设置回调
     * @return sql查询结果，一般为sql更新语句执行后影响的数据表中的数据行数
     * @throws JDBCException
     */
    public int update(final PreparedStatementCreator psc, final PreparedStatementSetter pss)
            throws JDBCException {
        log.debug("Executing prepared SQL update");

        Integer result = execute(psc, new PreparedStatementCallback<Integer>() {
            public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException {
                if (pss != null) {
                    pss.setValues(ps);
                }
                int rows = ps.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("SQL update affected " + rows + " rows");
                }
                return rows;

            }
        });
        return result.intValue();
    }

    /**
     * 使用PreparedStatement执行预编译SQL 更新语句，即一般只用于执行UPDATE,DELETE,INSERT等。
     * 该方法的sql语句中必须不能带参数
     * 
     * @param psc PreparedStatement创建器回调
     * @return sql查询返回的结果
     * @throws JDBCException
     */
    public int update(PreparedStatementCreator psc) throws JDBCException {
        return update(psc, null);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 更新语句，即一般只用于执行UPDATE,DELETE,INSERT等。
     * 
     * @param sql 预编译sql语句
     * @param pss sql参数设置器回调
     * @return sql查询返回的结果
     * @throws JDBCException
     */
    public int update(String sql, PreparedStatementSetter pss) throws JDBCException {
        return update(new SimplePreparedStatementCreator(sql), pss);
    }

    /**
     * 使用PreparedStatement执行预编译SQL 更新语句，即一般只用于执行UPDATE,DELETE,INSERT等。
     * 
     * <pre>
     * 注意：
     *   1、args中的sql参数顺序必须与sql语句中的保持一致。
     *   2、args和argTypes中的顺序必须保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param args sql参数数组
     * @param argTypes sql参数类型列表
     * @return sql查询返回的结果
     * @throws JDBCException
     */
    public int update(String sql, Object[] args, int[] argTypes) throws JDBCException {
        return update(sql, new ArgTypePreparedStatementSetter(args, argTypes));
    }

    /**
     * 使用PreparedStatement执行预编译SQL更新语句，即一般只用于执行UPDATE,DELETE,INSERT等。
     * 
     * <pre>
     * 注意：
     *   args中的sql参数顺序必须与sql语句中的保持一致。
     * </pre>
     * 
     * @param sql 预编译sql语句
     * @param agrs sql参数列表
     * @return sql查询返回的结果
     * @throws JDBCException
     */
    public int update(String sql, Object... agrs) throws JDBCException {
        return update(sql, new ArgPreparedStatementSetter(agrs));
    }

    // -------------------------------------------------------------------------
    // 一些工具类和工具方法
    // -------------------------------------------------------------------------
    /**
     * 设置Statement的参数
     * 
     * @param stmt 需要设置参数的Statement
     * @throws SQLException
     */
    protected void applyStatementSettings(Statement stmt) throws SQLException {
        int fetchSize = getFetchSize();
        if (fetchSize > 0) {
            stmt.setFetchSize(fetchSize);
        }
        int maxRows = getMaxRows();
        if (maxRows > 0) {
            stmt.setMaxRows(maxRows);
        }
        stmt.setQueryTimeout(getQueryTimeout());
    }

    /**
     * 处理SQL警告！
     * 
     * @param warning 需要处理的sql警告对象
     * @throws SQLException
     */
    protected void handleWarnings(SQLWarning warning) throws SQLException {
        if (warning != null) {
            SQLWarning warningToLog = warning;
            while (warningToLog != null) {
                log.debug("SQLWarning ignored: SQL state '" + warningToLog.getSQLState()
                        + "', error code '" + warningToLog.getErrorCode() + "', message ["
                        + warningToLog.getMessage() + "]");
                warningToLog = warningToLog.getNextWarning();
            }
        }
    }

    /**
     * 一个简单的PreparedStatement创建器，根据构造方法传入的sql语句创建PreparedStatement对象
     */
    private static class SimplePreparedStatementCreator implements PreparedStatementCreator {

        private final String sql;

        public SimplePreparedStatementCreator(String sql) {
            Assert.notNull(sql, "SQL must not be null!");
            this.sql = sql;
        }

        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
            return con.prepareStatement(this.sql);
        }
    }

    // 没有使用
    // private static class SimpleCallableStatementCreator implements
    // CallableStatementCreator {
    //
    // private final String callString;
    //
    // public SimpleCallableStatementCreator(String callString) {
    // if (callString == null) {
    // throw new IllegalArgumentException(
    // "Call string must not be null");
    // }
    // this.callString = callString;
    // }
    //
    // public CallableStatement createCallableStatement(Connection con)
    // throws SQLException {
    // return con.prepareCall(this.callString);
    // }
    // }

    /**
     * RowCallbackHandler和ResultSetExtractor之间的一个适配器
     */
    private static class RowCallbackHandlerResultSetExtractor implements ResultSetExtractor<Object> {

        private final RowCallbackHandler rch;

        public RowCallbackHandlerResultSetExtractor(RowCallbackHandler rch) {
            this.rch = rch;
        }

        public Object extractData(ResultSet rs) throws SQLException {
            while (rs.next()) {
                this.rch.processRow(rs);
            }
            return null;
        }
    }

    /**
     * @return the fetchSize
     */
    public int getFetchSize() {
        return fetchSize;
    }

    /**
     * @param fetchSize the fetchSize to set
     */
    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    /**
     * @return the maxRows
     */
    public int getMaxRows() {
        return maxRows;
    }

    /**
     * @param maxRows the maxRows to set
     */
    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    /**
     * @return the queryTimeout
     */
    public int getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * @param queryTimeout the queryTimeout to set
     */
    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
    }

}
