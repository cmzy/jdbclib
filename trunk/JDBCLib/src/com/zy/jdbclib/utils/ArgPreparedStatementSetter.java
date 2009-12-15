
package com.zy.jdbclib.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.zy.jdbclib.core.PreparedStatementSetter;
import com.zy.jdbclib.core.SqlParameterValue;
import com.zy.jdbclib.core.SqlTypeValue;

/**
 * @version 1.0
 * @since 1.0
 */
public class ArgPreparedStatementSetter implements PreparedStatementSetter {

    private final Object[] args;

    public ArgPreparedStatementSetter(Object[] args) {
        this.args = args;
    }

    public void setValues(PreparedStatement ps) throws SQLException {
        if (this.args != null) {
            for (int i = 0; i < this.args.length; i++) {
                Object arg = this.args[i];
                if (arg instanceof SqlParameterValue) {
                    SqlParameterValue paramValue = (SqlParameterValue)arg;
                    StatementCreatorUtils.setParameterValue(ps, i + 1, paramValue, paramValue
                            .getValue());
                } else {
                    StatementCreatorUtils.setParameterValue(ps, i + 1, SqlTypeValue.TYPE_UNKNOWN,
                            arg);
                }
            }
        }
    }

}
