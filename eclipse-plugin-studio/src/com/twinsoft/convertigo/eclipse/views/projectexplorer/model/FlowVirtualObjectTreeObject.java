/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
 * License  as published by  the Free Software Foundation; either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.property_editors.FlowOutputSchemaPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.property_editors.FlowPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.InfoPropertyDescriptor;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;

public class FlowVirtualObjectTreeObject extends DatabaseObjectTreeObject implements IOrderableTreeObject {

	private static final String P_FLOW_PROPERTY = "#flow_property:";
	private static final String P_FLOW_VALUE = "#flow_value";
	private static final String P_FLOW_INFO = "#flow_info:";
	private static final String P_FLOW_SCHEMA = "#flow_schema";
	private static final String P_COMMENT = "comment";
	private static final String CATEGORY = "Base properties";
	private static final String EXPERT_CATEGORY = "Expert";
	private static final String DATA_CATEGORY = "Data flow";

	public FlowVirtualObjectTreeObject(Viewer viewer, FlowVirtualObject object, boolean inherited) {
		super(viewer, object, inherited);
	}

	@Override
	public FlowVirtualObject getObject() {
		return (FlowVirtualObject) super.getObject();
	}

	public String getIconImagePath() {
		String icon = studioIcon(getObject().getVirtualInfoObject(), "iconFile16", "iconFile", "iconFile32", "icon");
		if (!icon.isBlank()) {
			return icon;
		}
		return studioIcon(getObject().getDefinitionObject(), "iconFile16", "iconFile", "iconFile32", "icon");
	}

	@Override
	public String getImageName() {
		var icon = getIconImagePath();
		if (icon.isBlank()) {
			return super.getImageName();
		}
		icon += (!isEnabled() ? "_disabled" : hasAncestorDisabled() ? "_unreachable" : "");
		icon += (isInherited ? "_inherited" : "");
		icon += (isDetectedObject ? "_detected" : "");
		return icon;
	}

	private static String studioIcon(JSONObject object, String... keys) {
		if (object == null) {
			return "";
		}
		for (var key : keys) {
			var icon = object.optString(key, "");
			if (!icon.isBlank() && !isIconifyIcon(icon)) {
				return icon;
			}
		}
		return "";
	}

	private static boolean isIconifyIcon(String icon) {
		return icon != null && icon.matches("[A-Za-z][A-Za-z0-9_-]*:[A-Za-z0-9_.-]+");
	}

	public boolean canLaunchEditor() {
		return isEditableSourceKind() && !getSourceFilePath().isBlank();
	}

	public boolean isReadOnlyReference() {
		return jsonFlag(getObject().getVirtualInfoObject(), "readOnly")
				|| jsonFlag(getObject().getVirtualInfoObject(), "readOnlyReference")
				|| jsonFlag(getObject().getDefinitionObject(), "readOnly");
	}

	private static boolean jsonFlag(JSONObject object, String key) {
		return object != null && object.optBoolean(key, false);
	}

