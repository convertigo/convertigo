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
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.ITagsProperty;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.json.BaserowCellFormat;
import com.twinsoft.convertigo.engine.json.JsonData;
import com.twinsoft.convertigo.engine.json.JsonRowStream;
import com.twinsoft.convertigo.engine.json.JsonRowStream.StreamFailure;
import com.twinsoft.convertigo.engine.json.JsonStreamOutput;

/**
 * Writes the rows of a Baserow table in stream: the steps Format cells, Distinct rows and Select columns fused in one
 * pass, each row read, formatted and written then dropped. Without stream mode it is a JSON to XML step on its JSON
 * object. In stream mode, the rows of the first page (read by the sequence) and of the next pages (read here, by the
 * "next" URL of Baserow) are written:
 * - while the servlet writes the response, without values only;
 * - into a spool now, with values only, because the options of the columns (fields variable), known after the last
 * row, are answered before the rows; an error of Baserow then goes to the error variable and nothing is output.
 */
public class JsonStreamRowsStep extends JsonToXmlStep implements ITagsProperty {
	private static final long serialVersionUID = 1L;

	private String streamMode = "false";
	private String rows = "results";
	private String firstPage = "page";
	private String nextUrl = "";
	private String baseUrl = "";
	private String authorization = "";
	private String maxRows = "1000";
	private String schema = "";
	private String columns = "";
	private String hiddenColumns = "";
	private String valuesOnly = "false";
	private String defaultTimezone = "\"UTC\"";
	private String distinctColumns = "";
	private String firstColumns = "";
	private String outputColumns = "";
	private String columnsOrder = "";
	private String failPage = "";
	private String restartPageSize = "";
	private String fieldsVariableName = "";
	private String errorVariableName = "";

	transient private JsonStreamOutput streamOutput;
	transient private boolean skipOutput;

	public JsonStreamRowsStep() {
		super();
	}

	private Object eval(Context javascriptContext, Scriptable scope, String expression, String property) throws EngineException {
		if (expression == null || expression.trim().isEmpty()) {
			return null;
		}
		evaluate(javascriptContext, scope, expression, property, true);
		return JsonData.unwrap(evaluated);
	}

	private String text(Context javascriptContext, Scriptable scope, String expression, String property) throws EngineException {
		Object value = eval(javascriptContext, scope, expression, property);
		return JsonData.isNullish(value) ? null : JsonData.toStr(value);
	}

	@Override
	protected boolean stepExecute(Context javascriptContext, Scriptable scope) throws EngineException {
		streamOutput = null;
		skipOutput = false;
		if (isEnabled() && ScriptRuntime.toBoolean(eval(javascriptContext, scope, streamMode, "streamMode"))) {
			boolean lean = ScriptRuntime.toBoolean(eval(javascriptContext, scope, valuesOnly, "valuesOnly"));
			boolean direct = !lean && isJsonNativeCandidate();
			BaserowCellFormat format = new BaserowCellFormat(JsonData.toStr(eval(javascriptContext, scope, defaultTimezone, "defaultTimezone")),
					javascriptContext, direct ? null : scope);
			JsonRowStream stream = new JsonRowStream(format,
					JsonData.list(eval(javascriptContext, scope, schema, "schema")),
					JsonData.strings(eval(javascriptContext, scope, columns, "columns")),
					eval(javascriptContext, scope, hiddenColumns, "hiddenColumns"), lean,
					JsonData.strings(eval(javascriptContext, scope, distinctColumns, "distinctColumns")),
					JsonData.strings(eval(javascriptContext, scope, firstColumns, "firstColumns")),
					JsonData.strings(eval(javascriptContext, scope, outputColumns, "outputColumns")),
					JsonData.strings(eval(javascriptContext, scope, columnsOrder, "columnsOrder")),
					(int) ScriptRuntime.toNumber(eval(javascriptContext, scope, maxRows, "maxRows")),
					JsonData.list(eval(javascriptContext, scope, rows, "rows")),
					text(javascriptContext, scope, firstPage, "firstPage"),
					text(javascriptContext, scope, nextUrl, "nextUrl"),
					text(javascriptContext, scope, baseUrl, "baseUrl"),
					text(javascriptContext, scope, authorization, "authorization"),
					text(javascriptContext, scope, failPage, "failPage"),
					text(javascriptContext, scope, restartPageSize, "restartPageSize"));
			evaluate(javascriptContext, scope, "key", getKey());
			String keyName = getKey().getSingleString(this);
			if (direct) {
				streamOutput = new JsonStreamOutput(keyName, getStepNodeName(), stream);
			} else {
				try {
					streamOutput = JsonStreamOutput.spool(keyName, getStepNodeName(), stream);
				} catch (StreamFailure e) {
					skipOutput = true;
					if (!errorVariableName.isEmpty()) {
						scope.put(errorVariableName, scope, e.getMessage());
					}
				} catch (IOException e) {
					throw new EngineException("Unable to write the rows of '" + getName() + "'", e);
				}
				if (lean && !fieldsVariableName.isEmpty()) {
					scope.put(fieldsVariableName, scope, stream.getLeanFields());
				}
			}
		}
		return super.stepExecute(javascriptContext, scope);
	}

