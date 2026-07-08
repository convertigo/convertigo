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

package com.twinsoft.convertigo.beans.flow;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IDynamicPropertyContainer;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;
import com.twinsoft.convertigo.engine.util.XMLUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FlowVirtualObject extends DatabaseObject implements IDynamicPropertyContainer {

	private static final long serialVersionUID = -8182422318922188314L;

	private String virtualKind = "";
	private String virtualType = "";
	private String virtualPath = "";
	private String summary = "";
	private String definition = "";
	private String virtualInfo = "";
	private long virtualOrder = -1;
	private final List<DatabaseObject> children = new ArrayList<>();

	public FlowVirtualObject() {
		super();
		databaseType = "flowvirtualobject";
	}

	FlowVirtualObject(DatabaseObject parent, String name, String virtualKind, String virtualType, String virtualPath,
			String summary, String definition) {
		super();
		databaseType = "flowvirtualobject";
		this.virtualKind = valueOrEmpty(virtualKind);
		this.virtualType = valueOrEmpty(virtualType);
		this.virtualPath = valueOrEmpty(virtualPath);
		this.summary = valueOrEmpty(summary);
		this.definition = valueOrEmpty(definition);
		try {
			setName(safeName(name));
		} catch (EngineException e) {
			try {
				setName("flowVirtualObject");
			} catch (EngineException ignore) {
			}
		}
		this.parent = parent;
		this.priority = stablePriority(parent, this.virtualPath, getName());
	}

	void addVirtualChild(FlowVirtualObject child) {
		children.add(child);
	}

	@Override
	public void remove(DatabaseObject databaseObject) throws EngineException {
		children.remove(databaseObject);
		super.remove(databaseObject);
	}

	public String getVirtualKind() {
		return virtualKind;
	}

	public void setVirtualKind(String virtualKind) {
		this.virtualKind = valueOrEmpty(virtualKind);
	}

	public String getVirtualType() {
		return virtualType;
	}

	public void setVirtualType(String virtualType) {
		this.virtualType = valueOrEmpty(virtualType);
	}

	public String getVirtualPath() {
		return virtualPath;
	}

	public void setVirtualPath(String virtualPath) {
		this.virtualPath = valueOrEmpty(virtualPath);
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = valueOrEmpty(summary);
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) throws EngineException {
		definition = valueOrEmpty(definition);
		if (!this.definition.equals(definition)) {
			applyDefinitionMutation(definition);
			this.definition = definition;
		}
	}

	public String getVirtualInfo() {
		return virtualInfo;
	}

	public void setVirtualInfo(String virtualInfo) {
		this.virtualInfo = valueOrEmpty(virtualInfo);
	}

	public JSONObject getVirtualInfoObject() {
		var value = parseDefinitionValue(virtualInfo);
		return value instanceof JSONObject ? (JSONObject) value : null;
	}

	void setVirtualOrder(long virtualOrder) {
		this.virtualOrder = virtualOrder;
	}

	public void setDefinitionValue(Object value) throws EngineException {
		applyMutation(virtualPath, value);
		definition = definitionString(value);
	}

	public Object getDefinitionValue() {
		return parseDefinitionValue(definition);
	}

	public boolean isDefinitionWritable() {
		var target = mutableSourceRoot();
		if (isReadOnlyReference() && !isWritableSourceObject()) {
			return false;
		}
		return target != null && !virtualPath.isBlank()
				&& (isWritablePath(target, virtualPath) || isWritableSourceObject());
	}

	public JSONObject getDefinitionObject() {
		var value = getDefinitionValue();
		return value instanceof JSONObject ? (JSONObject) value : null;
	}

	public Object getDefinitionProperty(String key) {
		var object = getDefinitionObject();
		if (object == null) {
			return null;
		}
		if (object.has(key)) {
			return object.opt(key);
		}
		var props = object.optJSONObject("props");
		return props == null ? null : props.opt(key);
	}

	public void setDefinitionProperty(String key, Object value) throws EngineException {
		key = valueOrEmpty(key).trim();
		if (key.isBlank()) {
			throw new EngineException("Flow virtual property name must not be empty.");
		}
		if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
			throw new EngineException("Unsupported Flow virtual property name: " + key);
		}
		if (isReadOnlyProperty(propertyDefinition(key))) {
			throw new EngineException("Flow virtual property \"" + key + "\" is read-only.");
		}
		var propertyPath = virtualPath.isBlank() ? key : virtualPath + "." + key;
		applyPropertyMutation(key, propertyPath, value);
		try {
			var object = getDefinitionObject();
			if (object != null) {
				object.put(key, value == null ? JSONObject.NULL : value);
				var props = object.optJSONObject("props");
				if (props != null) {
					props.remove(key);
					if (props.length() == 0) {
						object.remove("props");
					}
				}
				definition = object.toString();
				refreshSummaryFromDefinition(object);
			}
		} catch (JSONException e) {
			throw new EngineException("Unable to update Flow virtual property cache.", e);
		}
	}

	@Override
	public String getComment() {
		if ("node".equals(virtualKind)) {
			var comment = getDefinitionProperty("comment");
			if (comment != null && !JSONObject.NULL.equals(comment)) {
				return String.valueOf(comment);
			}
		}
		return super.getComment();
	}

	@Override
	public void setComment(String comment) {
		if ("node".equals(virtualKind) && isDefinitionWritable()) {
			try {
				setDefinitionProperty("comment", valueOrEmpty(comment));
				return;
			} catch (EngineException e) {
				Engine.logBeans.warn("Unable to update Flow virtual object comment.", e);
			}
		}
		super.setComment(comment);
	}

	@Override
	public List<DatabaseObject> getDatabaseObjectChildren() {
		return new ArrayList<>(children);
	}

	@Override
	public List<DatabaseObject> getAllChildren() {
		return getDatabaseObjectChildren();
	}

	@Override
	public boolean hasDatabaseObjectChildren() {
		return !children.isEmpty();
	}

	@Override
	public void delete() throws EngineException {
		if (isSourceBackedDeletable()) {
			applySourceDeleteMutation((FlowEngine) mutableSourceRoot(), sourceValue("sourcePath"), sourceValue("sourceMutationPath"));
		} else if (isSourceBackedFileDeletable()) {
			deleteSourceFile((FlowEngine) mutableSourceRoot(), sourceValue("sourcePath"));
		} else if (isDefinitionWritable() && isDeletableVirtualKind()) {
			applyDeleteMutation(virtualPath);
		} else if (!virtualKind.isBlank()) {
			throw new EngineException("Cannot delete Flow virtual " + virtualKind + " object from the tree.");
		}
		super.delete();
	}

	private boolean isDeletableVirtualKind() {
		return switch (virtualKind) {
		case "node", "field", "binding" -> true;
		default -> false;
		};
	}

	@Override
	public boolean isHiddenProperty(String propertyName) {
		return switch (propertyName) {
		case "comment", "definition", "summary", "virtualInfo", "virtualKind", "virtualPath", "virtualType" -> true;
		default -> super.isHiddenProperty(propertyName);
		};
	}

	@Override
	public Object getOrderedValue() {
		return virtualOrder >= 0 ? Long.valueOf(virtualOrder) : super.getOrderedValue();
	}

	@Override
	public Element toXml(Document document) throws EngineException {
		var element = super.toXml(document);
		appendDynamicProperties(document, element);
		return element;
	}

	@Override
	public boolean setDynamicProperty(String name, String value) throws EngineException {
		if (!isDefinitionWritable()) {
			return false;
		}
		if ("comment".equals(name)) {
			setComment(value);
			return true;
		}
		if ("#flow_value".equals(name)) {
			setDefinitionValue(parseEditedValue(value, getDefinitionValue()));
			return true;
		}
		var definition = getDefinitionObject();
		if (definition != null && (definition.has(name) || hasDeclaredProperty(name))) {
			setDefinitionProperty(name, parseEditedValue(value, declaredOrCurrentValue(name)));
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return summary.isBlank() ? getName() : summary;
	}

	@Override
	protected String defaultBeanName(String displayName) {
		return "flowVirtualObject";
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}

	private static String safeName(String name) {
		var normalized = valueOrEmpty(name).trim().replaceAll("[^A-Za-z0-9_]", "_");
		normalized = normalized.replaceAll("_+", "_");
		if (normalized.isBlank()) {
			normalized = "item";
		}
		if (!Character.isLetter(normalized.charAt(0)) && normalized.charAt(0) != '_') {
			normalized = "_" + normalized;
		}
		return normalized;
	}

	private static long stablePriority(DatabaseObject parent, String virtualPath, String name) {
		var seed = (parent == null ? "" : parent.getQName()) + "|" + valueOrEmpty(virtualPath) + "|" + valueOrEmpty(name);
		var hash = 1125899906842597L;
		for (var i = 0; i < seed.length(); i++) {
			hash = 31 * hash + seed.charAt(i);
		}
		return Math.abs(hash == Long.MIN_VALUE ? 0 : hash);
	}

	private static boolean isInternalDefinitionProperty(String key) {
		return "id".equals(key) || "block".equals(key) || "props".equals(key);
	}

	private void appendDynamicProperties(Document document, Element root) throws EngineException {
		if (!exportOptions.contains(ExportOption.bIncludeDisplayName)) {
			return;
		}

		var value = getDefinitionValue();
		try {
			if (value instanceof JSONObject json) {
				var info = getVirtualInfoObject();
				var propertyDefinitions = info == null ? null : info.optJSONObject("propertyDefinitions");
				if ("node".equals(virtualKind)) {
					appendDynamicProperty(document, root, "comment", "Comment", "Base properties", getComment(), "Flow node comment.", false);
				}
				if (propertyDefinitions != null) {
					for (var key : propertyDefinitionKeys(info, propertyDefinitions)) {
						var definition = propertyDefinitions.optJSONObject(key);
						if (!isHiddenProperty(definition)) {
							appendDynamicProperty(document, root, key, propertyLabel(key, definition),
									propertyCategory(definition),
									declaredOrCurrentValue(key),
									propertyDescription(key, definition),
									isReadOnlyProperty(definition));
						}
					}
				}
				for (var key : sortedKeys(json)) {
					if (!"comment".equals(key) && !isInternalDefinitionProperty(key)) {
						if (propertyDefinitions == null || !propertyDefinitions.has(key)) {
							appendDynamicProperty(document, root, key, key, "Expert", json.opt(key), "Flow property \"" + key + "\".", false);
						}
					}
				}
			} else if (isEditableScalarKind() && !(value instanceof JSONObject) && !(value instanceof JSONArray)) {
				appendDynamicProperty(document, root, "#flow_value", "Value", "Base properties", value, "Flow value.", false);
			}
		} catch (Exception e) {
			throw new EngineException("Unable to append Flow virtual properties.", e);
		}
	}

	private boolean isEditableScalarKind() {
		return "field".equals(virtualKind) || "binding".equals(virtualKind);
	}

	private void appendDynamicProperty(Document document, Element root, String name, String displayName, String category,
			Object value, String description, boolean readOnly) throws Exception {
		var property = document.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("displayName", displayName);
		property.setAttribute("isHidden", "false");
		property.setAttribute("isMasked", "false");
		property.setAttribute("isExpert", "false");
		property.setAttribute("isDisabled", readOnly || !isDefinitionWritable() ? "true" : "false");
		property.setAttribute("category", category == null || category.isBlank() ? "Base properties" : category);
		property.setAttribute("shortDescription", description);
		property.setAttribute("editorClass", "null");
		if (isMultilineValue(value)) {
			property.setAttribute("isMultiline", "true");
		}
		property.appendChild(XMLUtils.writeObjectToXml(document, dynamicPropertyValue(value)));
		root.appendChild(property);
	}

	private static List<String> sortedKeys(JSONObject object) {
		var keys = new ArrayList<String>();
		for (var it = object.keys(); it.hasNext();) {
			keys.add(String.valueOf(it.next()));
		}
		Collections.sort(keys);
		return keys;
	}

	private static List<String> propertyDefinitionKeys(JSONObject info, JSONObject definitions) {
		var keys = new ArrayList<String>();
		var order = info == null ? null : info.optJSONArray("propertyOrder");
		if (order != null) {
			for (var i = 0; i < order.length(); i++) {
				var key = order.optString(i, "");
				if (!key.isBlank() && definitions.has(key) && !keys.contains(key)) {
					keys.add(key);
				}
			}
		}
		for (var key : sortedKeys(definitions)) {
			if (!keys.contains(key)) {
				keys.add(key);
			}
		}
		return keys;
	}

	private static boolean isHiddenProperty(JSONObject definition) {
		return definition != null && definition.optBoolean("hidden", false);
	}

	private static boolean isReadOnlyProperty(JSONObject definition) {
		return definition != null && definition.optBoolean("readOnly", false);
	}

	private static String propertyLabel(String key, JSONObject definition) {
		if (definition == null) {
			return key;
		}
		var label = definition.optString("label", "");
		return label.isBlank() ? key : label;
	}

	private static String propertyDescription(String key, JSONObject definition) {
		if (definition == null) {
			return "Flow property \"" + key + "\".";
		}
		var description = definition.optString("shortDescription", definition.optString("description", ""));
		if (description.isBlank()) {
			description = definition.optString("longDescription", "");
		}
		return description.isBlank() ? "Flow property \"" + key + "\"." : description;
	}

	private static String propertyCategory(JSONObject definition) {
		if (definition == null) {
			return "Expert";
		}
		var category = definition.optString("category", "");
		if (!category.isBlank()) {
			return category;
		}
		return definition.optBoolean("expert", false) || definition.optBoolean("advanced", false)
				? "Expert"
				: "Base properties";
	}

	private static boolean isMultilineValue(Object value) {
		return value instanceof JSONObject || value instanceof JSONArray
				|| value instanceof String text && text.contains("\n");
	}

	private static Object dynamicPropertyValue(Object value) {
		if (value == null || JSONObject.NULL.equals(value)) {
			return "";
		}
		return value instanceof JSONObject || value instanceof JSONArray ? value.toString() : value;
	}

	private static Object parseEditedValue(String text, Object currentValue) {
		var trimmed = text == null ? "" : text.trim();
		if (currentValue instanceof Boolean) {
			return Boolean.valueOf(trimmed);
		}
		if (currentValue instanceof Number) {
			return parseNumber(trimmed, currentValue);
		}
		if (currentValue instanceof JSONObject || currentValue instanceof JSONArray
				|| trimmed.startsWith("{") || trimmed.startsWith("[") || "null".equals(trimmed)) {
			try {
				return new JSONTokener(trimmed).nextValue();
			} catch (Exception e) {
				return text;
			}
		}
		return text;
	}

	private static Object parseNumber(String text, Object currentValue) {
		try {
			if (currentValue instanceof Integer) {
				return Integer.valueOf(text);
			}
			if (currentValue instanceof Long) {
				return Long.valueOf(text);
			}
			if (currentValue instanceof Float) {
				return Float.valueOf(text);
			}
			if (currentValue instanceof Double) {
				return Double.valueOf(text);
			}
			return text.contains(".") || text.contains("e") || text.contains("E")
					? Double.valueOf(text)
					: Long.valueOf(text);
		} catch (NumberFormatException e) {
			return text;
		}
	}

	private void applyDefinitionMutation(String newDefinition) throws EngineException {
		applyMutation(virtualPath, parseDefinitionValue(newDefinition));
	}

	private void applyMutation(String path, Object value) throws EngineException {
		var target = mutableSourceRoot();
		if (target == null || path.isBlank()) {
			return;
		}
		if (!isWritablePath(target, path)) {
			throw new EngineException("Flow virtual path \"" + path + "\" is read-only.");
		}
		try {
			var mutation = new JSONObject()
					.put("op", "replace")
					.put("path", path)
					.put("value", value == null ? JSONObject.NULL : value);
			var response = target instanceof Flow flow
					? new FlowEngineBridge().applyMutation(flow, mutation)
					: new FlowEngineBridge().applyMutation((FlowEngine) target, mutation);
			if (!response.optBoolean("ok", false)) {
				var error = response.optJSONObject("error");
				var message = error == null ? response.optString("message", "Flow mutation failed.") : flowErrorMessage(error);
				throw new EngineException("Flow virtual mutation failed: " + message);
			}
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow virtual mutation.", e);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to apply Flow virtual mutation.", e);
		}
	}

	private void applyDeleteMutation(String path) throws EngineException {
		var target = mutableSourceRoot();
		if (target == null || path.isBlank()) {
			return;
		}
		if (!isWritablePath(target, path)) {
			throw new EngineException("Flow virtual path \"" + path + "\" is read-only.");
		}
		try {
			var mutation = new JSONObject()
					.put("op", "delete")
					.put("path", path);
			var response = target instanceof Flow flow
					? new FlowEngineBridge().applyMutation(flow, mutation)
					: new FlowEngineBridge().applyMutation((FlowEngine) target, mutation);
			if (!response.optBoolean("ok", false)) {
				var error = response.optJSONObject("error");
				var message = error == null ? response.optString("message", "Flow delete failed.") : flowErrorMessage(error);
				throw new EngineException("Flow virtual delete failed: " + message);
			}
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow virtual delete mutation.", e);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to delete Flow virtual object.", e);
		}
	}

	private void applyPropertyMutation(String key, String path, Object value) throws EngineException {
		var target = mutableSourceRoot();
		if (target instanceof FlowEngine flowEngine && "block".equals(virtualKind) && isWritableSourceObject()) {
			new FlowEngineBridge().setBlockProperty(flowEngine, virtualType, key, value);
			FlowStudioSupport.clearCatalogCache(flowEngine);
			return;
		}
		if (target instanceof FlowEngine flowEngine && "type".equals(virtualKind) && isWritableSourceObject()) {
			new FlowEngineBridge().setTypeProperty(flowEngine, virtualType, key, value);
			FlowStudioSupport.clearCatalogCache(flowEngine);
			return;
		}
		if (target instanceof FlowEngine flowEngine && "typeResource".equals(virtualKind) && isWritableSourceObject()) {
			var object = getDefinitionObject();
			var typeName = object == null ? "" : object.optString("type", "");
			var role = object == null ? "" : object.optString("role", virtualType);
			new FlowEngineBridge().setTypeResourceProperty(flowEngine, typeName, role, key, value);
			FlowStudioSupport.clearCatalogCache(flowEngine);
			return;
		}
		if (target instanceof FlowEngine flowEngine && isWritableSourceObject()) {
			var sourcePath = sourceValue("sourcePath");
			var sourceMutationPath = sourceValue("sourceMutationPath");
			if (!sourcePath.isBlank() && !sourceMutationPath.isBlank()) {
				var propertyPath = sourcePropertyMutationPath(key);
				applySourcePropertyMutation(flowEngine, sourcePath,
						propertyPath.isBlank() ? sourceMutationPath + "." + key : propertyPath, value);
				return;
			}
		}
		applyMutation(path, value);
	}

	private void applySourcePropertyMutation(FlowEngine flowEngine, String sourcePath, String path, Object value) throws EngineException {
		try {
			var response = new FlowEngineBridge().applySourceMutation(flowEngine, sourcePath, new JSONObject()
					.put("op", "replace")
					.put("path", path)
					.put("value", value == null ? JSONObject.NULL : value));
			if (!response.optBoolean("ok", false)) {
				var error = response.optJSONObject("error");
				var message = error == null ? response.optString("message", "Flow source mutation failed.") : flowErrorMessage(error);
				throw new EngineException("Flow source mutation failed: " + message);
			}
			FlowStudioSupport.afterSourceMutation(flowEngine, sourcePath);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow source property mutation.", e);
		}
	}

	private void applySourceDeleteMutation(FlowEngine flowEngine, String sourcePath, String path) throws EngineException {
		try {
			var response = new FlowEngineBridge().applySourceMutation(flowEngine, sourcePath, new JSONObject()
					.put("op", "delete")
					.put("path", path));
			if (!response.optBoolean("ok", false)) {
				var error = response.optJSONObject("error");
				var message = error == null ? response.optString("message", "Flow source delete failed.") : flowErrorMessage(error);
				throw new EngineException("Flow source delete failed: " + message);
			}
			FlowStudioSupport.afterSourceMutation(flowEngine, sourcePath);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow source delete mutation.", e);
		}
	}

	private boolean isSourceBackedDeletable() {
		var target = mutableSourceRoot();
		var sourcePath = sourceValue("sourcePath");
		var sourceMutationPath = sourceValue("sourceMutationPath");
		return target instanceof FlowEngine
				&& isWritableSourceObject()
				&& (sourcePath.endsWith(".front.json") || sourcePath.endsWith(".flow.svelte"))
				&& !sourceMutationPath.isBlank();
	}

	private boolean isSourceBackedFileDeletable() {
		var target = mutableSourceRoot();
		var sourcePath = sourceValue("sourcePath");
		return target instanceof FlowEngine
				&& "frontendBlock".equals(virtualKind)
				&& isWritableSourceObject()
				&& isFrontendSourceFile(sourcePath);
	}

	private static boolean isFrontendSourceFile(String sourcePath) {
		return sourcePath.endsWith(".flow.svelte")
				|| sourcePath.endsWith(".svelte")
				|| sourcePath.endsWith(".svelte.js")
				|| sourcePath.endsWith(".svelte.ts")
				|| sourcePath.endsWith(".uiblock.json");
	}

	private void deleteSourceFile(FlowEngine flowEngine, String sourcePath) throws EngineException {
		try {
			var file = new File(sourcePath);
			if (!file.isFile()) {
				throw new EngineException("Flow virtual source file does not exist: " + sourcePath);
			}
			FileUtils.forceDelete(file);
			FlowEngineBridge.clearCaches();
			FlowStudioSupport.clearCatalogCache(flowEngine);
			FlowStudioSupport.afterSourceMutation(flowEngine, sourcePath);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to delete Flow virtual source file \"" + sourcePath + "\".", e);
		}
	}

	private boolean isWritableSourceObject() {
		return jsonFlag(getDefinitionObject(), "sourceWritable") || jsonFlag(getVirtualInfoObject(), "sourceWritable");
	}

	private boolean isReadOnlyReference() {
		return jsonFlag(getDefinitionObject(), "readOnlyReference") || jsonFlag(getVirtualInfoObject(), "readOnlyReference");
	}

	private String sourceValue(String key) {
		var info = getVirtualInfoObject();
		var value = info == null ? "" : info.optString(key, "");
		if (!value.isBlank()) {
			return value;
		}
		var definition = getDefinitionObject();
		return definition == null ? "" : definition.optString(key, "");
	}

	private String sourcePropertyMutationPath(String key) {
		var value = sourcePropertyMutationPath(getVirtualInfoObject(), key);
		if (!value.isBlank()) {
			return value;
		}
		return sourcePropertyMutationPath(getDefinitionObject(), key);
	}

	private static String sourcePropertyMutationPath(JSONObject object, String key) {
		var paths = object == null ? null : object.optJSONObject("sourcePropertyMutationPaths");
		return paths == null ? "" : paths.optString(key, "");
	}

	private static String flowErrorMessage(JSONObject error) {
		var message = error.optString("message", "");
		return message.isBlank() ? error.optString("code", "Flow error.") : message;
	}

	private static boolean jsonFlag(JSONObject object, String key) {
		return object != null && object.optBoolean(key, false);
	}

	private void refreshSummaryFromDefinition(JSONObject object) {
		if (object == null) {
			return;
		}
		var next = firstNonBlank(
				object.optString("label", ""),
				object.optString("title", ""),
				object.optString("text", ""),
				object.optString("name", ""),
				object.optString("id", ""),
				object.optString("kind", ""));
		if (!next.isBlank()) {
			summary = next;
		}
	}

	private static String firstNonBlank(String... values) {
		if (values != null) {
			for (var value : values) {
				value = valueOrEmpty(value);
				if (!value.isBlank()) {
					return value;
				}
			}
		}
		return "";
	}

	private boolean hasDeclaredProperty(String key) {
		var definition = propertyDefinition(key);
		return definition != null && !isHiddenProperty(definition);
	}

	private Object declaredOrCurrentValue(String key) {
		var value = getDefinitionProperty(key);
		if (value != null) {
			return value;
		}
		return declaredDefaultValue(key);
	}

	private Object declaredDefaultValue(String key) {
		var definition = propertyDefinition(key);
		if (definition != null && definition.has("default")) {
			return definition.opt("default");
		}
		var info = getVirtualInfoObject();
		var defaults = info == null ? null : info.optJSONObject("propertyDefaults");
		return defaults == null ? "" : defaults.opt(key);
	}

	private JSONObject propertyDefinition(String key) {
		var info = getVirtualInfoObject();
		var definitions = info == null ? null : info.optJSONObject("propertyDefinitions");
		return definitions == null ? null : definitions.optJSONObject(key);
	}

	private DatabaseObject mutableSourceRoot() {
		var current = (DatabaseObject) this;
		while (current != null) {
			if (current instanceof Flow || current instanceof FlowEngine) {
				return current;
			}
			try {
				current = current.getParent();
			} catch (Exception e) {
				Engine.logBeans.warn("Unable to resolve Flow virtual object parent.", e);
				return null;
			}
		}
		return null;
	}

	private static boolean isWritablePath(DatabaseObject target, String virtualPath) {
		if (target instanceof Flow) {
			return !virtualPath.startsWith("catalog");
		}
		if (target instanceof FlowEngine) {
			return virtualPath.equals("bindings")
					|| virtualPath.startsWith("bindings.")
					|| virtualPath.equals("config")
					|| virtualPath.startsWith("config.");
		}
		return false;
	}

	public static Object parseDefinitionValue(String value) {
		var text = valueOrEmpty(value).trim();
		if (text.isBlank()) {
			return "";
		}
		try {
			return new JSONTokener(text).nextValue();
		} catch (Exception e) {
			return value;
		}
	}

	private static String definitionString(Object value) {
		if (value == null || JSONObject.NULL.equals(value)) {
			return "null";
		}
		return value instanceof String text ? JSONObject.quote(text) : String.valueOf(value);
	}
}
