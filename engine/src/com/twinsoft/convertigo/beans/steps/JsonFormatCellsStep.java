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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.json.BaserowCellFormat;
import com.twinsoft.convertigo.engine.json.JsonData;

/**
 * Formats every value of every row by the type of its column: each value becomes a cell (its value, its HTML), the
 * columns being described by a schema (the fields of the table). With values only, a cell keeps its value without
 * its HTML, and the options of its column are put once in the fields variable.
 */
public class JsonFormatCellsStep extends JsonTransformStep {
	private static final long serialVersionUID = 1L;

	private String format = "baserow";
	private String schema = "";
	private String columns = "";
	private String hiddenColumns = "";
	private String valuesOnly = "false";
	private String defaultTimezone = "\"UTC\"";
	private String fieldsVariableName = "";

	public JsonFormatCellsStep() {
		super();
		setVariableName("cells");
	}

	private static final class Column {
		Object field;
		boolean hidden;
		Boolean leanTyped;
	}

	@Override
	protected Object transform(Context javascriptContext, Scriptable scope) throws EngineException {
		List<Object> rows = JsonData.list(eval(javascriptContext, scope, getRows(), "rows"));
		List<Object> fields = JsonData.list(eval(javascriptContext, scope, schema, "schema"));
		List<String> exposed = JsonData.strings(eval(javascriptContext, scope, columns, "columns"));
		Object hidden = eval(javascriptContext, scope, hiddenColumns, "hiddenColumns");
		boolean lean = ScriptRuntime.toBoolean(eval(javascriptContext, scope, valuesOnly, "valuesOnly"));
		String timezone = JsonData.toStr(eval(javascriptContext, scope, defaultTimezone, "defaultTimezone"));

		Map<String, Object> fieldsByName = new HashMap<String, Object>();
		if (fields != null) {
			for (Object field : fields) {
				fieldsByName.put(JsonData.toStr(JsonData.get(field, "name")), field);
			}
		}
		Set<String> exposedSet = exposed == null ? null : new HashSet<String>(exposed);
		List<String> hiddenList = JsonData.isArray(hidden) ? JsonData.strings(hidden) : null;
		String hiddenText = hidden instanceof CharSequence ? hidden.toString() : null;
		BaserowCellFormat cellFormat = new BaserowCellFormat(timezone, javascriptContext, scope);
		Map<String, Column> columnsByKey = new HashMap<String, Column>();
		Map<String, Object> leanFields = new LinkedHashMap<String, Object>();

		List<Object> result = new ArrayList<Object>(rows == null ? 0 : rows.size());
		if (rows != null) {
			for (Object row : rows) {
				List<String> keys = JsonData.keys(row);
				Map<String, Object> cells = new LinkedHashMap<String, Object>(keys.size() * 2);
				for (String key : keys) {
					Column column = columnsByKey.get(key);
					if (column == null) {
						column = new Column();
						column.field = fields != null && (exposedSet == null || exposedSet.contains(key)) ? fieldsByName.get(key) : null;
						column.hidden = hiddenList != null ? hiddenList.contains(key) : hiddenText != null && hiddenText.contains(key);
						columnsByKey.put(key, column);
					}
					Object current = JsonData.get(row, key);
					Map<String, Object> cell = new LinkedHashMap<String, Object>(8);
					cell.put("displayName", key);
					cell.put("type", "html");
					if (JsonData.isNullish(current)) {
						cell.put("value", null);
					} else if (lean) {
						if (column.leanTyped == null) {
							column.leanTyped = BaserowCellFormat.isLeanTyped(column.field);
							if (column.field != null) {
								leanFields.put(key, cellFormat.leanFieldOptions(column.field));
							}
						}
						cell.put("value", BaserowCellFormat.leanValue(current, column.leanTyped));
					} else {
						StringBuilder html = new StringBuilder(256).append(BaserowCellFormat.BASE_HTML);
						cell.put("value", cellFormat.format(current, column.field, false, html));
						cell.put("displayValue", html.toString());
					}
					if (column.hidden) {
						cell.put("hidden", Boolean.TRUE);
					}
					cells.put(key, cell);
				}
				result.add(cells);
			}
		}
		if (fieldsVariableName != null && !fieldsVariableName.isEmpty()) {
			scope.put(fieldsVariableName, scope, leanFields);
		}
		return result;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getHiddenColumns() {
		return hiddenColumns;
	}

	public void setHiddenColumns(String hiddenColumns) {
		this.hiddenColumns = hiddenColumns;
	}

	public String getValuesOnly() {
		return valuesOnly;
	}

	public void setValuesOnly(String valuesOnly) {
		this.valuesOnly = valuesOnly;
	}

	public String getDefaultTimezone() {
		return defaultTimezone;
	}

	public void setDefaultTimezone(String defaultTimezone) {
		this.defaultTimezone = defaultTimezone;
	}

	public String getFieldsVariableName() {
		return fieldsVariableName;
	}

	public void setFieldsVariableName(String fieldsVariableName) {
		this.fieldsVariableName = fieldsVariableName;
	}
}
