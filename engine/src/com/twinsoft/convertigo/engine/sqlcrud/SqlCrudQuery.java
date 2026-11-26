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

package com.twinsoft.convertigo.engine.sqlcrud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.engine.EngineException;

/** Server-owned fragments and allowed selectors exported with a generated CRUD transaction. */
public record SqlCrudQuery(SqlTableIdentity table, Map<String, String> columns, List<String> keys, String updateParameter) {

	public static final int DEFAULT_LIMIT = 100;
	public static final int MAX_LIMIT = 1000;
	public static final String SORT_KEY = "sort_key";
	public static final String SORT_DIRECTION = "sort_direction";
	public static final String LIMIT = "limit";
	public static final String OFFSET = "offset";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public String serialize() throws EngineException {
		try {
			return MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new EngineException("Cannot export the generated CRUD query configuration.", e);
		}
	}

	public static SqlCrudQuery parse(String json) throws EngineException {
		try {
			return MAPPER.readValue(json, SqlCrudQuery.class);
		} catch (JsonProcessingException e) {
			throw new EngineException("Cannot read the generated CRUD query configuration.", e);
		}
	}

	public String orderBy(String key, String direction) throws EngineException {
		var order = new ArrayList<String>();
		var sqlDirection = switch (direction) {
			case "asc" -> " ASC";
			case "desc" -> " DESC";
			default -> throw new EngineException("sort_direction must be asc or desc.");
		};
		if (!key.isEmpty()) {
			var column = columns.get(key);
			if (column == null) {
				throw new EngineException("Unknown sort_key for this table.");
			}
			order.add(column + sqlDirection);
		}
		for (var primaryKey : keys) {
			if (!primaryKey.equals(key)) {
				order.add(columns.get(primaryKey) + " ASC");
			}
		}
		return order.isEmpty() ? "" : "ORDER BY " + String.join(", ", order);
	}

	public static long paginationValue(String name, String value) throws EngineException {
		long min = LIMIT.equals(name) ? 1 : 0;
		long max = LIMIT.equals(name) ? MAX_LIMIT : Long.MAX_VALUE;
		try {
			long number = Long.parseLong(value);
			if (number >= min && number <= max) {
				return number;
			}
		} catch (NumberFormatException e) {
			// Report the allowed range below.
		}
		throw new EngineException(name + " must be an integer between " + min + " and " + max + ".");
	}

	static String updateParameter(Iterable<String> columnNames) {
		var names = new HashSet<String>();
		columnNames.forEach(column -> names.add(column.toLowerCase(Locale.ROOT)));
		var name = "crud_fields";
		while (names.contains(name)) {
			name += "_";
		}
		return name;
	}

	public List<String> updateFields(String value) throws EngineException {
		var fields = Arrays.asList(value.split(",", -1));
		if (fields.stream().anyMatch(field -> !columns.containsKey(field)) || fields.stream().distinct().count() != fields.size()) {
			throw new EngineException("Specify distinct writable columns in the CRUD update field list.");
		}
		return fields;
	}
}
