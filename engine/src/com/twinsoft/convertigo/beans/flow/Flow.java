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
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

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
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.XmlSchemaUtils;

public class Flow extends Sequence {

	private static final long serialVersionUID = 1258757999474893551L;
	private static final String DEFAULT_FLOW_SOURCE = "function Flow({ input, config, result }) {\n"
			+ "  return result\n"
			+ "}\n";
	private static final Object planPreparationLock = new Object();
	private static final Map<String, FlowPlanPreparation> flowPlanPreparations = new LinkedHashMap<>();
	private static final Map<String, Long> preparedRuntimeGenerations = new LinkedHashMap<>();
	private static final AtomicLong planPreparationGeneration = new AtomicLong();

	private record FlowPlanPreparation(
			String projectName,
			String engineQName,
			WeakReference<Flow> flowReference,
			boolean ready,
			long lastPreparedGeneration) {
	}

	private String flowSource = DEFAULT_FLOW_SOURCE;
	private boolean includeTrace = false;
	private transient String flowSourceDraft = null;
	private transient long flowSourceFileLastModified = -1;
	private transient String flowInputSyncSource = null;
	private transient boolean flowInputSyncing = false;
	private transient String flowVirtualChildrenCacheKey = "";
	private transient List<DatabaseObject> flowVirtualChildrenCache = null;

	public Flow() {
		super();
	}

	@Override
	public void setParent(DatabaseObject databaseObject) {
		super.setParent(databaseObject);
		if (databaseObject == null || !isOriginal()) {
			return;
		}
		var project = getProject();
		if (project == null) {
			return;
		}
		var engineQName = effectiveEngineQName();
		var flowQName = getQName();
		synchronized (planPreparationLock) {
			flowPlanPreparations.put(flowQName, new FlowPlanPreparation(
					project.getName(), engineQName, new WeakReference<>(this), !isImporting, -1));
		}
		if (isImporting) {
			com.twinsoft.convertigo.engine.DatabaseObjectsManager.getProjectLoadingData()
					.addAfterLoaded(() -> markPlanPreparationReady(flowQName, this));
		} else {
			prepareReadyFlows(engineQName);
		}
	}

	public static void runtimePrepared(String engineQName) {
		synchronized (planPreparationLock) {
			preparedRuntimeGenerations.put(engineQName, planPreparationGeneration.incrementAndGet());
		}
		prepareReadyFlows(engineQName);
	}

	public static void projectUnloaded(Project project) {
		if (project == null) {
			return;
		}
		var projectName = project.getName();
		var flowEngine = project.getFlowEngine();
		var ownedEngineQName = flowEngine == null ? null : flowEngine.getEngineQName();
		if (ownedEngineQName != null && !ownedEngineQName.startsWith(projectName + ".")) {
			ownedEngineQName = null;
		}
		synchronized (planPreparationLock) {
			flowPlanPreparations.values().removeIf(preparation -> projectName.equals(preparation.projectName()));
			if (ownedEngineQName != null) {
				preparedRuntimeGenerations.remove(ownedEngineQName);
			}
			removeReleasedFlows();
		}
	}

	private static void markPlanPreparationReady(String flowQName, Flow flow) {
		var engineQName = flow.effectiveEngineQName();
		synchronized (planPreparationLock) {
			var preparation = flowPlanPreparations.get(flowQName);
			if (preparation != null && preparation.flowReference().get() == flow) {
				flowPlanPreparations.put(flowQName, new FlowPlanPreparation(
						preparation.projectName(), engineQName, preparation.flowReference(), true,
						preparation.lastPreparedGeneration()));
			} else {
				engineQName = null;
			}
		}
		if (engineQName != null) {
			prepareReadyFlows(engineQName);
		}
	}

	private static void prepareReadyFlows(String engineQName) {
		List<Flow> flows = new ArrayList<>();
		synchronized (planPreparationLock) {
			var runtimeGeneration = preparedRuntimeGenerations.get(engineQName);
			if (runtimeGeneration == null) {
				return;
			}
			var iterator = flowPlanPreparations.entrySet().iterator();
			while (iterator.hasNext()) {
				var entry = iterator.next();
				var preparation = entry.getValue();
				var flow = preparation.flowReference().get();
				if (flow == null) {
					iterator.remove();
				} else if (preparation.ready() && engineQName.equals(preparation.engineQName())
						&& preparation.lastPreparedGeneration() < runtimeGeneration) {
					entry.setValue(new FlowPlanPreparation(
							preparation.projectName(), engineQName, preparation.flowReference(), true,
							runtimeGeneration));
					flows.add(flow);
				}
			}
		}
		for (var flow : flows) {
			flow.prepareFlowPlan();
		}
	}

	private static void removeReleasedFlows() {
		var iterator = flowPlanPreparations.values().iterator();
		while (iterator.hasNext()) {
			if (iterator.next().flowReference().get() == null) {
				iterator.remove();
			}
		}
	}

	private String effectiveEngineQName() {
		var project = getProject();
		var flowEngine = project == null ? null : project.getFlowEngine();
		var engineQName = flowEngine == null ? null : flowEngine.getEngineQName();
		return engineQName == null || engineQName.isBlank() ? FlowEngineBridge.DEFAULT_ENGINE_QNAME : engineQName;
	}

