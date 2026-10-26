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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.json.JsonData;

/**
 * Keeps and orders the columns of every row: the first columns, then the listed columns, or without a list the
 * columns in the given order followed by all the others.
 */
public class JsonSelectColumnsStep extends JsonTransformStep {
	private static final long serialVersionUID = 1L;

	private String firstColumns = "";
	private String columns = "";
	private String columnsOrder = "";

	public JsonSelectColumnsStep() {
		super();
		setVariableName("selectedRows");
	}

	private static void copy(Object row, String column, Map<String, Object> target) {
		// a column already put keeps its place, as in a JavaScript object
		if (JsonData.has(row, column)) {
			target.put(column, JsonData.get(row, column));
		}
	}

	@Override
	protected Object transform(Context javascriptContext, Scriptable scope) throws EngineException {
		List<Object> rows = JsonData.list(eval(javascriptContext, scope, getRows(), "rows"));
		List<String> first = JsonData.strings(eval(javascriptContext, scope, firstColumns, "firstColumns"));
		List<String> selected = JsonData.strings(eval(javascriptContext, scope, columns, "columns"));
		List<String> order = JsonData.strings(eval(javascriptContext, scope, columnsOrder, "columnsOrder"));
		if (rows == null) {
			return null;
		}
		List<Object> result = new ArrayList<Object>(rows.size());
		for (Object row : rows) {
			Map<String, Object> target = new LinkedHashMap<String, Object>();
			if (first != null) {
				for (String column : first) {
					copy(row, column, target);
				}
			}
			if (selected != null) {
				for (String column : selected) {
					copy(row, column, target);
				}
			} else {
				if (order != null) {
					for (String column : order) {
						copy(row, column, target);
					}
				}
				for (String column : JsonData.keys(row)) {
					if (!target.containsKey(column)) {
						target.put(column, JsonData.get(row, column));
					}
				}
			}
			result.add(target);
		}
		return result;
	}

	public String getFirstColumns() {
		return firstColumns;
	}

	public void setFirstColumns(String firstColumns) {
		this.firstColumns = firstColumns;
	}

	public String getColumns() {
		return columns;
	}

	public void setColumns(String columns) {
		this.columns = columns;
	}

	public String getColumnsOrder() {
		return columnsOrder;
	}

	public void setColumnsOrder(String columnsOrder) {
		this.columnsOrder = columnsOrder;
	}
}
