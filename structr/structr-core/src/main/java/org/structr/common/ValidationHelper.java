/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.common;

import java.util.Arrays;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.*;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.entity.AbstractNode.Key;

/**
 *
 * @author Christian Morgner
 */
public class ValidationHelper {

	// ----- public static methods -----
	public static boolean checkStringMinLength(GraphObject node, PropertyKey key, int minLength, ErrorBuffer errorBuffer) {

		String value = node.getStringProperty(key);
		String type  = node.getType();

		if (StringUtils.isNotBlank(value)) {

			if (value.length() >= minLength) {

				return false;

			}

			errorBuffer.add(type, new TooShortToken(key, minLength));

			return true;

		}

		errorBuffer.add(type, new EmptyPropertyToken(key));

		return true;
	}

	public static boolean checkStringNotBlank(GraphObject node, PropertyKey key, ErrorBuffer errorBuffer) {

		String type  = node.getType();

		if (StringUtils.isNotBlank((String)node.getProperty(key))) {

			return false;

		}

		errorBuffer.add(type, new EmptyPropertyToken(key));

		return true;
	}

	public static boolean checkPropertyNotNull(GraphObject node, PropertyKey key, ErrorBuffer errorBuffer) {
		
		String type  = node.getType();

		if (key == null) {
			errorBuffer.add(type, new EmptyPropertyToken(Key.unknown_type));
			return true;
		}

		Object value = node.getProperty(key);

		if (value != null) {

			if (value instanceof IterableAdapter) {

				if (((IterableAdapter) value).iterator().hasNext()) {

					return false;

				}

			} else {

				return false;

			}

		}

		errorBuffer.add(type, new EmptyPropertyToken(key));

		return true;
	}

	public static boolean checkDate(GraphObject node, PropertyKey key, ErrorBuffer errorBuffer) {

		Date date     = node.getDateProperty(key);
		String type   = node.getType();
		boolean error = false;

		if ((date == null) || ((date != null) && (date.getTime() == 0))) {

			errorBuffer.add(type, new EmptyPropertyToken(key));
			error = true;

		}

		return error;
	}

	public static boolean checkDatesChronological(GraphObject node, PropertyKey key1, PropertyKey key2, ErrorBuffer errorBuffer) {

		Date date1    = node.getDateProperty(key1);
		Date date2    = node.getDateProperty(key2);
		String type   = node.getType();
		boolean error = false;

		error |= checkDate(node, key1, errorBuffer);
		error |= checkDate(node, key2, errorBuffer);

		if ((date1 != null) && (date2 != null) &&!date1.before(date2)) {

			errorBuffer.add(type, new ChronologicalOrderToken(key1, key2));

			error = true;

		}

		return error;
	}

	public static boolean checkStringInArray(GraphObject node, PropertyKey key, String[] values, ErrorBuffer errorBuffer) {

		String type  = node.getType();

		if (StringUtils.isNotBlank(node.getStringProperty(key))) {

			if (Arrays.asList(values).contains(node.getStringProperty(key))) {

				return false;

			}

		}

		errorBuffer.add(type, new ValueToken(key, values));

		return true;
	}

	public static boolean checkStringInEnum(GraphObject node, PropertyKey key, Class<? extends Enum> enumType, ErrorBuffer errorBuffer) {
		
		return checkStringInEnum(node.getType(), node, key, enumType, errorBuffer);
	}
	
	public static boolean checkStringInEnum(String typeString, GraphObject node, PropertyKey key, Class<? extends Enum> enumType, ErrorBuffer errorBuffer) {

		String value = node.getStringProperty(key);
		Object[] values = enumType.getEnumConstants();

		if (StringUtils.isNotBlank(value)) {

			for (Object v : values) {
				
				String enumValue = v.toString();
				
				if (enumValue.equals(value)) {
					return false;
				}
				
			}
		}

		errorBuffer.add(typeString, new ValueToken(key, values));

		return true;
	}

	public static boolean checkNullOrStringInArray(GraphObject node, PropertyKey key, String[] values, ErrorBuffer errorBuffer) {

		String value = node.getStringProperty(key);
		String type  = node.getType();

		if(value == null) {
			return false;
		}

		if (StringUtils.isNotBlank(node.getStringProperty(key))) {

			if (Arrays.asList(values).contains(node.getStringProperty(key))) {

				return false;

			}

		}

		errorBuffer.add(type, new ValueToken(key, values));

		return true;
	}
}
