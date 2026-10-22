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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.beans.flow;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaObjectCollection;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.constants.Constants;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ObjectLiteral;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.StringLiteral;
import org.mozilla.javascript.ast.VariableInitializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Variable;
import com.twinsoft.convertigo.beans.variables.RequestableMultiValuedVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.requesters.InternalHttpServletRequest;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.flow.FlowSourceLayout;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class Flow extends Sequence {

	private static final long serialVersionUID = 1258757999474893551L;
	private static final String DEFAULT_FLOW_SOURCE = "function Flow({ input, config, result }) {\n"
			+ "  return result\n"
			+ "}\n";

	private String flowSource = DEFAULT_FLOW_SOURCE;
	private boolean includeTrace = false;
	private transient String flowSourceDraft = null;
	private transient long flowSourceFileLastModified = -1;
	private transient String flowInputSyncSource = null;
	private transient boolean flowInputSyncing = false;
	/** the accessibility declared by _flow.accessibility, which replaces the property while the source declares it */
	private transient Accessibility declaredAccessibility = null;
	private transient String flowVirtualChildrenCacheKey = "";
	private transient List<DatabaseObject> flowVirtualChildrenCache = null;

	public Flow() {
		super();
	}

	/** Plans are prepared on first use in the runtime pool, never during project import. */
	public static void projectUnloaded(Project project) {
		if (project != null) {
			FlowEngine.projectUnloaded(project);
		}
	}

	@Override
	public Flow clone() throws CloneNotSupportedException {
		var clone = (Flow) super.clone();
		clone.clearFlowVirtualChildrenCache();
		return clone;
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		var element = super.toXml(document);
		removeSerializedProperty(element, "flowSource");
		return element;
	}

	@Override
	public void parseInputDocument(com.twinsoft.convertigo.engine.Context requestContext) throws EngineException {
		if (requestContext.nativeInput == null) { super.parseInputDocument(requestContext); return; }
		this.context = requestContext;
		variables.clear();
		variables.putAll(requestContext.nativeInput);
	}

	@Override
	protected boolean requiresJavascriptScope() { return false; }

	@Override
	public void prepareForRequestable(com.twinsoft.convertigo.engine.Context context,
			org.mozilla.javascript.Context javascriptContext, org.mozilla.javascript.Scriptable scope) {
		// The bridge owns its pooled scope. Sequence step maps, XPath and legacy globals are unused.
	}

	@Override
	public void runCore() throws EngineException {
		var runStarted = System.nanoTime();
		// the context of this request: the field is reset when a request on this object ends, possibly during the run
		var requestContext = context;
		try {
			var response = new FlowEngineBridge().run(this, requestContext, null, null);
			var bridgeFinished = System.nanoTime();
			if (response instanceof com.twinsoft.convertigo.engine.flow.rows.StreamResult streamed) {
				var document = streamed.take();
				if (requestContext.nativeJson && requester instanceof com.twinsoft.convertigo.engine.requesters.JsonServletRequester) {
					requestContext.nativeRows = document;
				} else {
					// Decide the adapter before the first pull: XML, internal calls, REST
					// options, cache and billable paths consume once under explicit bounds.
					try (document) { response.put("result", document.collect()); }
					catch (com.twinsoft.convertigo.engine.flow.rows.RowStreamException error) {
						if (error.status() == 200) response.put("result", error.response());
						else response = new JSONObject().put("ok", false).put("error", error.error());
					}
				}
			}


			if (response.optBoolean("ok", false)) {
				var result = response.has("result") ? response.get("result") : new JSONObject();
				var rawResult = result;
				if (JSONObject.NULL.equals(result)) {
					result = new JSONObject();
				}
				// _flow.xml.arrayItem keeps the array elements legacy XPath consumers read, as "object"
				var xml = response.optJSONObject("xml");
				var arrayItem = xml == null ? "item" : xml.optString("arrayItem", "item");
				var rawBoundary = requester instanceof com.twinsoft.convertigo.engine.requesters.NativeJsonRequester nativeRequester && nativeRequester.isRawResult();
				var nativeResult = !requestContext.nativeJson ? null : rawBoundary ? new JSONObject().put("result", rawResult)
						: com.twinsoft.convertigo.engine.flow.NativeJsonSupport.projectResult(result, arrayItem);
				if (nativeResult != null) {
					requestContext.nativeJsonResponse = nativeResult;
				} else {
					if (requestContext.nativeRows != null) {
						try (var document = requestContext.takeNativeRows()) { result = document.collect(); }
					}
					XMLUtils.jsonToXml(result, null, ensureOutputDocument().getDocumentElement(), true, true, false, arrayItem);
				}
			} else {
				var errorResponse = new JSONObject();
				errorResponse.put("error", response.has("error") ? response.get("error") : response);
				if (requestContext.nativeJson) requestContext.nativeJsonResponse =
						requester instanceof com.twinsoft.convertigo.engine.requesters.NativeJsonRequester nativeRequester && nativeRequester.isRawResult()
						? new JSONObject().put("result", errorResponse) : errorResponse;
				else XMLUtils.jsonToXml(errorResponse, ensureOutputDocument().getDocumentElement());
				applyErrorStatus(requestContext, response.optJSONObject("error"));
			}
			var profile = response.optJSONObject("profile");
			if (profile != null) {
				profile.put("javaRunCore", new JSONObject()
						.put("bridgeMs", nanosToMillis(bridgeFinished - runStarted))
						.put("jsonToXmlMs", nanosToMillis(System.nanoTime() - bridgeFinished))
						.put("totalMs", nanosToMillis(System.nanoTime() - runStarted)));
				Engine.logBeans.info("(Flow) Profile for " + getQName() + ": " + com.twinsoft.convertigo.engine.flow.FlowProfileSummary.summarize(profile));
			}
		} catch (Exception e) {
			var rows = requestContext.takeNativeRows();
			if (rows != null) try { rows.close(); } catch (Exception ignored) { }
			throw new EngineException("Unable to run flow \"" + getName() + "\".", e);
		}
	}

	/**
	 * The HTTP status an error of the requested Flow carries (throw with a status, from 400 to 599), the body keeping the
	 * error: neither for a Flow another one calls (the request is the one of its caller), nor over a status already set.
	 */
	private void applyErrorStatus(com.twinsoft.convertigo.engine.Context requestContext, JSONObject error) {
		var status = error == null ? 0 : error.optInt("status", 0);
		var request = requestContext == null ? null : requestContext.httpServletRequest;
		if (status < 400 || status > 599 || request == null || request instanceof InternalHttpServletRequest
				|| !getName().equals(request.getParameter(Parameter.Sequence.getName()))
				|| RequestAttribute.responseStatus.has(request)) {
			return;
		}
		RequestAttribute.responseStatus.set(request, Map.of(status, ""));
	}

	private static double nanosToMillis(long nanos) {
		return nanos / 1_000_000d;
	}

	@Override
	public void setStatisticsOfRequestFromCache() {
		context.statistics.add(EngineStatistics.EXECUTE_SEQUENCE_STEPS, 0);
		context.statistics.add(EngineStatistics.EXECUTE_SEQUENCE_CALLS, 0);
	}

	@Override
	public List<DatabaseObject> getDatabaseObjectChildren() throws Exception {
		synchronizeFlowInputs();
		var children = new ArrayList<DatabaseObject>(super.getDatabaseObjectChildren());
		children.addAll(getFlowVirtualChildren());
		return children;
	}

	@Override
	public DatabaseObject getDatabaseObjectChild(String name) throws Exception {
		return FlowVirtualObject.findChild(getDatabaseObjectChildren(), name);
	}

	@Override
	public List<DatabaseObject> getAllChildren() {
		synchronizeFlowInputs();
		var children = new ArrayList<DatabaseObject>(super.getAllChildren());
		children.addAll(getFlowVirtualChildren());
		return children;
	}

	@Override
	public boolean hasDatabaseObjectChildren() throws Exception {
		synchronizeFlowInputs();
		return super.hasDatabaseObjectChildren() || !getFlowVirtualChildren().isEmpty();
	}

	/** The accessibility the source declares (_flow.accessibility), else the property: requesters check it first. */
	@Override
	public Accessibility getAccessibility() {
		synchronizeFlowInputs();
		return declaredAccessibility != null ? declaredAccessibility : super.getAccessibility();
	}

	@Override
	public boolean isPublicAccessibility() {
		return getAccessibility() == Accessibility.Public;
	}

	@Override
	public boolean isPrivateAccessibility() {
		return getAccessibility() == Accessibility.Private;
	}

	@Override
	public boolean isHiddenAccessibility() {
		return getAccessibility() == Accessibility.Hidden;
	}

	@Override
	public List<RequestableVariable> getVariablesList() {
		synchronizeFlowInputs();
		return super.getVariablesList();
	}

	@Override
	public Variable getVariable(int index) {
		synchronizeFlowInputs();
		return super.getVariable(index);
	}

	@Override
	public Variable getVariable(String variableName) {
		synchronizeFlowInputs();
		return super.getVariable(variableName);
	}

	@Override
	public boolean hasVariables() {
		synchronizeFlowInputs();
		return super.hasVariables();
	}

	@Override
	public int numberOfVariables() {
		synchronizeFlowInputs();
		return super.numberOfVariables();
	}

	public List<DatabaseObject> getFlowVirtualChildren() {
		var source = getFlowSource();
		var key = FlowEngineBridge.cacheGeneration() + "\n" + getQName() + "\n" + source;
		if (flowVirtualChildrenCache != null && key.equals(flowVirtualChildrenCacheKey)) {
			return new ArrayList<>(flowVirtualChildrenCache);
		}
		var children = FlowVirtualProjector.childrenOf(this);
		flowVirtualChildrenCacheKey = key;
		flowVirtualChildrenCache = children;
		return new ArrayList<>(children);
	}

	/**
	 * A required input left empty is refused by the Flow engine (FLOW_INPUT_REQUIRED, status 400) as for any caller,
	 * not by the native check of the sequence variables; the variable stays required for the API descriptions.
	 */
	@Override
	public Object getVariableValue(String requestedVariableName) throws EngineException {
		if (variables.get(requestedVariableName) == null) {
			var variable = getVariable(requestedVariableName);
			if (variable != null && Boolean.TRUE.equals(variable.isRequired()) && variable.getValueOrNull() == null) {
				return null;
			}
		}
		return super.getVariableValue(requestedVariableName);
	}

	public JSONObject getFlowInput() throws EngineException {
		try {
			synchronizeFlowInputs();
			var input = new JSONObject();
			mergeBodyInput(input, variables.get("__body"));
			for (var entry : variables.entrySet()) {
				var name = entry.getKey();
				if (!name.startsWith("__")) {
					input.put(name, toJsonValue(entry.getValue()));
				}
			}
			for (RequestableVariable variable : getVariablesList()) {
				var name = variable.getName();
				if (!name.startsWith("__") && !input.has(name)) {
					var value = getVariableValue(name);
					if (value != null) {
						input.put(name, toJsonValue(value));
					}
				}
			}
			return input;
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow input.", e);
		}
	}

	private void synchronizeFlowInputs() {
		var source = getFlowSource();
		if (flowInputSyncing || source.equals(flowInputSyncSource)) {
			return;
		}
		if (!source.contains("_flow")) {
			declaredAccessibility = null;
			flowInputSyncSource = source;
			return;
		}
		if (Boolean.FALSE.equals(hasDeclaredFlowInputs(source))) {
			declaredAccessibility = null;
			flowInputSyncSource = source;
			return;
		}
		flowInputSyncing = true;
		try {
			var response = new FlowEngineBridge().syncInputs(this);
			if (response.optBoolean("ok", false)) {
				validateFlowInputResponseMetadata(response);
				var inputDefinitions = response.optJSONObject("inputDefinitions");
				var hasInputs = inputDefinitions != null && inputDefinitions.length() > 0;
				if (hasInputs) {
					syncFlowInputDefinitions(inputDefinitions);
				}
				// jettison reads a JSON null as a null text
				var accessibility = response.optString("accessibility", "");
				declaredAccessibility = accessibility == null || accessibility.isEmpty() || "null".equals(accessibility) ? null : Accessibility.valueOf(accessibility);
				flowInputSyncSource = source;
			} else {
				var error = response.optJSONObject("error");
				var message = error == null ? response.toString() : error.optString("message", error.toString());
				Engine.logBeans.warn("(Flow) Unable to synchronize Flow inputs for \"" + getQName() + "\": " + message);
				flowInputSyncSource = source;
			}
		} catch (Exception e) {
			Engine.logBeans.warn("(Flow) Unable to synchronize Flow inputs for \"" + getQName() + "\".", e);
			flowInputSyncSource = source;
		} finally {
			flowInputSyncing = false;
		}
	}

	private static Boolean hasDeclaredFlowInputs(String source) {
		try {
			var environs = new CompilerEnvirons();
			environs.setLanguageVersion(Context.VERSION_ES6);
			environs.setRecoverFromErrors(false);
			var root = new Parser(environs).parse(source, "FlowScript", 1);
			var result = new Boolean[1];
			root.visit(node -> {
				if (result[0] != null || !(node instanceof VariableInitializer initializer)
						|| !(initializer.getTarget() instanceof Name target) || !"_flow".equals(target.getIdentifier())
						|| !(initializer.getInitializer() instanceof ObjectLiteral metadata)) {
					return result[0] == null;
				}
				result[0] = false;
				for (var element : metadata.getElements()) {
					if (!(element instanceof ObjectProperty property)) {
						result[0] = null;
						return false;
					}
					var key = property.getKey();
					var name = key instanceof Name identifier ? identifier.getIdentifier()
							: key instanceof StringLiteral literal ? literal.getValue() : "";
					if ("input".equals(name) || "inputs".equals(name) || "accessibility".equals(name)
							|| "authenticatedContextRequired".equals(name)) {
						result[0] = true;
						return false;
					}
				}
				return false;
			});
			return result[0];
		} catch (Exception e) {
			return null;
		}
	}

	private void syncFlowInputDefinitions(JSONObject inputDefinitions) throws EngineException, JSONException {
		// Complete validation precedes removals too: a bad later input must leave
		// every previously loaded variable and property untouched.
		validateFlowInputDefinitions(inputDefinitions);
		var changed = false;
		var declaredNames = new HashSet<String>();
		for (var keys = inputDefinitions.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			if (key.matches("[A-Za-z_][A-Za-z0-9_]*") && !key.startsWith("__")) {
				declaredNames.add(key);
			}
		}
		for (var variable : new ArrayList<>(getVariablesList())) {
			if (!variable.getName().startsWith("__") && !declaredNames.contains(variable.getName())) {
				removeVariable(variable);
				changed = true;
			}
		}
		for (var keys = inputDefinitions.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			if (!key.matches("[A-Za-z_][A-Za-z0-9_]*") || key.startsWith("__")) {
				continue;
			}
			var definition = inputDefinitions.getJSONObject(key);
			var type = flowInputType(definition, "_flow.inputs." + key);
			var existing = super.getVariable(key);
			var variable = existing instanceof RequestableVariable requestable ? requestable : null;
			var variableChanged = false;
			if (variable == null) {
				variable = isFlowInputMultiValued(type, definition) ? new RequestableMultiValuedVariable() : new RequestableVariable();
				variable.setName(key);
				variableChanged = true;
			}
			if (definition.has("description")) {
				variableChanged |= setStringIfChanged(variable.getDescription(), definition.getString("description"), variable::setDescription);
			} else if (existing == null) {
				variable.setDescription("Flow input " + key);
			}
			if (definition.has("default")) {
				variableChanged |= setDefaultIfChanged(variable,
						flowInputDefaultValue(definition.opt("default"), variable instanceof RequestableMultiValuedVariable));
			}
			if (definition.has("required")) {
				variableChanged |= setBooleanIfChanged(variable.isRequired(), (Boolean) definition.get("required"), variable::setRequired);
			}
			if (definition.has("isFileUpload")) {
				variableChanged |= setBooleanIfChanged(variable.getIsFileUpload(), (Boolean) definition.get("isFileUpload"), variable::setIsFileUpload);
			}
			if (definition.has("visibility")) {
				var visibility = ((Number) definition.get("visibility")).intValue();
				if (variable.getVisibility() != visibility) {
					variable.setVisibility(visibility);
					variableChanged = true;
				}
			}
			variableChanged |= setStringIfChanged(variable.getSchemaType(), flowInputSchemaType(type), variable::setSchemaType);
			if (existing == null) {
				addVariable(variable);
				changed = true;
			} else {
				changed |= variableChanged;
			}
		}
		if (changed) {
			changed();
		}
	}

	// Convertigo variables persist scalars and XMLVector rows. A JSON array or object
	// default must never reach Variable.setValueOrNull as a Jettison object: it would be
	// stored as a serialized Java object that the deserialization filter rejects on
	// the next project load. Rows become scalars, nested structures become JSON text.
	static Object flowInputDefaultValue(Object value, boolean multiValued) {
		if (value == null || JSONObject.NULL.equals(value)) {
			return null;
		}
		if (!multiValued) {
			return value instanceof JSONObject || value instanceof JSONArray ? value.toString() : value;
		}
		var rows = new java.util.ArrayList<Object>();
		if (value instanceof JSONArray array) {
			for (int i = 0; i < array.length(); i++) {
				var item = array.opt(i);
				if (item == null || JSONObject.NULL.equals(item)) {
					continue;
				}
				rows.add(item instanceof JSONObject || item instanceof JSONArray ? item.toString() : item);
			}
		} else {
			rows.add(value instanceof JSONObject ? value.toString() : value);
		}
		return rows;
	}

	private boolean isFlowInputMultiValued(String type, JSONObject definition) throws JSONException {
		return "array".equals(type) || definition.has("multi") && (Boolean) definition.get("multi")
				|| definition.has("multiValued") && (Boolean) definition.get("multiValued");
	}

	private String flowInputSchemaType(String type) throws EngineException {
		return switch (type) {
		case "boolean" -> "xsd:boolean";
		case "integer" -> "xsd:integer";
		case "number" -> "xsd:double";
		case "string", "array", "object", "any", "unknown", "null" -> "xsd:string";
		default -> throw invalidFlowProperty("_flow.inputs.type", type, "a canonical JSON type");
		};
	}

	// The JS contract owns safe spelling normalization, source lines and typed
	// default conversion. Java only replays its canonical response, never coerces it.
	static void validateFlowInputResponseMetadata(JSONObject response) throws EngineException, JSONException {
		if (response.has("inputDefinitions")) {
			if (!(response.get("inputDefinitions") instanceof JSONObject definitions)) {
				throw invalidFlowProperty("inputDefinitions", response.get("inputDefinitions"), "an object of input schemas");
			}
			validateFlowInputDefinitions(definitions);
		}
		if (response.has("accessibility")) {
			var value = response.get("accessibility");
			if (!(value instanceof String name) || !List.of("Public", "Hidden", "Private").contains(name)) {
				throw invalidFlowProperty("_flow.accessibility", value, "one of Public, Hidden, Private");
			}
		}
		if (response.has("flowMetadata")) {
			if (!(response.get("flowMetadata") instanceof JSONObject metadata)) {
				throw invalidFlowProperty("flowMetadata", response.get("flowMetadata"), "an object");
			}
			requireFlowBoolean(metadata, "authenticatedContextRequired", "_flow");
		}
	}

	static void validateFlowInputDefinitions(JSONObject definitions) throws EngineException, JSONException {
		for (var keys = definitions.keys(); keys.hasNext();) {
			var name = String.valueOf(keys.next());
			var path = "_flow.inputs." + name;
			if (!name.matches("[A-Za-z_][A-Za-z0-9_]*") || name.startsWith("__")) {
				throw invalidFlowProperty(path, name, "a native input name matching [A-Za-z_][A-Za-z0-9_]* without the reserved __ prefix");
			}
			if (!(definitions.get(name) instanceof JSONObject definition)) {
				throw invalidFlowProperty(path, definitions.get(name), "an input schema object");
			}
			flowInputType(definition, path);
			for (var property : List.of("required", "multi", "multiValued", "isFileUpload", "nullable")) {
				requireFlowBoolean(definition, property, path);
			}
			if (definition.has("description") && !(definition.get("description") instanceof String)) {
				throw invalidFlowProperty(path + ".description", definition.get("description"), "text");
			}
			if (definition.has("visibility")) {
				var value = definition.get("visibility");
				if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())
						|| number.doubleValue() != Math.rint(number.doubleValue())
						|| number.doubleValue() < -1 || number.doubleValue() > 15) {
					throw invalidFlowProperty(path + ".visibility", value, "-1 or an integer bitmask from 0 to 15");
				}
			}
			if (definition.has("default")) {
				validateFlowDefault(definition.get("default"), path + ".default");
			}
		}
	}

	private static String flowInputType(JSONObject schema, String path) throws EngineException, JSONException {
		var value = schema.has("type") ? schema.get("type") : schema.has("kind") ? schema.get("kind") : "string";
		if (value instanceof JSONArray types) {
			if (types.length() == 0) throw invalidFlowProperty(path + ".type", value, "at least one JSON type");
			for (int i = 0; i < types.length(); i++) requireFlowType(types.get(i), path + ".type");
			return types.length() == 1 ? (String) types.get(0) : "any";
		}
		return requireFlowType(value, path + ".type");
	}

	private static String requireFlowType(Object value, String path) throws EngineException {
		if (value instanceof String type && List.of("string", "integer", "number", "boolean", "array", "object", "any", "unknown", "null").contains(type)) return type;
		throw invalidFlowProperty(path, value, "one of string, integer, number, boolean, array, object, any, unknown, null");
	}

	private static void requireFlowBoolean(JSONObject object, String property, String path) throws EngineException, JSONException {
		if (object.has(property) && !(object.get(property) instanceof Boolean)) {
			throw invalidFlowProperty(path + "." + property, object.get(property), "true or false (canonical boolean)");
		}
	}

	private static void validateFlowDefault(Object value, String path) throws EngineException, JSONException {
		if (value == null || value == JSONObject.NULL || value instanceof String || value instanceof Boolean) return;
		if (value instanceof Number number && Double.isFinite(number.doubleValue())) return;
		if (value instanceof JSONObject object) {
			for (var keys = object.keys(); keys.hasNext();) { var key = String.valueOf(keys.next()); validateFlowDefault(object.get(key), path + "." + key); }
			return;
		}
		if (value instanceof JSONArray array) {
			for (int i = 0; i < array.length(); i++) validateFlowDefault(array.get(i), path + "[" + i + "]");
			return;
		}
		throw invalidFlowProperty(path, value, "JSON literal data (finite numbers); typed default compatibility is checked by the JS contract");
	}

	private static EngineException invalidFlowProperty(String path, Object value, String expected) {
		var given = value instanceof String text ? JSONObject.quote(text) : String.valueOf(value);
		return new EngineException("FLOW_NATIVE_PROPERTY_INVALID: " + path + " takes " + expected + ", not " + given + ".");
	}

	private boolean setStringIfChanged(String current, String next, java.util.function.Consumer<String> setter) {
		if (next == null || sameValue(current, next)) {
			return false;
		}
		setter.accept(next);
		return true;
	}

	private boolean setBooleanIfChanged(Boolean current, boolean next, java.util.function.Consumer<Boolean> setter) {
		if (Boolean.valueOf(next).equals(current)) {
			return false;
		}
		setter.accept(next);
		return true;
	}

	// A default naming symbols (${name}) stays the source value of the variable, saved as is, and is used compiled,
	// as the bean properties of the former sequences: compare with that source value, compile what is set.
	private boolean setDefaultIfChanged(RequestableVariable variable, Object next) {
		var source = variable.getCompilablePropertySourceValue("value");
		if (sameValue(source != null ? source : variable.getValueOrNull(), next)) {
			return false;
		}
		if (next instanceof String) {
			variable.setValueOrNull(variable.compileProperty("value", next));
		} else {
			variable.removeCompilablePropertySourceValue("value");
			variable.setValueOrNull(next);
		}
		return true;
	}

	private boolean sameValue(Object current, Object next) {
		if (current == next) {
			return true;
		}
		if (current == null || next == null) {
			return false;
		}
		return String.valueOf(current).equals(String.valueOf(next));
	}

	public String getFlowSource() {
		if (flowSourceDraft != null) {
			return flowSourceDraft.isBlank() ? DEFAULT_FLOW_SOURCE : flowSourceDraft;
		}
		loadFlowSourceFile();
		return flowSource == null || flowSource.isBlank() ? DEFAULT_FLOW_SOURCE : flowSource;
	}

	public void setFlowSource(String flowSource) {
		if (flowSource == null || flowSource.isBlank()) {
			flowSource = DEFAULT_FLOW_SOURCE;
		}
		var savedSource = getSavedFlowSource();
		if (savedSource.equals(flowSource)) {
			if (flowSourceDraft != null) {
				flowSourceDraft = null;
				clearFlowVirtualChildrenCache();
				changed();
			}
			return;
		}
		if (!getFlowSource().equals(flowSource)) {
			flowSourceDraft = flowSource;
			clearFlowVirtualChildrenCache();
			changed();
		}
	}

	public boolean isFlowSourceDirty() {
		return flowSourceDraft != null;
	}

	public String getSavedFlowSource() {
		loadFlowSourceFile();
		return flowSource == null || flowSource.isBlank() ? DEFAULT_FLOW_SOURCE : flowSource;
	}

	public void discardFlowSource() {
		if (flowSourceDraft != null) {
			flowSourceDraft = null;
			loadFlowSourceFile();
			clearFlowVirtualChildrenCache();
			changed();
		}
	}

	public void saveFlowSourceFile() throws EngineException {
		writeFlowSourceFile();
	}

	public boolean isIncludeTrace() {
		return includeTrace;
	}

	public void setIncludeTrace(boolean includeTrace) {
		if (this.includeTrace != includeTrace) {
			this.includeTrace = includeTrace;
			changed();
		}
	}

	@Override
	protected String defaultBeanName(String displayName) {
		return "flow";
	}

	public File getFlowSourceFile() {
		var project = getProject();
		var name = getName();
		if (project == null || name == null || name.isBlank()) {
			return null;
		}
		return new File(new File(project.getDirFile(), sourceLayout().flows()), name + ".flow.js");
	}

	protected FlowSourceLayout sourceLayout() {
		return FlowSourceLayout.current();
	}

	private void loadFlowSourceFile() {
		var file = getFlowSourceFile();
		if (file == null || !file.isFile()) {
			return;
		}
		var lastModified = file.lastModified();
		if (lastModified == flowSourceFileLastModified) {
			return;
		}
		try {
			flowSource = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
			flowSourceFileLastModified = lastModified;
			clearFlowVirtualChildrenCache();
		} catch (Exception e) {
			Engine.logBeans.warn("Unable to read Flow source file \"" + file.getAbsolutePath() + "\".", e);
		}
	}

	private void writeFlowSourceFile() throws EngineException {
		var file = getFlowSourceFile();
		if (file == null) {
			return;
		}
		try {
			sourceLayout().ensureHttpIgnore(getProject().getDirFile());
			file.getParentFile().mkdirs();
			var source = getFlowSource();
			FileUtils.writeStringToFile(file, source, StandardCharsets.UTF_8);
			flowSource = source;
			flowSourceDraft = null;
			flowSourceFileLastModified = file.lastModified();
			clearFlowVirtualChildrenCache();
			var legacyFile = new File(file.getParentFile(), getName() + ".flow.yaml");
			if (legacyFile != null && legacyFile.isFile()) {
				legacyFile.delete();
			}
		} catch (Exception e) {
			throw new EngineException("Unable to write Flow source file \"" + file.getAbsolutePath() + "\".", e);
		}
	}

	private void clearFlowVirtualChildrenCache() {
		flowVirtualChildrenCacheKey = "";
		flowVirtualChildrenCache = null;
	}

	private static void removeSerializedProperty(Element element, String propertyName) {
		var properties = element.getChildNodes();
		for (var i = properties.getLength() - 1; i >= 0; i--) {
			var node = properties.item(i);
			if (node instanceof Element property
					&& "property".equals(property.getTagName())
					&& propertyName.equals(property.getAttribute("name"))) {
				element.removeChild(property);
			}
		}
	}

	private static void mergeBodyInput(JSONObject input, Object body) throws JSONException {
		if (body == null) {
			return;
		}
		var value = body instanceof List<?> list && !list.isEmpty() ? list.get(0) : body;
		if (!(value instanceof String text) || text.isBlank()) {
			input.put("body", toJsonValue(value));
			return;
		}
		try {
			var parsed = new JSONTokener(text.trim()).nextValue();
			if (parsed instanceof JSONObject json) {
				for (var it = json.keys(); it.hasNext();) {
					var key = String.valueOf(it.next());
					input.put(key, json.opt(key));
				}
			} else {
				input.put("body", parsed);
			}
		} catch (Exception e) {
			input.put("body", text);
		}
	}

	static Object toJsonValue(Object value) throws JSONException {
		if (value == null) {
			return JSONObject.NULL;
		}
		if (value instanceof JSONObject || value instanceof JSONArray || value instanceof Number || value instanceof Boolean || value instanceof String) {
			return value;
		}
		if (value instanceof List<?> list) {
			var array = new JSONArray();
			for (var item : list) {
				array.put(toJsonValue(item));
			}
			return array;
		}
		if (value instanceof Object[] array) {
			var json = new JSONArray();
			for (var item : array) {
				json.put(toJsonValue(item));
			}
			return json;
		}
		if (value instanceof Element element) {
			// Xerces DOM nodes also implement NodeList. Handle the concrete node
			// before the collection interface or text values are mistaken for
			// sparse character arrays. docChildNodes removes the transport wrapper
			// (<item>) and preserves the XML type annotations.
			if ("variable".equals(element.getTagName()) && !element.hasAttribute("value")) {
				var children = new JSONArray();
				for (var child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
					if (child instanceof Element childElement) {
						children.put(toJsonValue(childElement));
					}
				}
				if (children.length() == 1) {
					return children.get(0);
				}
				return children;
			}
			return new JSONTokener(XMLUtils.XmlToJson(element, true, true, JsonRoot.docChildNodes)).nextValue();
		}
		if (value instanceof Node node) {
			return node.getTextContent();
		}
		if (value instanceof NodeList nodes) {
			if (nodes.getLength() == 1) {
				return toJsonValue(nodes.item(0));
			}
			var array = new JSONArray();
			for (int i = 0; i < nodes.getLength(); i++) {
				var node = nodes.item(i);
				if (node != null && (node.getNodeType() != Node.TEXT_NODE || !node.getTextContent().isBlank())) {
					array.put(toJsonValue(node));
				}
			}
			return array;
		}
		if (value instanceof Map<?, ?> map) {
			var json = new JSONObject();
			for (var entry : map.entrySet()) {
				json.put(String.valueOf(entry.getKey()), toJsonValue(entry.getValue()));
			}
			return json;
		}
		return String.valueOf(value);
	}

	public void addFlowOutputSchema(XmlSchema schema, XmlSchemaSequence sequence) throws EngineException, JSONException {
		var response = new FlowEngineBridge().outputSchema(this);
		if (!response.optBoolean("ok", false)) {
			if (response.has("error")) {
				Engine.logBeans.warn("Flow output schema engine returned an error for \"" + getQName() + "\": " + response.opt("error"));
			}
			return;
		}

		var flowSchema = response.opt("schema");
		if (flowSchema == null || JSONObject.NULL.equals(flowSchema)) {
			return;
		}
		addSchemaChildren(schema, sequence, flowSchema);
	}

	private void addSchemaChildren(XmlSchema schema, XmlSchemaSequence sequence, Object flowSchema) throws JSONException {
		var normalized = normalizeSchema(flowSchema);
		var type = schemaType(normalized);
		if ("object".equals(type)) {
			var properties = schemaProperties(normalized);
			if (properties == null || properties.length() == 0) {
				return;
			}
			for (var it = properties.keys(); it.hasNext();) {
				var name = String.valueOf(it.next());
				sequence.getItems().add(schemaElement(schema, name, properties.opt(name)));
			}
			return;
		}

		sequence.getItems().add(schemaElement(schema, "value", normalized));
	}

	private XmlSchemaElement schemaElement(XmlSchema schema, String name, Object flowSchema) throws JSONException {
		var element = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaElement());
		var elementName = xmlName(name);
		element.setName(elementName);
		element.setMinOccurs(0);

		applyElementSchema(schema, element, normalizeSchema(flowSchema), originalKeyName(name, elementName));
		return element;
	}

	private void applyElementSchema(XmlSchema schema, XmlSchemaElement element, Object flowSchema, String originalKeyName) throws JSONException {
		var normalized = normalizeSchema(flowSchema);
		var type = schemaType(normalized);
		if ("object".equals(type)) {
			var complexType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
			var sequence = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSequence());
			complexType.setParticle(sequence);
			element.setType(complexType);
			addJsonMetadataAttributes(complexType.getAttributes(), "object", false, originalKeyName);

			var properties = schemaProperties(normalized);
			if (properties != null) {
				for (var it = properties.keys(); it.hasNext();) {
					var name = String.valueOf(it.next());
					sequence.getItems().add(schemaElement(schema, name, properties.opt(name)));
				}
			}
			return;
		}
		if ("array".equals(type)) {
			var complexType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
			var sequence = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSequence());
			complexType.setParticle(sequence);
			element.setType(complexType);
			addJsonMetadataAttributes(complexType.getAttributes(), "array", true, originalKeyName);

			var item = schemaElement(schema, "item", schemaItems(normalized));
			item.setMaxOccurs(Long.MAX_VALUE);
			sequence.getItems().add(item);
			return;
		}

		var complexType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
		var simpleContent = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSimpleContent());
		var extension = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSimpleContentExtension());
		extension.setBaseTypeName(schemaTypeName(type));
		addJsonMetadataAttributes(extension.getAttributes(), type, false, originalKeyName);
		simpleContent.setContent(extension);
		complexType.setContentModel(simpleContent);
		element.setType(complexType);
	}

	private void addJsonMetadataAttributes(XmlSchemaObjectCollection attributes, String type, boolean array, String originalKeyName) {
		addOptionalAttribute(attributes, "type", type);
		if (originalKeyName != null && !originalKeyName.isBlank()) {
			addOptionalAttribute(attributes, "originalKeyName", originalKeyName);
		}
		if (array) {
			addOptionalAttribute(attributes, "length", null);
		}
	}

	private void addOptionalAttribute(XmlSchemaObjectCollection attributes, String name, String defaultValue) {
		var attribute = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaAttribute());
		attribute.setName(name);
		attribute.setSchemaTypeName(Constants.XSD_STRING);
		if (defaultValue != null && !defaultValue.isBlank()) {
			attribute.setDefaultValue(defaultValue);
		}
		attribute.setUse(XmlSchemaUtils.attributeUseOptional);
		attributes.add(attribute);
	}

	private static Object normalizeSchema(Object flowSchema) throws JSONException {
		if (flowSchema == null || JSONObject.NULL.equals(flowSchema)) {
			return new JSONObject().put("type", "unknown");
		}
		if (flowSchema instanceof String type) {
			return new JSONObject().put("type", type);
		}
		if (flowSchema instanceof Number) {
			return new JSONObject().put("type", "number");
		}
		if (flowSchema instanceof Boolean) {
			return new JSONObject().put("type", "boolean");
		}
		if (flowSchema instanceof JSONArray) {
			return new JSONObject().put("type", "string");
		}
		return flowSchema;
	}

	private static String schemaType(Object flowSchema) throws JSONException {
		if (flowSchema instanceof JSONObject json) {
			var type = json.optString("type", "");
			if (!type.isBlank()) {
				return type;
			}
			return "object";
		}
		if (flowSchema instanceof String type) {
			return type;
		}
		if (flowSchema instanceof Number) {
			return "number";
		}
		if (flowSchema instanceof Boolean) {
			return "boolean";
		}
		return "unknown";
	}

	private static JSONObject schemaProperties(Object flowSchema) {
		if (flowSchema instanceof JSONObject json) {
			var properties = json.opt("properties");
			if (properties instanceof JSONObject object) {
				return object;
			}
			return json.has("type") ? new JSONObject() : json;
		}
		return null;
	}

	private static Object schemaItems(Object flowSchema) throws JSONException {
		if (flowSchema instanceof JSONObject json && json.has("items")) {
			return json.get("items");
		}
		return new JSONObject().put("type", "unknown");
	}

	private static QName schemaTypeName(String type) {
		return switch (type == null ? "" : type) {
		case "boolean" -> Constants.XSD_BOOLEAN;
		case "integer" -> Constants.XSD_INTEGER;
		case "number" -> Constants.XSD_DOUBLE;
		default -> Constants.XSD_STRING;
		};
	}

	private static String xmlName(String name) {
		var xmlName = StringUtils.normalize(name == null || name.isBlank() ? "value" : name);
		return xmlName == null || xmlName.isBlank() ? "value" : xmlName;
	}

	private static String originalKeyName(String name, String xmlName) {
		var original = name == null || name.isBlank() ? "value" : name;
		return original.equals(xmlName) ? null : original;
	}
}
