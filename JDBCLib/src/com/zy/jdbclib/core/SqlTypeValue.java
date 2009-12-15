
package com.zy.jdbclib.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @version 1.0
 * @since 1.0
 */
public interface SqlTypeValue {

    int TYPE_UNKNOWN = Integer.MIN_VALUE;

    void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName)
            throws SQLException;

}
