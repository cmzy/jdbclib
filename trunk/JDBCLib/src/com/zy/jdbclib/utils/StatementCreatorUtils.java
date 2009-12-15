/**
 * 
 */

package com.zy.jdbclib.utils;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zy.jdbclib.core.SqlParameter;
import com.zy.jdbclib.core.SqlParameterValue;
import com.zy.jdbclib.core.SqlTypeValue;

/**
 * @version 1.0
 * @since 1.0
 */
public class StatementCreatorUtils {

    private static final Log log = LogFactory.getLog(StatementCreatorUtils.class);

    public static void setParameterValue(PreparedStatement ps, int paramIndex, SqlParameter param,
            Object inValue) throws SQLException {
        setParameterValueInternal(ps, paramIndex, param.getSqlType(), param.getTypeName(), param
                .getScale(), inValue);
    }

    public static void setParameterValue(PreparedStatement ps, int paramIndex, int sqlType,
            Object inValue) throws SQLException {
        setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
    }

    private static void setParameterValueInternal(PreparedStatement ps, int paramIndex,
            int sqlType, String typeName, Integer scale, Object inValue) throws SQLException {

        String typeNameToUse = typeName;
        int sqlTypeToUse = sqlType;
        Object inValueToUse = inValue;

        // override type info?
        if (inValue instanceof SqlParameterValue) {
            SqlParameterValue parameterValue = (SqlParameterValue)inValue;
            log.debug("Overriding typeinfo with runtime info from SqlParameterValue: column index "
                    + paramIndex + ", SQL type " + parameterValue.getSqlType() + ", Type name "
                    + parameterValue.getTypeName());
            if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
                sqlTypeToUse = parameterValue.getSqlType();
            }
            if (parameterValue.getTypeName() != null) {
                typeNameToUse = parameterValue.getTypeName();
            }
            inValueToUse = parameterValue.getValue();
        }

