
package com.zy.jdbclib.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

import com.zy.jdbclib.core.JDBCException;
import com.zy.jdbclib.core.PreparedStatementSetter;

/**
 * 
 * @version 1.0
 * @since 1.0
 */
public class ArgTypePreparedStatementSetter implements PreparedStatementSetter {

	private final Object[] args;

	private final int[] argTypes;

	/**
	 * Create a new ArgTypePreparedStatementSetter for the given arguments.
	 * 
	 * @param args
	 *            the arguments to set
	 * @param argTypes
	 *            the corresponding SQL types of the arguments
	 */
	public ArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
		if ((args != null && argTypes == null)
				|| (args == null && argTypes != null)
				|| (args != null && args.length != argTypes.length)) {
			throw new JDBCException(
					"args and argTypes parameters must match");
		}
		this.args = args;
		this.argTypes = argTypes;
	}

	@SuppressWarnings("unchecked")
	public void setValues(PreparedStatement ps) throws SQLException {
		int argIndx = 1;
		if (this.args != null) {
			for (int i = 0; i < this.args.length; i++) {
				Object arg = this.args[i];
				if (arg instanceof Collection
						&& this.argTypes[i] != Types.ARRAY) {
					Collection entries = (Collection) arg;
					for (Iterator it = entries.iterator(); it.hasNext();) {
						Object entry = it.next();
						if (entry instanceof Object[]) {
							Object[] valueArray = ((Object[]) entry);
							for (int k = 0; k < valueArray.length; k++) {
								Object argValue = valueArray[k];
								StatementCreatorUtils.setParameterValue(ps,
										argIndx++, this.argTypes[i], argValue);
							}
						} else {
							StatementCreatorUtils.setParameterValue(ps,
									argIndx++, this.argTypes[i], entry);
						}
					}
				} else {
					StatementCreatorUtils.setParameterValue(ps, argIndx++,
							this.argTypes[i], arg);
				}
			}
		}
	}

}
