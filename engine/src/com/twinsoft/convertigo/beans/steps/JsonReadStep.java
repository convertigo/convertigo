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

import java.io.IOException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.json.JsonData;
import com.twinsoft.convertigo.engine.json.JsonPath;

/**
 * Reads a value at a JSON path in a JSON text (the raw answer of a transaction...) or in JSON data, in one execution.
 * A text is read in stream: only the value at the path is built, as JSON.parse would build it, and the rest of the
 * text is skipped (a path with a wildcard reads the whole text). A text that is not JSON gives the default value and
 * a warning, as a JSON.parse in a try.
 */
public class JsonReadStep extends JsonTransformStep {
	private static final long serialVersionUID = 1L;

	private String json = "text";
	private String path = "$";
	private String defaultValue = "";

	transient private String studioInput;

	public JsonReadStep() {
		super();
		setVariableName("value");
	}

	@Override
	public String toString() {
		return getName() + " (" + json + " " + path + " → " + getVariableName() + ")";
	}

	@Override
	protected Object transform(Context javascriptContext, Scriptable scope) throws EngineException {
		Object source = eval(javascriptContext, scope, json, "json");
		JsonPath jsonPath;
		try {
			jsonPath = JsonPath.parse(path == null || path.trim().isEmpty() ? "$" : path);
		} catch (IllegalArgumentException e) {
			throw new EngineException("Invalid JSON path of the step '" + getName() + "'", e);
		}
		Object value = JsonPath.MISSING;
		studioInput = null;
		if (source instanceof CharSequence) {
			if (Engine.isStudioMode()) {
				studioInput = source.toString();
			}
			try {
				// in one pass: the value is built from the stream of the text, without a copy of its JSON
				value = jsonPath.selectValue(source.toString());
			} catch (IOException e) {
				// as a JSON.parse in a try: the default value, and a warning
				Engine.logBeans.warn("(JsonReadStep) '" + getName() + "' did not read JSON: " + e.getMessage());
			}
		} else if (!JsonData.isNullish(source)) {
			value = jsonPath.select(source);
		}
		if (value == JsonPath.MISSING) {
			value = eval(javascriptContext, scope, defaultValue, "defaultValue");
		}
		return value;
	}

	@Override
	protected String getStudioInput() {
		return studioInput;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
