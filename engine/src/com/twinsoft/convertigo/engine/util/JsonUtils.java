/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class JsonUtils {
	private final static Pattern pQuotedString = Pattern.compile("^\"(?:[^\"]*(?:(?<=\\\\)\")?)*\"$");

	public static Object put(Object o, String key, Object value) {
		try {
			if (o instanceof JSONObject) {
				return ((JSONObject) o).put(key, value);
			} else if (o instanceof JSONArray) {
				JSONArray array = (JSONArray) o;
				int i = Integer.parseInt(key);
				return array.put(i, value);
			}
		} catch (Exception e) {
		}
		return null;
	}

	public static Object remove(Object o, String key) {
		try {
			if (o instanceof JSONObject) {
				return ((JSONObject) o).remove(key);
			} else if (o instanceof JSONArray) {
				JSONArray array = (JSONArray) o;
				int i = Integer.parseInt(key);
				Object r = array.get(i);
				array.put(i, (Object) null);
				return r;
			}
		} catch (Exception e) {
		}
		return null;
	}

	public static boolean has(Object o, String key) {
		try {
			if (o instanceof JSONObject) {
				return ((JSONObject) o).has(key);
			} else if (o instanceof JSONArray) {
				JSONArray array = (JSONArray) o;
				int i = Integer.parseInt(key);
				return i < array.length();
			}
		} catch (Exception e) {
		}
		return false;
	}

	public static String quoteValue(String value) {
		try {
			Long.parseLong(value);
		} catch (Exception eLong) {
			try {
				Double.parseDouble(value);
			} catch (Exception eDouble) {
				try {
					new JSONObject(value);
				} catch (Exception eJSONObject) {
					try {
						new JSONArray(value);
					} catch (Exception eJSONArray) {
						if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
							if (!pQuotedString.matcher(value).matches()) {
								value = '"' + value.replace("\"", "\\\"") + '"';
							}
						}
					}
				}
			}
		}
		return value;
	}

	public static void merge(JSONObject jsonTarget, JSONObject jsonSource) {
		for (Iterator<String> i = GenericUtils.cast(jsonSource.keys()); i.hasNext(); ) {
			String key = i.next();
			
			try {
				if (jsonTarget.has(key)) {
					Object targetValue = jsonTarget.get(key);
					Object sourceValue = jsonSource.get(key);
					
					if (targetValue instanceof JSONObject && sourceValue instanceof JSONObject) {
						merge((JSONObject) targetValue, (JSONObject) sourceValue);
					} else if (targetValue instanceof JSONArray && sourceValue instanceof JSONArray) {
						merge((JSONArray) targetValue, (JSONArray) sourceValue);
					} else {
						jsonTarget.put(key, sourceValue);
					}
				} else {
					jsonTarget.put(key, jsonSource.get(key));
				}
			} catch (JSONException e) {
				//TODO: handle
			}
		}
	}

	public static void merge(JSONArray targetArray, JSONArray sourceArray) {
		int targetSize = targetArray.length();
		int sourceSize = sourceArray.length();
		
		for (int i = 0; i < sourceSize; i++) {
			try {
				Object targetValue = targetSize > i && !targetArray.isNull(i) ? targetArray.get(i) : null;
				Object sourceValue = sourceArray.isNull(i) ? null : sourceArray.get(i);
				if (i < targetSize) {
					if (targetValue instanceof JSONObject && sourceValue instanceof JSONObject) {
						merge((JSONObject) targetValue, (JSONObject) sourceValue);
					} else if (targetValue instanceof JSONArray && sourceValue instanceof JSONArray) {
						merge((JSONArray) targetValue, (JSONArray) sourceValue);
					} else {
						targetArray.put(i, sourceValue);
					}
				} else {
					targetArray.put(sourceValue);
				}
			} catch (JSONException e) {
				//TODO: handle
			}
		}
	}

	public static Object get(Object object, String key) throws NoSuchElementException {
		try {
			if (object instanceof JSONObject) {
				object = ((JSONObject) object).get(key);
			} else if (object instanceof JSONArray) {
				object = ((JSONArray) object).get(Integer.parseInt(key));
			} else {
				throw new NoSuchElementException("'" + key + "' cannot be in a non-JSON object");
			}
		} catch (NumberFormatException e) {
			throw new NoSuchElementException("'" + key + "' isn't a number");
		} catch (Exception e) {
			throw new NoSuchElementException("'" + key + "' not found");
		}
		return object;
	}

	public static Object getOrNull(Object object, String key) {
		try {
			return get(object, key);
		} catch (Exception e) {
			return null;
		}
	}

	public static Object find(Object object, List<String> path) throws NoSuchElementException {
		for (String key: path) {
			object = get(object, key);
		}
		return object;
	}

	public static Object findOrNull(Object object, List<String> path) {
		try {
			return find(object, path);
		} catch (Exception e) {
			return null;
		}
	}
}