	private void prepareFlowPlan() {
		try {
			var result = new FlowEngineBridge().prepare(this);
			Engine.logBeans.info("(Flow) Prepared " + getQName() + " in "
					+ Math.round(result.optDouble("durationMs")) + " ms");
		} catch (Exception e) {
			Engine.logBeans.warn("(Flow) Unable to prepare " + getQName(), e);
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
		writeFlowSourceFile();
		var element = super.toXml(document);
		removeSerializedProperty(element, "flowSource");
		return element;
	}

	@Override
	public void runCore() throws EngineException {
		var runStarted = System.nanoTime();
		try {
			var response = new FlowEngineBridge().run(this, context, null, null);
			var bridgeFinished = System.nanoTime();
			var root = context.outputDocument.getDocumentElement();
			if (response.optBoolean("ok", false)) {
				var result = response.has("result") ? response.get("result") : new JSONObject();
				if (JSONObject.NULL.equals(result)) {
					result = new JSONObject();
				}
				XMLUtils.jsonToXml(result, root);
			} else {
				var errorResponse = new JSONObject();
				errorResponse.put("error", response.has("error") ? response.get("error") : response);
				XMLUtils.jsonToXml(errorResponse, root);
			}
			var profile = response.optJSONObject("profile");
			if (profile != null) {
				profile.put("javaRunCore", new JSONObject()
						.put("bridgeMs", nanosToMillis(bridgeFinished - runStarted))
						.put("jsonToXmlMs", nanosToMillis(System.nanoTime() - bridgeFinished))
						.put("totalMs", nanosToMillis(System.nanoTime() - runStarted)));
				Engine.logBeans.info("(Flow) Profile for " + getQName() + ": " + profile);
			}
		} catch (Exception e) {
			throw new EngineException("Unable to run flow \"" + getName() + "\".", e);
		}
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
			flowInputSyncSource = source;
			return;
		}
		if (Boolean.FALSE.equals(hasDeclaredFlowInputs(source))) {
			flowInputSyncSource = source;
			return;
		}
		flowInputSyncing = true;
		try {
			var response = new FlowEngineBridge().syncInputs(this);
			if (response.optBoolean("ok", false)) {
				var inputDefinitions = response.optJSONObject("inputDefinitions");
				var hasInputs = inputDefinitions != null && inputDefinitions.length() > 0;
				if (hasInputs) {
					syncFlowInputDefinitions(inputDefinitions);
				}
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
					if ("input".equals(name) || "inputs".equals(name)) {
						result[0] = !(property.getValue() instanceof ObjectLiteral inputs) || !inputs.getElements().isEmpty();
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
		var changed = false;
		for (var keys = inputDefinitions.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			if (!key.matches("[A-Za-z_$][\\w$]*") || key.startsWith("__")) {
				continue;
			}
			var definition = inputDefinitions.optJSONObject(key);
			if (definition == null) {
				definition = new JSONObject();
			}
			var type = definition.optString("type", definition.optString("kind", "string")).toLowerCase();
			var existing = super.getVariable(key);
			var variable = existing instanceof RequestableVariable requestable ? requestable : null;
			var variableChanged = false;
			if (variable == null) {
				variable = isFlowInputMultiValued(type, definition) ? new RequestableMultiValuedVariable() : new RequestableVariable();
				variable.setName(key);
				variableChanged = true;
			}
			if (definition.has("description") && !definition.optString("description").isBlank()) {
				variableChanged |= setStringIfChanged(variable.getDescription(), definition.optString("description"), variable::setDescription);
			} else if (existing == null) {
				variable.setDescription("Flow input " + key);
			}
			if (definition.has("default")) {
				variableChanged |= setValueIfChanged(variable.getValueOrNull(), definition.opt("default"), variable::setValueOrNull);
			}
			if (definition.has("required")) {
				variableChanged |= setBooleanIfChanged(variable.isRequired(), definition.optBoolean("required"), variable::setRequired);
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

	private boolean isFlowInputMultiValued(String type, JSONObject definition) {
		return "array".equals(type) || definition.optBoolean("multi", false) || definition.optBoolean("multiValued", false);
	}

	private String flowInputSchemaType(String type) {
		return switch (type == null ? "" : type.toLowerCase()) {
		case "boolean", "bool" -> "xsd:boolean";
		case "integer", "int" -> "xsd:integer";
		case "number", "double", "float" -> "xsd:double";
		default -> "xsd:string";
		};
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

	private boolean setValueIfChanged(Object current, Object next, java.util.function.Consumer<Object> setter) {
		if (sameValue(current, next)) {
			return false;
		}
		setter.accept(next);
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
		return new File(new File(project.getDirFile(), "libs/flows"), name + ".flow.js");
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

	private static Object toJsonValue(Object value) throws JSONException {
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
		if (value instanceof NodeList nodes) {
			if (nodes.getLength() == 1) {
				return toJsonValue(nodes.item(0));
			}
			var array = new JSONArray();
			for (int i = 0; i < nodes.getLength(); i++) {
				array.put(toJsonValue(nodes.item(i)));
			}
			return array;
		}
		if (value instanceof Element element) {
			return new JSONTokener(XMLUtils.XmlToJson(element, true)).nextValue();
		}
		if (value instanceof Node node) {
			return node.getTextContent();
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
