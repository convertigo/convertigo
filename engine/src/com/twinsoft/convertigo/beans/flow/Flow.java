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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
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

	private String flowSource = DEFAULT_FLOW_SOURCE;
	private boolean includeTrace = true;
	private transient boolean flowSourceDirty = false;
	private transient long flowSourceFileLastModified = -1;

	public Flow() {
		super();
	}

	@Override
	public Flow clone() throws CloneNotSupportedException {
		return (Flow) super.clone();
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
		try {
			var response = new FlowEngineBridge().run(this, context, runningThread.javascriptContext, scope);
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
		} catch (Exception e) {
			throw new EngineException("Unable to run flow \"" + getName() + "\".", e);
		}
	}

	@Override
	public void setStatisticsOfRequestFromCache() {
		context.statistics.add(EngineStatistics.EXECUTE_SEQUENCE_STEPS, 0);
		context.statistics.add(EngineStatistics.EXECUTE_SEQUENCE_CALLS, 0);
	}

	@Override
	public List<DatabaseObject> getDatabaseObjectChildren() throws Exception {
		var children = new ArrayList<DatabaseObject>(super.getDatabaseObjectChildren());
		children.addAll(getFlowVirtualChildren());
		return children;
	}

	@Override
	public List<DatabaseObject> getAllChildren() {
		var children = new ArrayList<DatabaseObject>(super.getAllChildren());
		children.addAll(getFlowVirtualChildren());
		return children;
	}

	@Override
	public boolean hasDatabaseObjectChildren() throws Exception {
		return super.hasDatabaseObjectChildren() || !getFlowVirtualChildren().isEmpty();
	}

	public List<DatabaseObject> getFlowVirtualChildren() {
		return FlowVirtualProjector.childrenOf(this);
	}

	public JSONObject getFlowInput() throws EngineException {
		try {
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

	public String getFlowSource() {
		loadFlowSourceFile();
		return flowSource == null || flowSource.isBlank() ? DEFAULT_FLOW_SOURCE : flowSource;
	}

	public void setFlowSource(String flowSource) {
		if (flowSource == null || flowSource.isBlank()) {
			flowSource = DEFAULT_FLOW_SOURCE;
		}
		if (!this.flowSource.equals(flowSource)) {
			this.flowSource = flowSource;
			flowSourceDirty = true;
			changed();
		}
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

	private File getFlowSourceFile() {
		var project = getProject();
		var name = getName();
		if (project == null || name == null || name.isBlank()) {
			return null;
		}
		return new File(new File(project.getDirFile(), "libs/flows"), name + ".flow.js");
	}

	private void loadFlowSourceFile() {
		if (flowSourceDirty) {
			return;
		}
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
			FileUtils.writeStringToFile(file, getFlowSource(), StandardCharsets.UTF_8);
			flowSourceDirty = false;
			flowSourceFileLastModified = file.lastModified();
			var legacyFile = new File(file.getParentFile(), getName() + ".flow.yaml");
			if (legacyFile != null && legacyFile.isFile()) {
				legacyFile.delete();
			}
		} catch (Exception e) {
			throw new EngineException("Unable to write Flow source file \"" + file.getAbsolutePath() + "\".", e);
		}
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
		element.setName(xmlName(name));
		element.setMinOccurs(0);

		applyElementSchema(schema, element, normalizeSchema(flowSchema));
		return element;
	}

	private void applyElementSchema(XmlSchema schema, XmlSchemaElement element, Object flowSchema) throws JSONException {
		var normalized = normalizeSchema(flowSchema);
		var type = schemaType(normalized);
		if ("object".equals(type)) {
			var complexType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
			var sequence = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSequence());
			complexType.setParticle(sequence);
			element.setType(complexType);
			addJsonMetadataAttributes(complexType.getAttributes(), "object", false);

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
			addJsonMetadataAttributes(complexType.getAttributes(), "array", true);

			var item = schemaElement(schema, "item", schemaItems(normalized));
			item.setMaxOccurs(Long.MAX_VALUE);
			sequence.getItems().add(item);
			return;
		}

		var complexType = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaComplexType(schema));
		var simpleContent = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSimpleContent());
		var extension = XmlSchemaUtils.makeDynamicReadOnly(this, new XmlSchemaSimpleContentExtension());
		extension.setBaseTypeName(schemaTypeName(type));
		addJsonMetadataAttributes(extension.getAttributes(), type, false);
		simpleContent.setContent(extension);
		complexType.setContentModel(simpleContent);
		element.setType(complexType);
	}

	private void addJsonMetadataAttributes(XmlSchemaObjectCollection attributes, String type, boolean array) {
		addOptionalAttribute(attributes, "type", type);
		addOptionalAttribute(attributes, "originalKeyName", null);
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
}
