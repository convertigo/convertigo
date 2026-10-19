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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.json.JsonData;

/**
 * Keeps the first row of each distinct combination of the values of some columns. Without columns, the rows are
 * kept as they are.
 */
public class JsonDistinctStep extends JsonTransformStep {
	private static final long serialVersionUID = 1L;

	private String columns = "";
	private String valueKey = "value";

	public JsonDistinctStep() {
		super();
		setVariableName("distinctRows");
	}

	/** column=JSON of the value|...: the signature of a row, as lib_BaseRow builds it */
	static String signature(Object row, List<String> columns, String valueKey) {
		StringBuilder sb = new StringBuilder(64);
		for (int i = 0; i < columns.size(); i++) {
			String column = columns.get(i);
			Object cell = JsonData.has(row, column) ? JsonData.get(row, column) : null;
			Object value = JsonData.isNullish(cell) ? null : (valueKey.isEmpty() ? cell : JsonData.get(cell, valueKey));
			if (i > 0) {
				sb.append('|');
			}
			sb.append(column).append('=').append(JsonData.stringify(value));
		}
		return sb.toString();
	}

	@Override
	protected Object transform(Context javascriptContext, Scriptable scope) throws EngineException {
		Object source = eval(javascriptContext, scope, getRows(), "rows");
		List<String> distinctColumns = JsonData.strings(eval(javascriptContext, scope, columns, "columns"));
		List<Object> rows = JsonData.list(source);
		if (distinctColumns == null || rows == null) {
			return rows;
		}
		Set<String> seen = new HashSet<String>();
		List<Object> result = new ArrayList<Object>(rows.size());
		for (Object row : rows) {
			if (seen.add(signature(row, distinctColumns, valueKey))) {
				result.add(row);
			}
		}
		return result;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getValueKey() {
		return valueKey;
	}

	public void setValueKey(String valueKey) {
		this.valueKey = valueKey;
	}
}
