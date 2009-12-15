
package com.zy.jdbclib.utils;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zy.jdbclib.core.JDBCException;
import com.zy.jdbclib.core.RowMapper;

/**
 * @version 1.0
 * @since 1.0
 */
public class BeanPropertyRowMapper<T extends Object> implements RowMapper<T> {

    protected final Log log = LogFactory.getLog(getClass());

    protected Class<T> mappedClass;

    private Map<String, PropertyDescriptor> mappedFields;

    public BeanPropertyRowMapper(Class<T> mappedClass) {
        initialize(mappedClass);
    }

    /**
     * Initialize the mapping metadata for the given class.
     * 
     * @param mappedClass the mapped class.
     */
    protected void initialize(Class<T> mappedClass) {
        this.mappedClass = mappedClass;
        this.mappedFields = new HashMap<String, PropertyDescriptor>();
        PropertyDescriptor[] pds = ReflectionUtils.getPropertyDescriptors(mappedClass);
        for (int i = 0; i < pds.length; i++) {
            PropertyDescriptor pd = pds[i];
            if (pd.getWriteMethod() != null) {
                this.mappedFields.put(pd.getName().toLowerCase(), pd);
                String underscoredName = underscoreName(pd.getName());
                if (!pd.getName().toLowerCase().equals(underscoredName)) {
                    this.mappedFields.put(underscoredName, pd);
                }
            }
        }
    }

    /**
     * Convert a name in camelCase to an underscored name in lower case. Any
     * upper case letters are converted to lower case with a preceding
     * underscore.
     * 
     * @param name the string containing original name
     * @return the converted name
     */
    private String underscoreName(String name) {
        StringBuffer result = new StringBuffer();
        if (name != null && name.length() > 0) {
            result.append(name.substring(0, 1).toLowerCase());
            for (int i = 1; i < name.length(); i++) {
                String s = name.substring(i, i + 1);
                if (s.equals(s.toUpperCase())) {
                    result.append("_");
                    result.append(s.toLowerCase());
                } else {
                    result.append(s);
                }
            }
        }
        return result.toString();
    }

    /**
     * Get the class that we are mapping to.
     */
    public final Class<T> getMappedClass() {
        return this.mappedClass;
    }

    /**
	 * 
	 */
    public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
        Assert.notNull(mappedClass, "Mapped class was not specified");

        T mappedObject = ReflectionUtils.instantiateClass(this.mappedClass);

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        for (int index = 1; index <= columnCount; index++) {
            String column = JdbcUtils.lookupColumnName(rsmd, index).toLowerCase();
            PropertyDescriptor pd = (PropertyDescriptor)this.mappedFields.get(column);
            if (pd != null) {
                try {
                    Object value = getColumnValue(rs, index, pd);
                    if (log.isDebugEnabled() && rowNumber == 0) {
                        log.debug("Mapping column '" + column + "' to property '" + pd.getName()
                                + "' of type " + pd.getPropertyType());
                    }
                    ReflectionUtils.setFieldValue(mappedObject, pd.getName(), value);
                } catch (Exception ex) {
                    throw new JDBCException("Unable to map column " + column + " to property "
                            + pd.getName(), ex);
                }
            }
        }
        return mappedObject;
    }

    protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd)
            throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
    }

}