        log.debug("Setting SQL statement parameter value: column index "
                + paramIndex
                + ", parameter value ["
                + inValueToUse
                + "], value class ["
                + (inValueToUse != null ? inValueToUse.getClass().getName() : "null")
                + "], SQL type "
                + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer
                        .toString(sqlTypeToUse)));

        if (inValueToUse == null) {
            if (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN) {
                boolean useSetObject = false;
                sqlTypeToUse = Types.NULL;
                try {
                    DatabaseMetaData dbmd = ps.getConnection().getMetaData();
                    String databaseProductName = dbmd.getDatabaseProductName();
                    String jdbcDriverName = dbmd.getDriverName();
                    if (databaseProductName.startsWith("Informix")
                            || jdbcDriverName.startsWith("Microsoft SQL Server")) {
                        useSetObject = true;
                    } else if (databaseProductName.startsWith("DB2")
                            || jdbcDriverName.startsWith("jConnect")
                            || jdbcDriverName.startsWith("SQLServer")
                            || jdbcDriverName.startsWith("Apache Derby Embedded")) {
                        sqlTypeToUse = Types.VARCHAR;
                    }
                } catch (Throwable ex) {
                    log.debug("Could not check database or driver name", ex);
                }
                if (useSetObject) {
                    ps.setObject(paramIndex, null);
                } else {
                    ps.setNull(paramIndex, sqlTypeToUse);
                }
            } else if (typeNameToUse != null) {
                ps.setNull(paramIndex, sqlTypeToUse, typeNameToUse);
            } else {
                ps.setNull(paramIndex, sqlTypeToUse);
            }
        }

        else { // inValue != null
            if (inValueToUse instanceof SqlTypeValue) {
                ((SqlTypeValue)inValueToUse).setTypeValue(ps, paramIndex, sqlTypeToUse,
                        typeNameToUse);
            } else if (sqlTypeToUse == Types.VARCHAR || sqlTypeToUse == Types.LONGVARCHAR
                    || (sqlTypeToUse == Types.CLOB && isStringValue(inValueToUse.getClass()))) {
                ps.setString(paramIndex, inValueToUse.toString());
            } else if (sqlTypeToUse == Types.DECIMAL || sqlTypeToUse == Types.NUMERIC) {
                if (inValueToUse instanceof BigDecimal) {
                    ps.setBigDecimal(paramIndex, (BigDecimal)inValueToUse);
                } else if (scale != null) {
                    ps.setObject(paramIndex, inValueToUse, sqlTypeToUse, scale.intValue());
                } else {
                    ps.setObject(paramIndex, inValueToUse, sqlTypeToUse);
                }
            } else if (sqlTypeToUse == Types.DATE) {
                if (inValueToUse instanceof java.util.Date) {
                    if (inValueToUse instanceof java.sql.Date) {
                        ps.setDate(paramIndex, (java.sql.Date)inValueToUse);
                    } else {
                        ps.setDate(paramIndex, new java.sql.Date(((java.util.Date)inValueToUse)
                                .getTime()));
                    }
                } else if (inValueToUse instanceof Calendar) {
                    Calendar cal = (Calendar)inValueToUse;
                    ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
                } else {
                    ps.setObject(paramIndex, inValueToUse, Types.DATE);
                }
            } else if (sqlTypeToUse == Types.TIME) {
                if (inValueToUse instanceof java.util.Date) {
                    if (inValueToUse instanceof java.sql.Time) {
                        ps.setTime(paramIndex, (java.sql.Time)inValueToUse);
                    } else {
                        ps.setTime(paramIndex, new java.sql.Time(((java.util.Date)inValueToUse)
                                .getTime()));
                    }
                } else if (inValueToUse instanceof Calendar) {
                    Calendar cal = (Calendar)inValueToUse;
                    ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
                } else {
                    ps.setObject(paramIndex, inValueToUse, Types.TIME);
                }
            } else if (sqlTypeToUse == Types.TIMESTAMP) {
                if (inValueToUse instanceof java.util.Date) {
                    if (inValueToUse instanceof java.sql.Timestamp) {
                        ps.setTimestamp(paramIndex, (java.sql.Timestamp)inValueToUse);
                    } else {
                        ps.setTimestamp(paramIndex, new java.sql.Timestamp(
                                ((java.util.Date)inValueToUse).getTime()));
                    }
                } else if (inValueToUse instanceof Calendar) {
                    Calendar cal = (Calendar)inValueToUse;
                    ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()),
                            cal);
                } else {
                    ps.setObject(paramIndex, inValueToUse, Types.TIMESTAMP);
                }
            } else if (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN) {
                if (isStringValue(inValueToUse.getClass())) {
                    ps.setString(paramIndex, inValueToUse.toString());
                } else if (isDateValue(inValueToUse.getClass())) {
                    ps.setTimestamp(paramIndex, new java.sql.Timestamp(
                            ((java.util.Date)inValueToUse).getTime()));
                } else if (inValueToUse instanceof Calendar) {
                    Calendar cal = (Calendar)inValueToUse;
                    ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()));
                } else {
                    // Fall back to generic setObject call without SQL type
                    // specified.
                    ps.setObject(paramIndex, inValueToUse);
                }
            } else {
                // Fall back to generic setObject call with SQL type specified.
                ps.setObject(paramIndex, inValueToUse, sqlTypeToUse);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean isStringValue(Class inValueType) {
        return (CharSequence.class.isAssignableFrom(inValueType) || StringWriter.class
                .isAssignableFrom(inValueType));
    }

    @SuppressWarnings("unchecked")
    private static boolean isDateValue(Class inValueType) {
        return (java.util.Date.class.isAssignableFrom(inValueType) && !(java.sql.Date.class
                .isAssignableFrom(inValueType)
                || java.sql.Time.class.isAssignableFrom(inValueType) || java.sql.Timestamp.class
                .isAssignableFrom(inValueType)));
    }
}
