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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IDynamicPropertyContainer;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.util.XMLUtils;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
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
		Object value = parseDefinitionValue(virtualInfo);
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
		DatabaseObject target = mutableSourceRoot();
		return target != null && !virtualPath.isBlank() && isWritablePath(target, virtualPath);
	}

	public JSONObject getDefinitionObject() {
		Object value = getDefinitionValue();
		return value instanceof JSONObject ? (JSONObject) value : null;
	}

	public Object getDefinitionProperty(String key) {
		JSONObject object = getDefinitionObject();
		return object == null ? null : object.opt(key);
	}

	public void setDefinitionProperty(String key, Object value) throws EngineException {
		key = valueOrEmpty(key).trim();
		if (key.isBlank()) {
			throw new EngineException("Flow virtual property name must not be empty.");
		}
		if (!key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
			throw new EngineException("Unsupported Flow virtual property name: " + key);
		}
		String propertyPath = virtualPath.isBlank() ? key : virtualPath + "." + key;
		applyMutation(propertyPath, value);
		try {
			JSONObject object = getDefinitionObject();
			if (object != null) {
				object.put(key, value == null ? JSONObject.NULL : value);
				definition = object.toString();
			}
		} catch (JSONException e) {
			throw new EngineException("Unable to update Flow virtual property cache.", e);
		}
	}

	@Override
	public String getComment() {
		if ("node".equals(virtualKind)) {
			Object comment = getDefinitionProperty("comment");
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
		if (definition != null && definition.has(name)) {
			setDefinitionProperty(name, parseEditedValue(value, getDefinitionProperty(name)));
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
		String normalized = valueOrEmpty(name).trim().replaceAll("[^A-Za-z0-9_]", "_");
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
		String seed = (parent == null ? "" : parent.getQName()) + "|" + valueOrEmpty(virtualPath) + "|" + valueOrEmpty(name);
		long hash = 1125899906842597L;
		for (int i = 0; i < seed.length(); i++) {
			hash = 31 * hash + seed.charAt(i);
		}
		return Math.abs(hash == Long.MIN_VALUE ? 0 : hash);
	}

	private void appendDynamicProperties(Document document, Element root) throws EngineException {
		if (!exportOptions.contains(ExportOption.bIncludeDisplayName) || !isDefinitionWritable()) {
			return;
		}

		var value = getDefinitionValue();
		try {
			if ("node".equals(virtualKind) && value instanceof JSONObject json) {
				appendDynamicProperty(document, root, "comment", "Comment", getComment(), "Flow node comment.");
				for (var key : sortedKeys(json)) {
					if (!"comment".equals(key)) {
						appendDynamicProperty(document, root, key, key, json.opt(key), "Flow property \"" + key + "\".");
					}
				}
			} else if (isEditableScalarKind() && !(value instanceof JSONObject) && !(value instanceof JSONArray)) {
				appendDynamicProperty(document, root, "#flow_value", "Value", value, "Flow value.");
			}
		} catch (Exception e) {
			throw new EngineException("Unable to append Flow virtual properties.", e);
		}
	}

	private boolean isEditableScalarKind() {
		return "field".equals(virtualKind) || "binding".equals(virtualKind);
	}

	private void appendDynamicProperty(Document document, Element root, String name, String displayName, Object value,
			String description) throws Exception {
		var property = document.createElement("property");
		property.setAttribute("name", name);
		property.setAttribute("displayName", displayName);
		property.setAttribute("isHidden", "false");
		property.setAttribute("isMasked", "false");
		property.setAttribute("isExpert", "false");
		property.setAttribute("isDisabled", "false");
		property.setAttribute("category", "Base properties");
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
		DatabaseObject target = mutableSourceRoot();
		if (target == null || path.isBlank()) {
			return;
		}
		if (!isWritablePath(target, path)) {
			throw new EngineException("Flow virtual path \"" + path + "\" is read-only.");
		}
		try {
			JSONObject mutation = new JSONObject()
					.put("op", "replace")
					.put("path", path)
					.put("value", value == null ? JSONObject.NULL : value);
			JSONObject response = target instanceof Flow flow
					? new FlowEngineBridge().applyMutation(flow, mutation)
					: new FlowEngineBridge().applyMutation((FlowEngine) target, mutation);
			if (!response.optBoolean("ok", false)) {
				JSONObject error = response.optJSONObject("error");
				String message = error == null ? response.toString() : error.optString("message", error.toString());
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

	private DatabaseObject mutableSourceRoot() {
		DatabaseObject current = this;
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
		String text = valueOrEmpty(value).trim();
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
