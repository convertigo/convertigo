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
	private transient String parsedDefinitionSource;
	private transient Object parsedDefinitionValue;
	private transient String parsedVirtualInfoSource;
	private transient Object parsedVirtualInfoValue;
	private transient JSONObject lastSourceMutationResult;
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
			if (!applyMutation(virtualPath, parseDefinitionValue(definition))) {
				this.definition = definition;
				parsedDefinitionSource = null;
				parsedDefinitionValue = null;
			}
		}
	}

	public String getVirtualInfo() {
		return virtualInfo;
	}

	public void setVirtualInfo(String virtualInfo) {
		this.virtualInfo = valueOrEmpty(virtualInfo);
		parsedVirtualInfoSource = null;
		parsedVirtualInfoValue = null;
	}

	public JSONObject getVirtualInfoObject() {
		var value = parsedVirtualInfoValue();
		return value instanceof JSONObject ? (JSONObject) value : null;
	}

	public String getSourcePath() {
		return sourceValue("sourcePath");
	}

	public String getSourceMutationPath() {
		return sourceValue("sourceMutationPath");
	}

	public JSONObject consumeLastSourceMutationResult() {
		var result = lastSourceMutationResult;
		lastSourceMutationResult = null;
		return result;
	}

	public boolean replaceProjectedTree(JSONObject source) {
		var projected = FlowVirtualProjector.projectedObject(getParent(), source, (int) virtualOrder);
		if (projected == null || !virtualPath.equals(projected.virtualPath)) {
			return false;
		}
		try {
			setName(projected.getName());
		} catch (EngineException e) {
			return false;
		}
		virtualKind = projected.virtualKind;
		virtualType = projected.virtualType;
		summary = projected.summary;
		definition = projected.definition;
		virtualInfo = projected.virtualInfo;
		priority = projected.priority;
		parsedDefinitionSource = null;
		parsedDefinitionValue = null;
		parsedVirtualInfoSource = null;
		parsedVirtualInfoValue = null;
		// Editors and mutation listeners can still hold the previous projection
		// until the UI reconciles it. Keep its owner chain usable during that
		// handoff; only this children list defines the current projection.
		children.clear();
		for (var child : projected.children) {
			child.setParent(this);
			children.add(child);
		}
		return true;
	}

	void setVirtualOrder(long virtualOrder) {
		this.virtualOrder = virtualOrder;
	}

	public void setDefinitionValue(Object value) throws EngineException {
		if (!applyMutation(virtualPath, value)) {
			definition = definitionString(value);
			parsedDefinitionSource = null;
			parsedDefinitionValue = null;
		}
	}

	public Object getDefinitionValue() {
		if (!definition.equals(parsedDefinitionSource)) {
			parsedDefinitionValue = parseDefinitionValue(definition);
			parsedDefinitionSource = definition;
		}
		return parsedDefinitionValue;
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
		var path = declaredDefinitionPath(key);
		if (!path.isBlank()) {
			Object value = object;
			for (var part : path.split("\\.")) {
				value = value instanceof JSONObject json ? json.opt(part) : null;
			}
			return value;
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
		if (!key.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
			throw new EngineException("Unsupported Flow virtual property name: " + key);
		}
		if (isReadOnlyProperty(propertyDefinition(key))) {
			throw new EngineException("Flow virtual property \"" + key + "\" is read-only.");
		}
		var relativePath = writableDefinitionPath(key);
		var propertyPath = virtualPath.isBlank() ? relativePath : virtualPath + "." + relativePath;
		try {
			var object = getDefinitionObject();
			if (object != null) {
				// Prepare a separate cache value before the mutation; a failed write
				// must not change the projected object or flatten its payload.
				object = new JSONObject(object.toString());
				var parts = relativePath.split("\\.");
				var parent = object;
				for (var i = 0; i < parts.length - 1; i++) {
					var child = parent.optJSONObject(parts[i]);
					if (child == null) {
						if (parent.has(parts[i]) && !parent.isNull(parts[i])) {
							throw new EngineException("Flow property path crosses a non-object: " + relativePath);
						}
						child = new JSONObject();
						parent.put(parts[i], child);
					}
					parent = child;
				}
				parent.put(parts[parts.length - 1], value == null ? JSONObject.NULL : value);
				if (!applyPropertyMutation(key, propertyPath, value)) {
					definition = object.toString();
					parsedDefinitionSource = definition;
					parsedDefinitionValue = object;
				}
			} else {
				applyPropertyMutation(key, propertyPath, value);
			}
		} catch (JSONException e) {
			throw new EngineException("Unable to update Flow virtual property cache.", e);
		}
	}

	private String writableDefinitionPath(String key) {
		var path = declaredDefinitionPath(key);
		if (!path.isBlank()) {
			return path;
		}
		var object = getDefinitionObject();
		var props = object == null ? null : object.optJSONObject("props");
		return object != null && !object.has(key) && props != null && props.has(key) ? "props." + key : key;
	}

	private String declaredDefinitionPath(String key) {
		var descriptor = propertyDefinition(key);
		var path = descriptor == null ? "" : descriptor.optString("definitionPath", "");
		if (!path.isBlank() && !path.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
			throw new IllegalArgumentException("Invalid projected Flow property path: " + path);
		}
		return path;
	}

	@Override
	public String getComment() {
		if ("node".equals(virtualKind)) {
			var object = getDefinitionObject();
			var comment = object == null ? null : object.opt("comment");
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
				setDefinitionProperty(hasDeclaredProperty("$$comment") ? "$$comment" : "comment", valueOrEmpty(comment));
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
		} else if (isDefinitionDeletable()) {
			applyDeleteMutation(virtualPath);
		} else if (!virtualKind.isBlank()) {
			throw new EngineException("Cannot delete Flow virtual " + virtualKind + " object from the tree.");
		}
		super.delete();
	}

	public boolean isDefinitionDeletable() {
		var info = getVirtualInfoObject();
		return isDefinitionWritable() && info != null && info.optBoolean("deletable", false);
	}

	public boolean isDeletable() {
		return isSourceBackedDeletable() || isSourceBackedFileDeletable() || isDefinitionDeletable();
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
		// Eclipse qualifies virtual properties with their synthetic descriptor name.
		// Keep the Flow contract stable: callers of this object deal in the actual
		// virtual property name, not in the Eclipse implementation prefix.
		name = unqualifiedVirtualPropertyName(name);
		if (!isDefinitionWritable()) {
			return false;
		}
		var descriptor = propertyDefinition(name);
		if (isReadOnlyProperty(descriptor) || isHiddenProperty(descriptor)) {
			throw new EngineException("This projected property is not editable: " + name);
		}
		if ("comment".equals(name) && declaredDefinitionPath(name).isBlank()) {
			setComment(value);
			return true;
		}
		if ("#flow_value".equals(name)) {
			setDefinitionValue(convertEditedValue(name, value));
			return true;
		}
		var definition = getDefinitionObject();
		if (definition != null && hasDeclaredProperty(name)) {
			var edited = convertEditedValue(name, value);
			setDefinitionProperty(name, invertedValue(propertyDefinition(name), edited));
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

	private static String unqualifiedVirtualPropertyName(String name) {
		var value = valueOrEmpty(name);
		var prefix = "#flow_property:";
		return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
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

	private void appendDynamicProperties(Document document, Element root) throws EngineException {
		if (!exportOptions.contains(ExportOption.bIncludeDisplayName)) {
			return;
		}

		var value = getDefinitionValue();
		try {
			if (value instanceof JSONObject json) {
				var info = getVirtualInfoObject();
				var propertyDefinitions = info == null ? null : info.optJSONObject("propertyDefinitions");
				var engineProjectsComment = propertyDefinitions != null && propertyDefinitions.has("$$comment");
				if ("node".equals(virtualKind) && !hasDeclaredProperty("$$comment") && !engineProjectsComment) {
					appendDynamicProperty(document, root, "comment", "Comment", "Base properties", getComment(), "Flow node comment.", false, null);
				}
				if (propertyDefinitions != null) {
					for (var key : propertyDefinitionKeys(info, propertyDefinitions)) {
						var definition = propertyDefinitions.optJSONObject(key);
						if (!isHiddenProperty(definition)) {
							appendDynamicProperty(document, root, key, propertyLabel(key, definition),
									propertyCategory(definition),
									projectedPropertyValue(key, definition),
									propertyDescription(key, definition),
									isReadOnlyProperty(definition), definition);
						}
					}
				}
			} else if ((isEditableScalarKind() && !(value instanceof JSONObject) && !(value instanceof JSONArray))
					|| ("scope".equals(virtualKind) && "config".equals(virtualType) && value instanceof JSONObject)) {
				var definition = propertyDefinition("#flow_value");
				appendDynamicProperty(document, root, "#flow_value", propertyLabel("#flow_value", definition),
						propertyCategory(definition), value, propertyDescription("#flow_value", definition), false, definition);
			}
		} catch (Exception e) {
			throw new EngineException("Unable to append Flow virtual properties.", e);
		}
	}

	private boolean isEditableScalarKind() {
		return "field".equals(virtualKind) || "binding".equals(virtualKind);
	}

	private void appendDynamicProperty(Document document, Element root, String name, String displayName, String category,
			Object value, String description, boolean readOnly, JSONObject definition) throws Exception {
		var property = document.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("displayName", displayName);
		property.setAttribute("isHidden", "false");
		property.setAttribute("isMasked", "false");
		property.setAttribute("isExpert", "false");
		property.setAttribute("isDisabled", readOnly || !isDefinitionWritable() ? "true" : "false");
		property.setAttribute("category", category == null || category.isBlank() ? "Base properties" : category);
		property.setAttribute("shortDescription", description);
		var editorClass = definition == null ? "" : definition.optString("editorClass", "");
		property.setAttribute("editorClass", editorClass.isBlank() ? "null" : editorClass);
		if (definition != null) {
			var editorResource = definition.optString("editorResource", "");
			if (!editorResource.isBlank()) {
				property.setAttribute("editorResource", editorResource);
			}
			var kind = definition.optString("kind", "");
			if (!kind.isBlank()) {
				property.setAttribute("flowKind", kind);
			}
			var type = definition.optString("type", "");
			if (!type.isBlank()) {
				property.setAttribute("flowType", type);
			}
		}
		if (isMultilineValue(value)) {
			property.setAttribute("isMultiline", "true");
		}
		property.appendChild(XMLUtils.writeObjectToXml(document, dynamicPropertyValue(value)));
		appendPossibleValues(document, property, definition);
		root.appendChild(property);
	}

	private static void appendPossibleValues(Document document, Element property, JSONObject definition) {
		var values = definition == null ? null : definition.optJSONArray("enum");
		if (values == null || values.length() == 0) {
			return;
		}
		var possibleValues = document.createElement("possibleValues");
		for (var i = 0; i < values.length(); i++) {
			var value = values.opt(i);
			if (value == null || JSONObject.NULL.equals(value)) {
				continue;
			}
			var possibleValue = document.createElement("value");
			possibleValue.setTextContent(String.valueOf(value));
			possibleValues.appendChild(possibleValue);
		}
		if (possibleValues.hasChildNodes()) {
			property.appendChild(possibleValues);
		}
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

	protected Object convertEditedValue(String name, String text) throws EngineException {
		var owner = mutableSourceRoot();
		if (owner == null) {
			throw new EngineException("A Flow property edit requires its owning provider.");
		}
		return new FlowEngineBridge().propertyValue(owner, propertyDefinition(name), text);
	}

	private boolean applyMutation(String path, Object value) throws EngineException {
		var target = mutableSourceRoot();
		if (target == null || path.isBlank()) {
			return false;
		}
		if (!isWritablePath(target, path)) {
			throw new EngineException("Flow virtual path \"" + path + "\" is read-only.");
		}
		try {
			var info = getVirtualInfoObject();
			var mutation = new JSONObject()
					.put("op", info == null ? "replace" : info.optString("sourceMutationOp", "replace"))
					.put("path", path)
					.put("value", value == null ? JSONObject.NULL : value);
			var response = FlowStudioSupport.applyProjectedMutation(target, this, mutation);
			if (!response.optBoolean("ok", false)) {
				var error = response.optJSONObject("error");
				var message = error == null ? response.optString("message", "Flow mutation failed.") : flowErrorMessage(error);
				throw new EngineException("Flow virtual mutation failed: " + message);
			}
			lastSourceMutationResult = response;
			return FlowStudioSupport.refreshVirtualObjectFromTree(this, response);
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
			var response = FlowStudioSupport.applyOwnerMutation(target, mutation, false, "");
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

	private boolean applyPropertyMutation(String key, String path, Object value) throws EngineException {
		var target = mutableSourceRoot();
		if (target instanceof FlowEngine flowEngine && "block".equals(virtualKind) && isWritableSourceObject()) {
			new FlowEngineBridge().setBlockProperty(flowEngine, virtualType, key, value);
			FlowStudioSupport.clearCatalogCache(flowEngine);
			return false;
		}
		if (target instanceof FlowEngine flowEngine && "type".equals(virtualKind) && isWritableSourceObject()) {
			new FlowEngineBridge().setTypeProperty(flowEngine, virtualType, key, value);
			FlowStudioSupport.clearCatalogCache(flowEngine);
			return false;
		}
		if (target instanceof FlowEngine flowEngine && "typeResource".equals(virtualKind) && isWritableSourceObject()) {
			var object = getDefinitionObject();
			var typeName = object == null ? "" : object.optString("type", "");
			var role = object == null ? "" : object.optString("role", virtualType);
			new FlowEngineBridge().setTypeResourceProperty(flowEngine, typeName, role, key, value);
			FlowStudioSupport.clearCatalogCache(flowEngine);
			return false;
		}
		if (target instanceof FlowEngine flowEngine && isWritableSourceObject()) {
			var sourcePath = sourceValue("sourcePath");
			var sourceMutationPath = sourceValue("sourceMutationPath");
			if (!sourcePath.isBlank() && !sourceMutationPath.isBlank()) {
				var propertyPath = sourcePropertyMutationPath(key);
				return applySourcePropertyMutation(flowEngine, sourcePath,
						propertyPath.isBlank() ? sourceMutationPath + "." + writableDefinitionPath(key) : propertyPath, value);
			}
		}
		return applyMutation(path, value);
	}

	private boolean applySourcePropertyMutation(FlowEngine flowEngine, String sourcePath, String path, Object value) throws EngineException {
		try {
			var response = FlowStudioSupport.applyProjectedMutation(flowEngine, this, new JSONObject()
					.put("__sourcePath", sourcePath)
					.put("op", "replace")
					.put("path", path)
					.put("value", value == null ? JSONObject.NULL : value));
			if (!response.optBoolean("ok", false)) {
				var error = response.optJSONObject("error");
				var message = error == null ? response.optString("message", "Flow source mutation failed.") : flowErrorMessage(error);
				throw new EngineException("Flow source mutation failed: " + message);
			}
			lastSourceMutationResult = response;
			return FlowStudioSupport.refreshVirtualObjectFromTree(this, response);
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow source property mutation.", e);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to apply Flow source property mutation.", e);
		}
	}

	private void applySourceDeleteMutation(FlowEngine flowEngine, String sourcePath, String path) throws EngineException {
		try {
			var response = FlowStudioSupport.applyProjectedSourceMutation(flowEngine, this, sourcePath,
					new JSONObject()
							.put("op", "delete")
							.put("path", path));
			if (!response.optBoolean("ok", false)) {
				var error = response.optJSONObject("error");
				var message = error == null ? response.optString("message", "Flow source delete failed.") : flowErrorMessage(error);
				throw new EngineException("Flow source delete failed: " + message);
			}
			lastSourceMutationResult = response;
		} catch (JSONException e) {
			throw new EngineException("Unable to build Flow source delete mutation.", e);
		} catch (EngineException e) {
			throw e;
		} catch (Exception e) {
			throw new EngineException("Unable to delete Flow source object.", e);
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
				|| sourcePath.endsWith(".flow.css")
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
			FlowEngineBridge.invalidateDataCaches();
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

	private boolean hasDeclaredProperty(String key) {
		var definition = propertyDefinition(key);
		return definition != null && !isHiddenProperty(definition);
	}

	private Object projectedPropertyValue(String key, JSONObject definition) {
		return invertedValue(definition, declaredOrCurrentValue(key));
	}

	// Value of a projected property as the user must see it: declared default when the
	// source omits it, inverted when the descriptor says so. Studio surfaces read this
	// instead of the raw definition so that one bridge owns the semantics.
	public Object getProjectedPropertyValue(String key) {
		return projectedPropertyValue(key, propertyDefinition(key));
	}

	// True when a raw definition key is already represented by a projected property
	// (through its definitionPath); such keys must not be listed again as raw rows.
	public boolean isProjectedDefinitionKey(String rawKey) {
		var info = getVirtualInfoObject();
		var definitions = info == null ? null : info.optJSONObject("propertyDefinitions");
		if (definitions == null || rawKey == null || rawKey.isBlank()) {
			return false;
		}
		for (var keys = definitions.keys(); keys.hasNext();) {
			var definition = definitions.optJSONObject(String.valueOf(keys.next()));
			var path = definition == null ? "" : definition.optString("definitionPath", "");
			if (path.equals(rawKey) || path.startsWith(rawKey + ".")) {
				return true;
			}
		}
		return false;
	}

	/** Provider-defined information takes precedence over technical host information. */
	public boolean hasProjectedInformationProperty(String label) {
		var info = getVirtualInfoObject();
		var definitions = info == null ? null : info.optJSONObject("propertyDefinitions");
		if (definitions == null) {
			return false;
		}
		for (var keys = definitions.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			var definition = definitions.optJSONObject(key);
			if (definition != null && !definition.optBoolean("hidden", false)
					&& "Information".equals(definition.optString("category"))
					&& label.equals(propertyLabel(key, definition))) {
				return true;
			}
		}
		return false;
	}

	// Descriptor flag "invert": the surface shows the negation of the stored boolean
	// (for example "Is active" over a stored "disabled"). Symmetric on read and write.
	private static Object invertedValue(JSONObject definition, Object value) {
		if (definition == null || !definition.optBoolean("invert", false)) {
			return value;
		}
		if (value instanceof Boolean flag) {
			return !flag;
		}
		if (value == null || "".equals(value) || JSONObject.NULL.equals(value)) {
			return Boolean.TRUE;
		}
		return !Boolean.parseBoolean(String.valueOf(value));
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
		if (defaults != null && defaults.has(key)) {
			return defaults.opt(key);
		}
		// Information rows (block provider, block source...) carry their value in the
		// projection info, not in the node definition.
		return info == null || !info.has(key) ? "" : info.opt(key);
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

	private Object parsedVirtualInfoValue() {
		if (!virtualInfo.equals(parsedVirtualInfoSource)) {
			parsedVirtualInfoValue = parseDefinitionValue(virtualInfo);
			parsedVirtualInfoSource = virtualInfo;
		}
		return parsedVirtualInfoValue;
	}

	private static String definitionString(Object value) {
		if (value == null || JSONObject.NULL.equals(value)) {
			return "null";
		}
		return value instanceof String text ? JSONObject.quote(text) : String.valueOf(value);
	}
}
