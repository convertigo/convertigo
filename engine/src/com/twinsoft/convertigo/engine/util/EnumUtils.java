/*
 * Copyright (c) 2001-2025 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;

public class EnumUtils {
	static private final Map<Class<?>, String[]> strings = new HashMap<Class<?>, String[]>();
	static private final Map<Class<?>, String[]> names = new HashMap<Class<?>, String[]>();
	
	static public String[] toStrings(Class<?> clenum) {
		synchronized (strings) {
			String[] result = strings.get(clenum);

			if (result == null) {
				try {
					Enum<?>[] values = (Enum<?>[]) clenum.getMethod("values").invoke(null);
					result = new String[values.length];

					for (int i = 0; i < values.length; i++) {
						result[i] = values[i].toString();
					}
				} catch (Exception e) {
					result = new String[] { "EnumUtils.toStrings failure" };
				}
				strings.put(clenum, result);
			}

			return result;
		}
	}
	
	static public String[] toNames(Class<?> clenum) {
		synchronized (names) {
			String[] result = names.get(clenum);

			if (result == null) {
				try {
					Enum<?>[] values = (Enum<?>[]) clenum.getMethod("values").invoke(null);
					result = new String[values.length];

					for (int i = 0; i < values.length; i++) {
						result[i] = values[i].name();
					}
				} catch (Exception e) {
					result = new String[] { "EnumUtils.toNames failure" };
				}
				names.put(clenum, result);
			}

			return result;
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	static public Object valueOf(Class<?> propertyType, Object key) {
		Object value = null;
		Method method = null;
		try {
			try {
				method = propertyType.getMethod("valueOf", key.getClass());
			} catch (Exception e1) {
				Class<?> keyClass = ClassUtils.wrapperToPrimitive(key.getClass());
				try {
					method = propertyType.getMethod("valueOf", keyClass);
				} catch (Exception e2) {
					method = propertyType.getMethod("valueOf", String.class);
					key = key.toString();
				}
			}
			value = method.invoke(null, key);
		} catch (Exception e) {
			try {
				Enum[] values = (Enum[]) propertyType.getMethod("values").invoke(null);
				String[] strings = toStrings(propertyType);
				
				for (int i = 0; value == null && i < values.length; i++) {
					if (strings[i].equals(key)) {
						value = values[i];
					}
				}
				
				if (value == null) {
					value = values[0];
				}
			} catch (Exception e1) {
				value = "EnumUtils.valueOf failure";
			}
		}
		return value;
	}
}