	@Override
	protected Node createStepNode() throws EngineException {
		if (skipOutput) {
			return null;
		}
		if (streamOutput != null) {
			if (isJsonNativeCandidate() && registerJsonNativeOutput(streamOutput)) {
				return null;
			}
			// no stream in this answer: the rows are the JSON of the step
			jsonSource = streamOutput.toJSONString();
		}
		return super.createStepNode();
	}

	public String getStreamMode() { return streamMode; }
	public void setStreamMode(String streamMode) { this.streamMode = streamMode; }
	public String getRows() { return rows; }
	public void setRows(String rows) { this.rows = rows; }
	public String getFirstPage() { return firstPage; }
	public void setFirstPage(String firstPage) { this.firstPage = firstPage; }
	public String getNextUrl() { return nextUrl; }
	public void setNextUrl(String nextUrl) { this.nextUrl = nextUrl; }
	public String getBaseUrl() { return baseUrl; }
	public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
	public String getAuthorization() { return authorization; }
	public void setAuthorization(String authorization) { this.authorization = authorization; }
	public String getMaxRows() { return maxRows; }
	public void setMaxRows(String maxRows) { this.maxRows = maxRows; }
	public String getSchema() { return schema; }
	public void setSchema(String schema) { this.schema = schema; }
	public String getColumns() { return columns; }
	public void setColumns(String columns) { this.columns = columns; }
	public String getHiddenColumns() { return hiddenColumns; }
	public void setHiddenColumns(String hiddenColumns) { this.hiddenColumns = hiddenColumns; }
	public String getValuesOnly() { return valuesOnly; }
	public void setValuesOnly(String valuesOnly) { this.valuesOnly = valuesOnly; }
	public String getDefaultTimezone() { return defaultTimezone; }
	public void setDefaultTimezone(String defaultTimezone) { this.defaultTimezone = defaultTimezone; }
	public String getDistinctColumns() { return distinctColumns; }
	public void setDistinctColumns(String distinctColumns) { this.distinctColumns = distinctColumns; }
	public String getFirstColumns() { return firstColumns; }
	public void setFirstColumns(String firstColumns) { this.firstColumns = firstColumns; }
	public String getOutputColumns() { return outputColumns; }
	public void setOutputColumns(String outputColumns) { this.outputColumns = outputColumns; }
	public String getColumnsOrder() { return columnsOrder; }
	public void setColumnsOrder(String columnsOrder) { this.columnsOrder = columnsOrder; }
	public String getFailPage() { return failPage; }
	public void setFailPage(String failPage) { this.failPage = failPage; }
	public String getRestartPageSize() { return restartPageSize; }
	public void setRestartPageSize(String restartPageSize) { this.restartPageSize = restartPageSize; }
	public String getFieldsVariableName() { return fieldsVariableName; }
	public void setFieldsVariableName(String fieldsVariableName) { this.fieldsVariableName = fieldsVariableName; }
	public String getErrorVariableName() { return errorVariableName; }
	public void setErrorVariableName(String errorVariableName) { this.errorVariableName = errorVariableName; }

	@Override
	public String[] getTagsForProperty(String propertyName) {
		return JsonStepTags.get(propertyName);
	}

	/** for the Studio, which colors the JavaScript expressions */
	public boolean isScriptableProperty(String propertyName) {
		return JsonStepTags.isScriptable(propertyName);
	}
}
