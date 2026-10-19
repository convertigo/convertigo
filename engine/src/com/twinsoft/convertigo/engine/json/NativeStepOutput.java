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

package com.twinsoft.convertigo.engine.json;

import java.util.List;
import java.util.Map;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

/**
 * What a native JSON step produced in one execution, sent to the Studio (EngineListener.nativeStepExecuted) for its
 * output and debug views: a JSON text taken when the step ends, as the next steps may change the value later.
 */
public class NativeStepOutput {
	/** above this size the text is cut: the Studio only shows it */
	public static final int MAX_LENGTH = 4 * 1024 * 1024;

	private final Step step;
	private final String executeTimeID;
	private final String variableName;
	private final String json;
	private final boolean truncated;
	private final int count;
	private final long durationMicros;
	private final String contextID;
	private final String note;

	private NativeStepOutput(Step step, String executeTimeID, String variableName, String json, boolean truncated,
			int count, long durationMicros, String contextID, String note) {
		this.step = step;
		this.executeTimeID = executeTimeID;
		this.variableName = variableName;
		this.json = json;
		this.truncated = truncated;
		this.count = count;
		this.durationMicros = durationMicros;
		this.contextID = contextID;
		this.note = note;
	}

	/** sends what the step produced to the Studio, original being its loaded copy; nothing out of the Studio */
	public static void fire(Step step, Step original, String executeTimeID, String variableName, Object value,
			long durationMicros, String contextID, String note) {
		if (!Engine.isStudioMode()) {
			return;
		}
		String json;
		try {
			json = value == null ? (note != null ? "" : "null") : value instanceof String && note != null ? (String) value : RhinoUtils.jsonStringifyData(value);
		} catch (Exception e) {
			json = "\"" + e.getClass().getSimpleName() + ": " + e.getMessage() + "\"";
		}
		boolean truncated = json.length() > MAX_LENGTH;
		if (truncated) {
			json = json.substring(0, MAX_LENGTH);
		}
		Object v = JsonData.unwrap(value);
		int count = v instanceof List ? ((List<?>) v).size() : v instanceof Map ? ((Map<?, ?>) v).size() : -1;
		Step projectStep = (Step) (original != null ? original : step).getOriginal();
		NativeStepOutput output = new NativeStepOutput(projectStep, executeTimeID, variableName,
				json, truncated, count, durationMicros, contextID, note);
		Engine.theApp.fireNativeStepExecuted(new EngineEvent(output));
	}

	/** the step of the project (not the copy executed) */
	public Step getStep() {
		return step;
	}

	public String getExecuteTimeID() {
		return executeTimeID;
	}

	/** the sequence variable or response key that received the value */
	public String getVariableName() {
		return variableName;
	}

	public String getJson() {
		return json;
	}

	public boolean isTruncated() {
		return truncated;
	}

	/** items of an array or keys of an object, -1 for another value */
	public int getCount() {
		return count;
	}

	public long getDurationMicros() {
		return durationMicros;
	}

	public String getContextID() {
		return contextID;
	}

	/** how the value is written, for a value that is not in a variable (an output written in stream) */
	public String getNote() {
		return note;
	}
}
