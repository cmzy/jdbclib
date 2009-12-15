
package com.zy.jdbclib.core;

/**
 * @version 1.0
 * @since 1.0
 */
public class SqlParameterValue extends SqlParameter {

    private final Object value;

    public SqlParameterValue(int sqlType, Object value) {
        super(sqlType);
        this.value = value;
    }

    public SqlParameterValue(int sqlType, String typeName, Object value) {
        super(sqlType, typeName);
        this.value = value;
    }

    public SqlParameterValue(int sqlType, int scale, Object value) {
        super(sqlType, scale);
        this.value = value;
    }

    public SqlParameterValue(SqlParameter declaredParam, Object value) {
        super(declaredParam);
        this.value = value;
    }

    public Object getValue() {
        return this.value;
    }

}