	public void launchEditor(String editorType) {
		var filePath = getSourceFilePath();
		if (filePath.isBlank()) {
			return;
		}
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page == null) {
				return;
			}
			var file = new File(filePath);
			var workspaceFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toURI());
			if (workspaceFiles != null && workspaceFiles.length > 0) {
				IFile workspaceFile = workspaceFiles[0];
				page.openEditor(new FileEditorInput(workspaceFile), "org.eclipse.ui.genericeditor.GenericEditor");
				return;
			}
			var linkedFile = getLinkedEditorFile(file);
			page.openEditor(new FileEditorInput(linkedFile), "org.eclipse.ui.genericeditor.GenericEditor");
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to open Flow block source \"" + filePath + "\".");
		}
	}

	private IFile getLinkedEditorFile(File file) throws Exception {
		var project = getProjectTreeObject().getIProject();
		var editorFolder = project.getFolder("_private/editor/flow-blocks");
		ensureFolder(editorFolder);
		var linkedFile = editorFolder.getFile(linkName(file));
		var target = file.toURI();
		if (linkedFile.exists()) {
			if (target.equals(linkedFile.getLocationURI())) {
				return linkedFile;
			}
			linkedFile.delete(true, null);
		}
		linkedFile.createLink(target, IResource.NONE, null);
		return linkedFile;
	}

	private static void ensureFolder(IFolder folder) throws Exception {
		if (folder.exists()) {
			return;
		}
		var parent = folder.getParent();
		if (parent instanceof IFolder parentFolder) {
			ensureFolder(parentFolder);
		}
		folder.create(true, true, null);
	}

	private static String linkName(File file) {
		var name = file.getName().replaceAll("[^A-Za-z0-9._-]", "_");
		return Integer.toHexString(file.getAbsolutePath().hashCode()) + "_" + name;
	}

	@Override
	protected List<PropertyDescriptor> getDynamicPropertyDescriptors() {
		var descriptors = new ArrayList<PropertyDescriptor>(super.getDynamicPropertyDescriptors());
		var object = getObject();

		if (isFlowRootVirtualObject(object)) {
			addFlowOutputSchemaDescriptors(descriptors);
		}

		var value = object.getDefinitionValue();
		if (value instanceof JSONObject json) {
			var info = object.getVirtualInfoObject();
			var propertyDefinitions = info == null ? null : info.optJSONObject("propertyDefinitions");
			if ("node".equals(object.getVirtualKind())) {
				var descriptor = new TextPropertyDescriptor(P_COMMENT, "Comment");
				descriptor.setCategory(CATEGORY);
				descriptor.setDescription("Flow node comment displayed in the treeview comment column.");
				descriptors.add(descriptor);
				addStructuralNodeDescriptors(descriptors, json);
			}
			if (propertyDefinitions != null) {
				for (String key : propertyDefinitionKeys(info, propertyDefinitions)) {
					var definition = propertyDefinitions.optJSONObject(key);
					if (!isHiddenProperty(definition)) {
						addFlowPropertyDescriptor(descriptors, key, definition);
					}
				}
				for (String key : sortedKeys(json)) {
					if (!isInternalNodeProperty(key) && !propertyDefinitions.has(key)) {
						addFlowPropertyDescriptor(descriptors, key, null);
					}
				}
			} else {
				for (String key : sortedKeys(json)) {
					if (P_COMMENT.equals(key) || isInternalNodeProperty(key)) {
						continue;
					}
					addFlowPropertyDescriptor(descriptors, key, null);
				}
			}
		} else if (isEditableScalarKind() && isScalar(value)) {
			var descriptor = new TextPropertyDescriptor(P_FLOW_VALUE, "Value");
			descriptor.setCategory(CATEGORY);
			descriptor.setDescription("Flow value.");
			descriptors.add(descriptor);
		}
		addDataFlowDescriptors(descriptors, object.getVirtualInfoObject());
		return descriptors;
	}

	private static boolean isFlowRootVirtualObject(FlowVirtualObject object) {
		return object != null && "folder".equals(object.getVirtualKind()) && "flow".equals(object.getVirtualType());
	}

	private void addFlowOutputSchemaDescriptors(List<PropertyDescriptor> descriptors) {
		var schema = new FlowOutputSchemaPropertyDescriptor(P_FLOW_SCHEMA, "Output schema", this);
		schema.setCategory(DATA_CATEGORY);
		schema.setDescription("Opens the effective Flow result schema used by downstream pickers.");
		descriptors.add(schema);
	}

	private void addStructuralNodeDescriptors(List<PropertyDescriptor> descriptors, JSONObject json) {
		if (json.has("id")) {
			addFlowPropertyDescriptor(descriptors, "id", informationPropertyDefinition("id", "Stable node identifier."));
		}
		if (json.has("block")) {
			addFlowPropertyDescriptor(descriptors, "block", informationPropertyDefinition("block", "Block implementation used by this node."));
		}
	}

	private static JSONObject propertyDefinition(String label, String description, boolean expert) {
		var definition = new JSONObject();
		try {
			definition.put("label", label);
			definition.put("description", description);
			definition.put("expert", expert);
		} catch (Exception e) {
		}
		return definition;
	}

	private static JSONObject informationPropertyDefinition(String label, String description) {
		var definition = propertyDefinition(label, description, false);
		try {
			definition.put("category", "Information");
			definition.put("readOnly", true);
		} catch (Exception e) {
		}
		return definition;
	}

	private static boolean isStructuralNodeProperty(String key) {
		return "id".equals(key) || "block".equals(key) || P_COMMENT.equals(key);
	}

	private static boolean isInternalNodeProperty(String key) {
		return isStructuralNodeProperty(key) || "props".equals(key);
	}

	private void addFlowPropertyDescriptor(List<PropertyDescriptor> descriptors, String key, JSONObject definition) {
		var id = P_FLOW_PROPERTY + key;
		var readOnly = isReadOnlyProperty(definition) || !getObject().isDefinitionWritable();
		PropertyDescriptor descriptor = readOnly
				? new InfoPropertyDescriptor(id, propertyLabel(key, definition))
				: usesFlowEditor(key, definition)
						? new FlowPropertyDescriptor(id, propertyLabel(key, definition), this, key, definition)
						: new TextPropertyDescriptor(id, propertyLabel(key, definition));
		descriptor.setCategory(propertyCategory(definition));
		descriptor.setDescription(propertyDescription(key, definition));
		descriptors.add(descriptor);
	}

	private static boolean usesFlowEditor(String key, JSONObject definition) {
		if ("id".equals(key) || "block".equals(key)) {
			return false;
		}
		if (definition == null) {
			return true;
		}
		return !definition.optBoolean("readOnly", false);
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

	private static String propertyCategory(JSONObject definition) {
		if (definition == null) {
			return EXPERT_CATEGORY;
		}
		var category = definition.optString("category", "");
		if (!category.isBlank()) {
			return category;
		}
		return definition.optBoolean("expert", false) || definition.optBoolean("advanced", false)
				? EXPERT_CATEGORY
				: CATEGORY;
	}

	private static String propertyDescription(String key, JSONObject definition) {
		if (definition == null) {
			return "Flow property \"" + key + "\".";
		}
		var description = definition.optString("shortDescription", definition.optString("description", ""));
		if (description.isBlank()) {
			description = definition.optString("longDescription", "");
		}
		if (description.isBlank()) {
			description = "Flow property \"" + key + "\".";
		}
		return description;
	}

	private String getSourceFilePath() {
		var object = getObject();
		if (!isEditableSourceKind()) {
			return "";
		}
		var info = object.getVirtualInfoObject();
		if (info != null) {
			var sourcePath = info.optString("sourcePath", info.optString("file", ""));
			if (!sourcePath.isBlank()) {
				return sourcePath;
			}
		}
		var definition = object.getDefinitionObject();
		if (definition == null) {
			return "";
		}
		if ("block".equals(object.getVirtualKind()) || definition.has("file")) {
			return definition.optString("file", "");
		}
		return "";
	}

	private boolean isEditableSourceKind() {
		var kind = getObject().getVirtualKind();
		if ("blockHooks".equals(kind) || "typeResource".equals(kind)) {
			return true;
		}
		if ("blockImplementation".equals(kind) || "fragmentImplementation".equals(kind)) {
			return !"flow".equals(getObject().getVirtualType());
		}
		return false;
	}

	private boolean isEditableScalarKind() {
		return switch (getObject().getVirtualKind()) {
		case "field", "binding" -> true;
		default -> false;
		};
	}

	@Override
	public Object getPropertyValue(Object id) {
		var propertyName = String.valueOf(id);
		if (P_COMMENT.equals(propertyName)) {
			return getObject().getComment();
		}
		if (propertyName.startsWith(P_FLOW_PROPERTY)) {
			return stringify(getObject().getDefinitionProperty(propertyName.substring(P_FLOW_PROPERTY.length())));
		}
		if (P_FLOW_VALUE.equals(propertyName)) {
			return stringify(getObject().getDefinitionValue());
		}
		if (propertyName.startsWith(P_FLOW_INFO)) {
			return infoValue(propertyName.substring(P_FLOW_INFO.length()));
		}
		if (P_FLOW_SCHEMA.equals(propertyName)) {
			return "";
		}
		return super.getPropertyValue(id);
	}

	@Override
	public void setPropertyValue(Object id, Object value) {
		var propertyName = String.valueOf(id);
		if (propertyName.startsWith(P_FLOW_INFO) || P_FLOW_SCHEMA.equals(propertyName)) {
			return;
		}
		if ((P_COMMENT.equals(propertyName) || propertyName.startsWith(P_FLOW_PROPERTY) || P_FLOW_VALUE.equals(propertyName))
				&& !getObject().isDefinitionWritable()) {
			return;
		}
		if (!P_COMMENT.equals(propertyName) && !propertyName.startsWith(P_FLOW_PROPERTY) && !P_FLOW_VALUE.equals(propertyName)) {
			super.setPropertyValue(id, value);
			return;
		}
		if (propertyName.startsWith(P_FLOW_PROPERTY)) {
			var definition = flowPropertyDefinition(propertyName.substring(P_FLOW_PROPERTY.length()));
			if (definition != null && definition.optBoolean("readOnly", false)) {
				return;
			}
		}

		var oldValue = getPropertyValue(id);
		try {
			if (P_COMMENT.equals(propertyName)) {
				getObject().setComment(String.valueOf(value == null ? "" : value));
			} else if (propertyName.startsWith(P_FLOW_PROPERTY)) {
				var key = propertyName.substring(P_FLOW_PROPERTY.length());
				var currentValue = getObject().getDefinitionProperty(key);
				var parsedValue = parseEditedValue(value, currentValue);
				getObject().setDefinitionProperty(key, parsedValue);
			} else {
				var parsedValue = parseEditedValue(value, getObject().getDefinitionValue());
				getObject().setDefinitionValue(parsedValue);
			}

			reloadDescriptors();
			hasBeenModified(true);
			var treeViewer = (TreeViewer) getAdapter(TreeViewer.class);
			if (treeViewer != null) {
				treeViewer.update(this, null);
			}
			ConvertigoPlugin.projectManager.getProjectExplorerView()
					.fireTreeObjectPropertyChanged(new TreeObjectEvent(this, propertyName, oldValue, getPropertyValue(id)));
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Unable to update Flow virtual property \"" + propertyName + "\".");
		}
	}

	public void setFlowPropertyValue(String key, Object value) {
		setPropertyValue(P_FLOW_PROPERTY + key, value);
	}

	private JSONObject flowPropertyDefinition(String key) {
		var info = getObject().getVirtualInfoObject();
		var definitions = info == null ? null : info.optJSONObject("propertyDefinitions");
		return definitions == null ? null : definitions.optJSONObject(key);
	}

	private static void addDataFlowDescriptors(List<PropertyDescriptor> descriptors, JSONObject info) {
		if (info == null) {
			return;
		}
		if (hasValues(info.optJSONArray("reads"))) {
			addInfoDescriptor(descriptors, "reads", "Reads", "Scope paths read by this node.");
		}
		if (hasValues(info.optJSONArray("writes"))) {
			addInfoDescriptor(descriptors, "writes", "Writes", "Scope paths written by this node.");
		}
		if (hasValues(info.optJSONArray("inputs"))) {
			addInfoDescriptor(descriptors, "sources", "Sources", "Known producers for the paths read by this node.");
		}
	}

	private static boolean hasValues(JSONArray array) {
		return array != null && array.length() > 0;
	}

	private static void addInfoDescriptor(List<PropertyDescriptor> descriptors, String key, String label, String description) {
		var descriptor = new InfoPropertyDescriptor(P_FLOW_INFO + key, label);
		descriptor.setCategory(DATA_CATEGORY);
		descriptor.setDescription(description);
		descriptors.add(descriptor);
	}

	private String infoValue(String key) {
		var info = getObject().getVirtualInfoObject();
		if (info == null) {
			return "";
		}
		if ("reads".equals(key)) {
			return joinArray(info.optJSONArray("reads"));
		}
		if ("writes".equals(key)) {
			return joinArray(info.optJSONArray("writes"));
		}
		if ("sources".equals(key)) {
			return sourcesValue(info.optJSONArray("inputs"));
		}
		return "";
	}

	private static String joinArray(JSONArray array) {
		if (array == null || array.length() == 0) {
			return "";
		}
		var values = new ArrayList<String>();
		for (var i = 0; i < array.length(); i++) {
			var value = array.opt(i);
			if (value != null && !JSONObject.NULL.equals(value)) {
				values.add(String.valueOf(value));
			}
		}
		return String.join(", ", values);
	}

	private static String sourcesValue(JSONArray inputs) {
		if (inputs == null || inputs.length() == 0) {
			return "";
		}
		var values = new ArrayList<String>();
		for (var i = 0; i < inputs.length(); i++) {
			var input = inputs.optJSONObject(i);
			if (input == null) {
				continue;
			}
			var path = input.optString("path", "");
			var source = input.optJSONObject("source");
			if (source != null) {
				var sourceId = source.optString("id", "");
				var sourceBlock = source.optString("block", "");
				if (!sourceId.isBlank()) {
					var sourcePath = source.optString("path", "");
					var via = sourcePath.isBlank() || sourcePath.equals(path) ? "" : " via " + sourcePath;
					values.add(path + " <= " + sourceId + (sourceBlock.isBlank() ? "" : " [" + sourceBlock + "]") + via);
					continue;
				}
			}
			if (!path.isBlank()) {
				values.add(path);
			}
		}
		return String.join(", ", values);
	}

	private static List<String> sortedKeys(JSONObject object) {
		var keys = new ArrayList<String>();
		for (Iterator<?> it = object.keys(); it.hasNext();) {
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
		for (String key : sortedKeys(definitions)) {
			if (!keys.contains(key)) {
				keys.add(key);
			}
		}
		return keys;
	}

	private static boolean isScalar(Object value) {
		return !(value instanceof JSONObject)
				&& !(value instanceof JSONArray);
	}

	private static String stringify(Object value) {
		if (value == null || JSONObject.NULL.equals(value)) {
			return "";
		}
		return String.valueOf(value);
	}

	private static Object parseEditedValue(Object editedValue, Object currentValue) {
		if (!(editedValue instanceof String text)) {
			return editedValue;
		}
		var trimmed = text.trim();
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
}
