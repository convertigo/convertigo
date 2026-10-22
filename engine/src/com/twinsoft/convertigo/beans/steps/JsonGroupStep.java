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


package com.twinsoft.convertigo.beans.steps;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.json.BaserowCellFormat;
import com.twinsoft.convertigo.engine.json.JsonData;

/**
 * Groups the rows by the values of some columns and aggregates each group (count, sum, avg, min, max): one row per
 * group, the first row of the group with an aggregate cell per aggregation, in the order of the first rows. The
 * aggregations ({type, column, key, label}) are checked against the schema; an invalid one gives no rows and its
 * message in the error variable. Without group columns, the rows are kept as they are.
 */
public class JsonGroupStep extends JsonTransformStep {
	private static final long serialVersionUID = 1L;

	private String format = "baserow";
	private String groupColumns = "";
	private String aggregations = "";
	private String schema = "";
	private String valueKey = "value";
	private String errorVariableName = "";

	public JsonGroupStep() {
		super();
		setVariableName("groups");
	}

	private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	private static final Pattern DATE_TIME = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})[T ](\\d{2}:\\d{2})(?::(\\d{2}))?(?:\\.(\\d{1,9}))?(Z|[+-]\\d{2}:?\\d{2})?");
	private static final DateTimeFormatter UTC_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'");

	private static final class Spec {
		String type;
		Object column, key, label, field;
		String keyText, columnText;
		/** a min or max of a date column, compared in time order */
		boolean date;
	}

	private static final class State {
		Object value;
		Double numericValue;
		String dateKey;
		double sum;
		int count;
	}

	private static final class Group {
		final Map<String, Object> row;
		final State[] states;

		Group(Map<String, Object> row, int size) {
			this.row = row;
			this.states = new State[size];
			for (int i = 0; i < size; i++) {
				states[i] = new State();
			}
		}
	}

	/** getAggregationSourceValue */
	private Object sourceValue(Object cell) {
		if (JsonData.isNullish(cell)) {
			return null;
		}
		Object value = valueKey.isEmpty() ? cell : JsonData.get(cell, valueKey);
		if (JsonData.isNullish(value) || JsonData.isArray(value)) {
			return null;
		}
		if (JsonData.isObject(value)) {
			Object inner = JsonData.get(value, "value");
			if (!JsonData.isNullish(inner) && !JsonData.isObject(inner)) {
				return inner;
			}
			Object name = JsonData.get(value, "name");
			if (!JsonData.isNullish(name) && !JsonData.isObject(name)) {
				return name;
			}
			return null;
		}
		return value;
	}

	/** getNumericValue */
	private static Double numeric(Object value) {
		if (value instanceof Number) {
			double d = ((Number) value).doubleValue();
			return Double.isFinite(d) ? d : null;
		}
		if (value instanceof CharSequence && value.toString().length() > 0) {
			String text = value.toString();
			int comma = text.indexOf(',');
			double d = JsonData.parseFloat(comma < 0 ? text : text.substring(0, comma) + "." + text.substring(comma + 1));
			return Double.isNaN(d) ? null : d;
		}
		return null;
	}

	/**
	 * getDateSortKey: a date of Baserow ("2024-03-05", "2024-03-05T08:07:00.123456Z") as a text that sorts in time
	 * order, the seconds and their fraction completed, the time in UTC; null for any other value.
	 */
	static String dateSortKey(Object value) {
		String text = JsonData.toStr(value);
		if (DATE.matcher(text).matches()) {
			return text;
		}
		Matcher m = DATE_TIME.matcher(text);
		if (!m.matches()) {
			return null;
		}
		String seconds = m.group(3) != null ? m.group(3) : "00";
		String fraction = ((m.group(4) != null ? m.group(4) : "") + "000000000").substring(0, 9);
		String zone = m.group(5);
		if (zone == null || zone.equals("Z") || zone.matches("[+-]00:?00")) {
			return m.group(1) + "T" + m.group(2) + ":" + seconds + "." + fraction + "Z";
		}
		try {
			String offset = zone.length() == 5 ? zone.substring(0, 3) + ":" + zone.substring(3) : zone;
			OffsetDateTime time = OffsetDateTime.parse(m.group(1) + "T" + m.group(2) + ":" + seconds + "." + fraction + offset);
			return time.withOffsetSameInstant(ZoneOffset.UTC).format(UTC_KEY);
		} catch (Exception e) {
			return null;
		}
	}

	/** getAggregationFieldKind == "date": a date, created on, last modified or date formula column */
	private static boolean isDateField(Object field) {
		Object type = JsonData.get(field, "type");
		String kind = JsonData.looseEq(type, "formula") || JsonData.looseEq(type, "lookup") ?
				JsonData.toStr(JsonData.get(field, "formula_type")) : JsonData.toStr(type);
		return kind.equals("date") || kind.equals("created_on") || kind.equals("last_modified");
	}

	/** updateAggregationState */
	private void update(State state, Spec spec, Object cell) {
		if (spec.type.equals("count")) {
			state.value = (state.value == null ? 0 : ((Number) state.value).doubleValue()) + 1;
			return;
		}
		Object value = sourceValue(cell);
		if (value == null) {
			return;
		}
		if (spec.type.equals("sum") || spec.type.equals("avg")) {
			Double number = numeric(value);
			if (number == null) {
				return;
			}
			state.sum += number;
			state.count++;
			state.value = spec.type.equals("sum") ? state.sum : state.sum / state.count;
			return;
		}
		// a date in time order (as a number it was its year only: parseFloat("2024-03-05") is 2024)
		String dateKey = spec.date ? dateSortKey(value) : null;
		if (dateKey != null) {
			if (state.dateKey == null || (spec.type.equals("min") ? dateKey.compareTo(state.dateKey) < 0 : dateKey.compareTo(state.dateKey) > 0)) {
				state.value = value;
				state.dateKey = dateKey;
			}
			return;
		}
		Double number = numeric(value);
		if (state.value == null) {
			state.value = value;
			state.numericValue = number;
			return;
		}
		if (state.numericValue != null && number != null) {
			if (spec.type.equals("min") ? number < state.numericValue : number > state.numericValue) {
				state.value = value;
				state.numericValue = number;
			}
			return;
		}
		int comparison = JsonData.toStr(value).compareTo(JsonData.toStr(state.value));
		if (spec.type.equals("min") ? comparison < 0 : comparison > 0) {
			state.value = value;
			state.numericValue = number;
		}
	}

	/** the message of the first aggregation its column cannot take, as lib_BaseRow words it */
	private static String check(List<Spec> specs, List<Object> fields) {
		Map<String, Object> fieldsByName = new HashMap<String, Object>();
		for (Object field : fields) {
			fieldsByName.put(JsonData.toStr(JsonData.get(field, "name")), field);
		}
		for (Spec spec : specs) {
			if (spec.type.equals("count")) {
				continue;
			}
			if (!fieldsByName.containsKey(spec.columnText)) {
				return "The aggregation column '" + spec.columnText + "' was not found in the table structure.";
			}
			Object field = fieldsByName.get(spec.columnText);
			spec.field = field;
			spec.date = (spec.type.equals("min") || spec.type.equals("max")) && isDateField(field);
			Object fieldType = JsonData.get(field, "type");
			boolean formula = false;
			for (String t : new String[] { "formula", "count", "rollup", "lookup" }) {
				formula |= fieldType instanceof CharSequence && t.equals(fieldType.toString());
			}
			boolean isArray = formula && JsonData.looseEq(JsonData.get(field, "formula_type"), "array");
			String type = JsonData.toStr(BaserowCellFormat.resolvedType(field));
			if (isArray || type.equals("file") || type.equals("link_row") || type.equals("multiple_collaborators") || type.equals("multiple_select")) {
				return "The aggregation '" + spec.type + "' is not supported on column '" + spec.columnText + "' of type '" + type + "'.";
			}
			if ((spec.type.equals("sum") || spec.type.equals("avg"))
					&& !(type.equals("autonumber") || type.equals("duration") || type.equals("number") || type.equals("rating"))) {
				return "The aggregation '" + spec.type + "' is only supported on numeric columns. Column '" + spec.columnText + "' is of type '" + type + "'.";
			}
		}
		return null;
	}

	@Override
	protected Object transform(Context javascriptContext, Scriptable scope) throws EngineException {
		List<Object> rows = JsonData.list(eval(javascriptContext, scope, getRows(), "rows"));
		List<String> columns = JsonData.strings(eval(javascriptContext, scope, groupColumns, "groupColumns"));
		List<Object> aggregationList = JsonData.list(eval(javascriptContext, scope, aggregations, "aggregations"));
		List<Object> fields = JsonData.list(eval(javascriptContext, scope, schema, "schema"));
		String[] error = new String[1];
		List<Object> result = group(rows, columns, aggregationList, fields, error);
		if (columns != null && rows != null && errorVariableName != null && !errorVariableName.isEmpty()) {
			scope.put(errorVariableName, scope, error[0]);
		}
		return result;
	}

	/** the groups of the rows, error[0] the message of an invalid aggregation (then no rows) */
	List<Object> group(List<Object> rows, List<String> columns, List<Object> aggregationList, List<Object> fields, String[] error) {
		if (columns == null || rows == null) {
			return rows;
		}
		List<Spec> specs = new ArrayList<Spec>();
		if (aggregationList != null) {
			for (Object aggregation : aggregationList) {
				Spec spec = new Spec();
				spec.type = JsonData.toStr(JsonData.get(aggregation, "type"));
				spec.column = JsonData.get(aggregation, "column");
				spec.columnText = JsonData.toStr(spec.column);
				spec.key = JsonData.get(aggregation, "key");
				spec.keyText = JsonData.toStr(spec.key);
				spec.label = JsonData.get(aggregation, "label");
				specs.add(spec);
			}
		}
		error[0] = fields != null ? check(specs, fields) : null;
		if (error[0] != null) {
			return new ArrayList<Object>(0);
		}
		Map<String, Group> groups = new LinkedHashMap<String, Group>();
		for (Object row : rows) {
			String signature = JsonDistinctStep.signature(row, columns, valueKey);
			Group group = groups.get(signature);
			if (group == null) {
				Map<String, Object> groupRow = new LinkedHashMap<String, Object>();
				for (String key : JsonData.keys(row)) {
					groupRow.put(key, JsonData.get(row, key));
				}
				for (Spec spec : specs) {
					groupRow.put(spec.keyText, null);
				}
				group = new Group(groupRow, specs.size());
				groups.put(signature, group);
			}
			for (int i = 0; i < specs.size(); i++) {
				Spec spec = specs.get(i);
				Object cell = !JsonData.isNullish(spec.column) && JsonData.has(row, spec.columnText) ? JsonData.get(row, spec.columnText) : null;
				update(group.states[i], spec, cell);
			}
		}
		List<Object> result = new ArrayList<Object>(groups.size());
		for (Group group : groups.values()) {
			for (int i = 0; i < specs.size(); i++) {
				Spec spec = specs.get(i);
				group.row.put(spec.keyText, BaserowCellFormat.aggregateCell(spec.type, spec.key, spec.column, spec.label, spec.field, group.states[i].value));
			}
			result.add(group.row);
		}
		return result;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getGroupColumns() {
		return groupColumns;
	}

	public void setGroupColumns(String groupColumns) {
		this.groupColumns = groupColumns;
	}

	public String getAggregations() {
		return aggregations;
	}

	public void setAggregations(String aggregations) {
		this.aggregations = aggregations;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getValueKey() {
		return valueKey;
	}

	public void setValueKey(String valueKey) {
		this.valueKey = valueKey;
	}

	public String getErrorVariableName() {
		return errorVariableName;
	}

	public void setErrorVariableName(String errorVariableName) {
		this.errorVariableName = errorVariableName;
	}
}
