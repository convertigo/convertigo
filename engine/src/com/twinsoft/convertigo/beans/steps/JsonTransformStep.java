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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.json.JsonData;
import com.twinsoft.convertigo.engine.util.JsonNativeOutput;
import com.twinsoft.convertigo.engine.util.RhinoUtils;

/**
 * A native JSON step: it runs once on a whole array of the sequence (the rows of a table) and puts the array it
 * builds in a variable of the sequence. Its inputs are JavaScript expressions evaluated in the scope of the sequence;
 * the array it builds holds Java maps and lists, which the next native steps and the native JSON output of the
 * sequence read without converting them. No XML is built for them, unless a step of the sequence reads one by an
 * XPath source: that step only gets an XML view of the array, built on demand like a JSON to XML step would build it.
 */
public abstract class JsonTransformStep extends Step {
	private static final long serialVersionUID = 1L;

	private String rows = "rows";
	private String variableName = "rows";

	transient private Object result;
	transient private Node xmlView;

	public JsonTransformStep() {
		super();
		setOutput(false);
		this.xml = false;
	}

	public String getRows() {
		return rows;
	}

	public void setRows(String rows) {
		this.rows = rows;
	}

	public String getVariableName() {
		return variableName;
	}

	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	@Override
	public String toJsString() {
		return "";
	}

	@Override
	public String toString() {
		return getName() + " (" + rows + " → " + variableName + ")";
	}

	/** the value of a JavaScript expression of the step, null for an empty expression */
	protected Object eval(Context javascriptContext, Scriptable scope, String expression, String property) throws EngineException {
		if (expression == null || expression.trim().isEmpty()) {
			return null;
		}
		evaluate(javascriptContext, scope, expression, property, true);
		return JsonData.unwrap(evaluated);
	}

	/** the XML view of the array for an XPath source, built on the first read only */
	@Override
	public Node getContextNode(int loop) throws EngineException {
		if (xmlView == null && result != null) {
			try {
				xmlView = new JsonNativeOutput(variableName, RhinoUtils.jsonStringifyData(result), getStepNodeName(), "item")
						.toElement(getOutputDocument());
				Engine.logBeans.debug("(JsonTransformStep) XML view of '" + getName() + "' built for an XPath source");
			} catch (Exception e) {
				throw new EngineException("Unable to build the XML view of the step '" + getName() + "'", e);
			}
		}
		return xmlView;
	}

	@Override
	public String getStepNodeName() {
		return getName();
	}

	protected abstract Object transform(Context javascriptContext, Scriptable scope) throws EngineException;

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		if (isEnabled() && super.stepExecute(javascriptContext, scope)) {
			result = transform(javascriptContext, scope);
			xmlView = null;
			scope.put(variableName, scope, result);
			return true;
		}
		return false;
	}
}
