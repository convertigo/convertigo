/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import java.beans.Introspector;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;

/**
 * The table properties of the objects, the XMLVector of rows the table editors of the Eclipse Studio edit,
 * for the web Studio: rows of text, number or boolean cells. A list of cells is a step source, the source
 * picker edits it.
 */
class PropertyTables {

	private PropertyTables() {
	}

	/**
	 * @return true for the type of a table property, an XMLVector of rows
	 */
	static boolean isTable(Type type) {
		if (type instanceof ParameterizedType vector && vector.getRawType() == XMLVector.class) {
			var row = vector.getActualTypeArguments()[0];
			return row == XMLVector.class || row instanceof ParameterizedType cells && cells.getRawType() == XMLVector.class;
		}
		return false;
	}

	/**
	 * @return the rows of a table property of an object, or null for another property or a table of other
	 *         cells
	 */
	static JSONArray rows(DatabaseObject dbo, String name) {
		try {
			for (var pd : Introspector.getBeanInfo(dbo.getClass()).getPropertyDescriptors()) {
				var getter = pd.getReadMethod();
				if (pd.getName().equals(name) && getter != null) {
					return isTable(getter.getGenericReturnType()) ? toJson(getter.invoke(dbo)) : null;
				}
			}
		} catch (Exception e) {
			// not a table
		}
		return null;
	}

	private static JSONArray toJson(Object value) {
		if (!(value instanceof XMLVector<?> vector)) {
			return null;
		}
		var rows = new JSONArray();
		for (var item : vector) {
			if (!(item instanceof XMLVector<?> cells)) {
				return null;
			}
			var row = new JSONArray();
			for (var cell : cells) {
				if (!isScalar(cell)) {
					return null;
				}
				row.put(cell == null ? "" : cell);
			}
			rows.put(row);
		}
		return rows;
	}

	/**
	 * @return the XMLVector of rows edited by the Studio, each cell taking the type of the cells of its column
	 *         in the original value
	 */
	static XMLVector<Object> fromJson(JSONArray rows, Object original) throws Exception {
		List<Class<?>> types = new ArrayList<>();
		if (original instanceof XMLVector<?> vector) {
			for (var item : vector) {
				if (item instanceof XMLVector<?> cells) {
					for (int i = 0; i < cells.size(); i++) {
						if (types.size() <= i) {
							types.add(null);
						}
						if (types.get(i) == null && cells.get(i) != null) {
							types.set(i, cells.get(i).getClass());
						}
					}
				}
			}
		}
		var result = new XMLVector<Object>();
		for (int r = 0; r < rows.length(); r++) {
			var row = rows.getJSONArray(r);
			var cells = new XMLVector<Object>();
			for (int c = 0; c < row.length(); c++) {
				cells.add(convert(row.opt(c), c < types.size() ? types.get(c) : null));
			}
			result.add(cells);
		}
		return result;
	}
	private static boolean isScalar(Object cell) {
		return cell == null || cell instanceof String || cell instanceof Number || cell instanceof Boolean;
	}

	private static Object convert(Object cell, Class<?> type) {
		var text = cell == null || cell == JSONObject.NULL ? "" : cell.toString();
		try {
			if (type == Integer.class) {
				return Integer.valueOf(text.trim());
			}
			if (type == Long.class) {
				return Long.valueOf(text.trim());
			}
			if (type == Double.class) {
				return Double.valueOf(text.trim());
			}
			if (type == Boolean.class) {
				return Boolean.valueOf(text.trim());
			}
		} catch (NumberFormatException e) {
			// a text that is not a number stays a text
		}
		return text;
	}
}
