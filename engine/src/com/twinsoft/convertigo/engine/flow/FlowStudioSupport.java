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

package com.twinsoft.convertigo.engine.flow;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.engine.Engine;

public class FlowStudioSupport {

	private static final String FLOW_BLOCK_TYPE = "FlowBlock";
	private static final String FLOW_BLOCK_DEFINITION_TYPE = "FlowBlockDefinition";
	private static final String FLOW_TYPE_DEFINITION_TYPE = "FlowTypeDefinition";
	private static final String FLOW_PROPERTY_DEFINITION_TYPE = "FlowPropertyDefinition";
	private static final String FLOW_HELPER_DEFINITION_TYPE = "FlowHelperDefinition";
	private static final String FRONTEND_BLOCK_TYPE = "FrontendBlock";
	private static final String FLOW_BLOCK_ID_PREFIX = "flowblock:";
	private static final String FLOW_BLOCK_DEFINITION_ID_PREFIX = "flowblockdef:";
	private static final String FRONTEND_BLOCK_ID_PREFIX = "frontendblock:";
	private static final String FLOW_TYPE_DEFINITION_ID = "flowtypedef:property";
	private static final String FLOW_PROPERTY_DEFINITION_ID = "flowpropdef:property";
	private static final String FLOW_HELPER_DEFINITION_ID = "flowhelperdef:function";
	private static final String FLOW_ICON = "/com/twinsoft/convertigo/beans/flow/images/flow_color_32x32.png";
	private static final String FLOW_VIRTUAL_ICON = "/com/twinsoft/convertigo/beans/flow/images/flowvirtualobject_color_32x32.png";
	private static final String FLOW_SCRIPT_ICON = "/com/twinsoft/convertigo/beans/extractionrules/siteclipper/images/rule_script_color_32x32.png";
	private static final Map<String, JSONObject> catalogCache = new ConcurrentHashMap<>();
	private static final Map<String, ScheduledFuture<?>> frontendGenerationTasks = new ConcurrentHashMap<>();
	private static final ScheduledExecutorService frontendGenerationExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
		var thread = new Thread(r, "Flow frontend generator");
		thread.setDaemon(true);
		return thread;
	});

	private FlowStudioSupport() {
	}

	public static boolean isFlowPaletteTarget(DatabaseObject dbo) {
		return flowAuthoringRoot(dbo) != null
				&& (dbo instanceof Flow || dbo instanceof FlowEngine || dbo instanceof FlowVirtualObject);
	}

	public static boolean isFlowContextTarget(DatabaseObject dbo) {
		return isFlowPaletteTarget(dbo);
	}

	public static String paletteKey(DatabaseObject dbo) {
		var root = flowAuthoringRoot(dbo);
		var project = root == null ? null : root.getProject();
		if (project == null) {
			return "";
		}
		var flowEngine = root instanceof FlowEngine engine ? engine : project.getFlowEngine();
		var engineQName = flowEngine == null || flowEngine.getEngineQName() == null || flowEngine.getEngineQName().isBlank()
				? FlowEngineBridge.DEFAULT_ENGINE_QNAME
				: flowEngine.getEngineQName();
		if (root instanceof Flow flow) {
			return project.getName() + "|" + engineQName + "|" + flow.getFullQName() + "|" + Integer.toHexString(flow.getFlowSource().hashCode());
		}
		return project.getName() + "|" + engineQName;
	}

	public static DatabaseObject resolveTreeObject(String id) throws Exception {
		if (id == null || id.isBlank() || id.contains("/")) {
			return null;
		}
		var direct = resolveRealTreeObject(id);
		if (direct != null) {
			return direct;
		}
		var rootId = flowRootId(id);
		if (rootId.isBlank()) {
			return null;
		}
		var root = resolveRealTreeObject(rootId);
		if (root == null) {
			return null;
		}
		var virtualPath = id.substring(rootId.length() + 1);
		for (var child : root.getDatabaseObjectChildren()) {
			var resolved = resolveVirtualTreeObject(child, id, virtualPath);
			if (resolved != null) {
				return resolved;
			}
		}
		return null;
	}

	private static String flowRootId(String id) throws Exception {
		var cursor = id.lastIndexOf('.');
		while (cursor > 0) {
			var candidate = id.substring(0, cursor);
			var dbo = resolveRealTreeObject(candidate);
			if (dbo instanceof Flow || dbo instanceof FlowEngine) {
				return candidate;
			}
			cursor = id.lastIndexOf('.', cursor - 1);
		}
		return "";
	}

	private static DatabaseObject resolveRealTreeObject(String id) throws Exception {
		if (Engine.theApp == null || Engine.theApp.databaseObjectsManager == null) {
			return null;
		}
		try {
			return Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
		} catch (Exception e) {
			return null;
		}
	}

	private static DatabaseObject resolveVirtualTreeObject(DatabaseObject candidate, String id, String virtualPath) {
		if (candidate instanceof FlowVirtualObject fvo
				&& (id.equals(fvo.getFullQName()) || virtualPath.equals(fvo.getVirtualPath()) || virtualPath.equals(fvo.getName()))) {
			return fvo;
		}
		try {
			for (var child : candidate.getDatabaseObjectChildren()) {
				var resolved = resolveVirtualTreeObject(child, id, virtualPath);
				if (resolved != null) {
					return resolved;
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	public static void clearCatalogCache(DatabaseObject dbo) {
		var root = flowAuthoringRoot(dbo);
		if (root instanceof Flow flow && flow.getProject() != null) {
			var flowPrefix = flow.getProject().getName() + "|" + effectiveEngineQName(flow) + "|" + flow.getFullQName() + "|";
			catalogCache.keySet().removeIf(key -> key.startsWith(flowPrefix));
			return;
		}
		var key = paletteKey(dbo);
		if (key.isBlank()) {
			catalogCache.clear();
			return;
		}
		catalogCache.remove(key);
	}

	public static JSONObject contextMenu(DatabaseObject targetDbo) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		if (root == null) {
			return emptyContextMenu();
		}
		var request = contextRequest(root, targetDbo);
		return root instanceof Flow flow
				? new FlowEngineBridge().contextMenu(flow, request)
				: new FlowEngineBridge().contextMenu((FlowEngine) root, request);
	}

	public static JSONObject outputSchema(DatabaseObject targetDbo) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof Flow flow)) {
			return new JSONObject()
					.put("ok", false)
					.put("message", "No Flow output schema is available for this selection.");
		}
		return new FlowEngineBridge().outputSchema(flow, new JSONObject()
				.put("source", "effective")
				.put("detail", "full")
				.put("allowRequestableSchema", false));
	}

	public static JSONObject contextAction(DatabaseObject targetDbo, JSONObject action) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		if (root == null || action == null) {
			return new JSONObject()
					.put("ok", false)
					.put("message", "No Flow context action is available for this selection.");
		}
		if ("flow.cache.clear".equals(action.optString("id", ""))) {
			FlowEngineBridge.clearCaches();
		}
		var request = contextRequest(root, targetDbo).put("action", action);
		var response = root instanceof Flow flow
				? new FlowEngineBridge().contextAction(flow, request)
				: new FlowEngineBridge().contextAction((FlowEngine) root, request);
		if (response.optBoolean("refreshPalette", false) || response.optBoolean("refresh", false)) {
			clearCatalogCache(root);
		}
		return response;
	}

	private static JSONObject emptyContextMenu() throws Exception {
		return new JSONObject()
				.put("ok", true)
				.put("protocol", "flow.studio.menu.v1")
				.put("items", new JSONArray());
	}

	private static JSONObject contextRequest(DatabaseObject root, DatabaseObject targetDbo) throws Exception {
		var request = new JSONObject()
				.put("surface", "studio.treeview")
				.put("root", contextObject(root))
				.put("targetObject", contextObject(targetDbo));
		if (root instanceof Flow flow) {
			request.put("flowName", flow.getName());
			request.put("flowQName", flow.getFullQName());
		}
		return request;
	}

	private static JSONObject contextObject(DatabaseObject dbo) throws Exception {
		var object = new JSONObject();
		if (dbo == null) {
			return object;
		}
		object.put("name", dbo.getName())
				.put("qname", dbo.getFullQName())
				.put("className", dbo.getClass().getName())
				.put("simpleClassName", dbo.getClass().getSimpleName());
		if (dbo.getProject() != null) {
			object.put("project", dbo.getProject().getName())
					.put("projectDir", dbo.getProject().getDirPath());
		}
		if (dbo instanceof Flow) {
			object.put("kind", "flow");
		} else if (dbo instanceof FlowEngine) {
			object.put("kind", "engine");
		} else if (dbo instanceof FlowVirtualObject fvo) {
			object.put("kind", fvo.getVirtualKind())
					.put("type", fvo.getVirtualType())
					.put("path", fvo.getVirtualPath())
					.put("summary", fvo.getSummary());
			var value = fvo.getDefinitionValue();
			object.put("definition", value == null ? JSONObject.NULL : value);
			var info = fvo.getVirtualInfoObject();
			if (info != null) {
				object.put("info", info);
			}
		}
		return object;
	}

	public static boolean canAddBlock(DatabaseObject targetDbo, String position, String blockName) {
		var root = flowAuthoringRoot(targetDbo);
		if (root == null || !isWritablePaletteTarget(targetDbo) || blockName == null || blockName.isBlank()) {
			return false;
		}
		try {
			return insertionFor(root, targetDbo, position, blockName, new JSONObject()) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean canAddBlockDefinition(DatabaseObject targetDbo, String runtime) {
		return isBlockDefinitionPaletteTarget(targetDbo) && ("flow".equals(runtime) || "rhino".equals(runtime));
	}

	public static boolean canAddTypeDefinition(DatabaseObject targetDbo) {
		return isTypeDefinitionPaletteTarget(targetDbo);
	}

	public static boolean canAddPropertyDefinition(DatabaseObject targetDbo) {
		return propertyDefinitionTarget(targetDbo) != null;
	}

	public static boolean canAddHelperDefinition(DatabaseObject targetDbo) {
		return helperDefinitionTarget(targetDbo) != null;
	}

	public static boolean canAddFrontendBlock(DatabaseObject targetDbo, String position, JSONObject data) {
		try {
			if (!frontendTargetKindsMatch(data, targetDbo)) {
				return false;
			}
			var insert = frontendInsertValue(data);
			return frontendSourceCreationSpec(targetDbo, insert) != null
					|| frontendEngineMutationFor(targetDbo, insert) != null
					|| frontendMutationFor(targetDbo, position, insert) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean canShowFrontendBlocks(DatabaseObject targetDbo) {
		return isFrontendPaletteTarget(targetDbo);
	}

	public static boolean canAddFromPalette(DatabaseObject targetDbo, String position, JSONObject transfer) {
		var data = transfer == null ? null : transfer.optJSONObject("data");
		var type = data == null ? "" : data.optString("type", "");
		if (FLOW_BLOCK_TYPE.equals(type)) {
			return canAddBlock(targetDbo, position, data.optString("block", data.optString("classname", "")));
		}
		if (FLOW_BLOCK_DEFINITION_TYPE.equals(type)) {
			return canAddBlockDefinition(targetDbo, data.optString("runtime", "flow"));
		}
		if (FLOW_TYPE_DEFINITION_TYPE.equals(type)) {
			return canAddTypeDefinition(targetDbo);
		}
		if (FLOW_PROPERTY_DEFINITION_TYPE.equals(type)) {
			return canAddPropertyDefinition(targetDbo);
		}
		if (FLOW_HELPER_DEFINITION_TYPE.equals(type)) {
			return canAddHelperDefinition(targetDbo);
		}
		if (FRONTEND_BLOCK_TYPE.equals(type)) {
			return canAddFrontendBlock(targetDbo, position, data);
		}
		return false;
	}

	public static JSONArray paletteCategories(DatabaseObject targetDbo) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		var categories = new JSONArray();
		if (root == null) {
			return categories;
		}
		if (isFrontendPaletteTarget(targetDbo)) {
			var frontendCategories = frontendBlockCategories(root, targetDbo);
			for (int i = 0; i < frontendCategories.length(); i++) {
				categories.put(frontendCategories.get(i));
			}
			if (!(targetDbo instanceof FlowEngine)) {
				return categories;
			}
		}

		var grouped = new LinkedHashMap<String, JSONObject>();
		if (isBlockDefinitionPaletteTarget(targetDbo)) {
			categories.put(blockDefinitionCategory(targetDbo));
		}
		if (isTypeDefinitionPaletteTarget(targetDbo)) {
			categories.put(typeDefinitionCategory(targetDbo));
		}
		if (isPropertyDefinitionPaletteTarget(targetDbo)) {
			categories.put(propertyDefinitionCategory(targetDbo));
		}
		if (categories.length() > 0) {
			return categories;
		}
		if (isHelperDefinitionPaletteTarget(targetDbo)) {
			categories.put(helperDefinitionCategory(targetDbo));
			if (!isWritablePaletteTarget(targetDbo)) {
				return categories;
			}
		}
		if (!isWritablePaletteTarget(targetDbo)) {
			return categories;
		}
		var blocks = catalog(root).optJSONArray("blocks");
		if (blocks != null) {
			for (int i = 0; i < blocks.length(); i++) {
				var block = blocks.optJSONObject(i);
				if (block != null) {
					var categoryName = blockCategoryName(block);
					var category = grouped.computeIfAbsent(categoryName, name -> {
						try {
							return new JSONObject()
									.put("type", "Category")
									.put("name", name)
									.put("items", new JSONArray());
						} catch (Exception e) {
							return new JSONObject();
						}
					});
					category.getJSONArray("items").put(paletteItem(block));
				}
			}
		}
		for (var category : grouped.values()) {
			if (category.optJSONArray("items") != null && category.getJSONArray("items").length() > 0) {
				categories.put(category);
			}
		}
		return categories;
	}

	private static String blockCategoryName(JSONObject block) {
		var provider = firstNonBlank(block, "provider", "origin");
		var namespace = firstNonBlank(block, "namespace");
		if (provider.isBlank()) {
			provider = "unknown";
		}
		return namespace.isBlank()
				? "Flow blocks - " + provider
				: "Flow blocks - " + provider + " / " + namespace;
	}

	private static JSONArray frontendBlockCategories(DatabaseObject root, DatabaseObject targetDbo) throws Exception {
		var categories = new JSONArray();
		var grouped = new LinkedHashMap<String, JSONObject>();
		var blocks = frontendPaletteDescriptors(root);
		if (blocks == null) {
			return categories;
		}
		for (int i = 0; i < blocks.length(); i++) {
			var block = blocks.optJSONObject(i);
			if (block == null || !frontendBlockSupportsTarget(block, targetDbo)) {
				continue;
			}
			var categoryName = frontendBlockCategoryName(block);
			var category = grouped.computeIfAbsent(categoryName, name -> {
				try {
					return new JSONObject()
							.put("type", "Category")
							.put("name", name)
							.put("items", new JSONArray());
				} catch (Exception e) {
					return new JSONObject();
				}
			});
			category.getJSONArray("items").put(frontendPaletteItem(block));
		}
		for (var category : grouped.values()) {
			if (category.optJSONArray("items") != null && category.getJSONArray("items").length() > 0) {
				categories.put(category);
			}
		}
		return categories;
	}

	private static String frontendBlockCategoryName(JSONObject block) {
		if (block.optBoolean("createAction", false) || "create".equals(firstNonBlank(block, "descriptorKind"))) {
			var category = firstNonBlank(block, "category");
			return category.isBlank()
					? "Frontend create actions"
					: "Frontend create actions - " + category;
		}
		var provider = firstNonBlank(block, "provider");
		var namespace = firstNonBlank(block, "namespace");
		var category = firstNonBlank(block, "category");
		if (provider.isBlank()) {
			provider = "unknown";
		}
		var prefix = namespace.isBlank()
				? "Frontend blocks - " + provider
				: "Frontend blocks - " + provider + " / " + namespace;
		return category.isBlank() ? prefix : prefix + " / " + category;
	}

	private static JSONArray frontendPaletteDescriptors(DatabaseObject root) throws Exception {
		var catalog = catalog(root);
		var descriptors = new JSONArray();
		appendObjects(descriptors, catalog.optJSONArray("frontendBlocks"));
		appendObjects(descriptors, catalog.optJSONArray("frontendCreateDescriptors"));
		return descriptors;
	}

	private static void appendObjects(JSONArray target, JSONArray source) {
		if (source == null) {
			return;
		}
		for (int i = 0; i < source.length(); i++) {
			var object = source.optJSONObject(i);
			if (object != null) {
				target.put(object);
			}
		}
	}

	private static boolean frontendBlockSupportsTarget(JSONObject block, DatabaseObject targetDbo) {
		if (!frontendTargetKindsMatch(block, targetDbo)) {
			return false;
		}
		return canAddFrontendBlock(targetDbo, "inside", block)
				|| canAddFrontendBlock(targetDbo, "before", block)
				|| canAddFrontendBlock(targetDbo, "after", block);
	}

	private static JSONObject frontendPaletteItem(JSONObject block) throws Exception {
		var blockId = firstNonBlank(block, "id", "name");
		var label = firstNonBlank(block, "label", "name", "id");
		var description = firstNonBlank(block, "description");
		var icon = studioIcon(block);
		var item = new JSONObject()
				.put("type", FRONTEND_BLOCK_TYPE)
				.put("id", FRONTEND_BLOCK_ID_PREFIX + blockId)
				.put("name", label)
				.put("classname", blockId)
				.put("block", blockId)
				.put("description", description)
				.put("shortDescriptionHtml", html(description))
				.put("shortDescriptionText", description)
				.put("longDescriptionHtml", "")
					.put("longDescriptionText", "")
					.put("propertiesDescriptionHtml", propertiesDescription(block.optJSONObject("properties")))
					.put("icon", icon)
					.put("tooltip", blockId)
					.put("builtin", false)
					.put("additional", true)
					.put("insert", block.optJSONObject("insert") == null ? new JSONObject() : block.getJSONObject("insert"));
		if (block.optBoolean("createAction", false) || "create".equals(firstNonBlank(block, "descriptorKind"))) {
			item.put("createAction", true);
		}
		if (block.optJSONArray("targetKinds") != null) {
			item.put("targetKinds", block.getJSONArray("targetKinds"));
		}
		for (var key : new String[] { "iconFile32", "iconFile16", "iconFile" }) {
			var value = firstNonBlank(block, key);
			if (!value.isBlank()) {
				item.put(key, value);
			}
		}
		var iconify = firstNonBlank(block, "iconify", "icon");
		if (!iconify.isBlank()) {
			item.put("iconify", iconify);
		}
		return item;
	}

	public static boolean isFlowPaletteData(JSONObject transfer) {
		var data = transfer == null ? null : transfer.optJSONObject("data");
		return "paletteData".equals(transfer == null ? "" : transfer.optString("type"))
				&& (FLOW_BLOCK_TYPE.equals(data == null ? "" : data.optString("type"))
						|| FLOW_BLOCK_DEFINITION_TYPE.equals(data == null ? "" : data.optString("type"))
						|| FLOW_TYPE_DEFINITION_TYPE.equals(data == null ? "" : data.optString("type"))
						|| FLOW_PROPERTY_DEFINITION_TYPE.equals(data == null ? "" : data.optString("type"))
						|| FLOW_HELPER_DEFINITION_TYPE.equals(data == null ? "" : data.optString("type"))
						|| FRONTEND_BLOCK_TYPE.equals(data == null ? "" : data.optString("type")));
	}

	public static JSONObject addBlock(DatabaseObject targetDbo, String position, String blockName) throws Exception {
		var transfer = new JSONObject()
				.put("type", "paletteData")
				.put("data", new JSONObject()
						.put("type", FLOW_BLOCK_TYPE)
						.put("block", blockName)
						.put("classname", blockName));
		return addFromPalette(targetDbo, position, transfer);
	}

	public static JSONObject addFromPalette(DatabaseObject targetDbo, String position, JSONObject transfer) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		var data = transfer == null ? null : transfer.optJSONObject("data");
		if (root == null || data == null) {
			return new JSONObject().put("done", false);
		}

		if (FLOW_BLOCK_DEFINITION_TYPE.equals(data.optString("type", ""))) {
			return addBlockDefinition(targetDbo, data.optString("runtime", "flow"));
		}
		if (FLOW_TYPE_DEFINITION_TYPE.equals(data.optString("type", ""))) {
			return addTypeDefinition(targetDbo);
		}
		if (FLOW_PROPERTY_DEFINITION_TYPE.equals(data.optString("type", ""))) {
			return addPropertyDefinition(targetDbo);
		}
		if (FLOW_HELPER_DEFINITION_TYPE.equals(data.optString("type", ""))) {
			return addHelperDefinition(targetDbo);
		}
		if (FRONTEND_BLOCK_TYPE.equals(data.optString("type", ""))) {
			return addFrontendBlock(targetDbo, position, data);
		}

		var blockName = data.optString("block", data.optString("classname", ""));
		if (blockName.isBlank()) {
			return new JSONObject().put("done", false);
		}

		var node = new JSONObject()
				.put("id", nextNodeId(root, blockName))
				.put("block", blockName);
		addDefaultProperties(root, blockName, node);
		var insertion = insertionFor(root, targetDbo, position, blockName, node);
		if (insertion == null) {
			return new JSONObject().put("done", false);
		}

		var response = applyMutation(root, targetDbo, insertion);
		var done = response.optBoolean("ok", false);
		return new JSONObject()
				.put("done", done)
				.put("id", done ? root.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
	}

	private static JSONObject addBlockDefinition(DatabaseObject targetDbo, String runtime) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof FlowEngine flowEngine) || !canAddBlockDefinition(targetDbo, runtime)) {
			return new JSONObject().put("done", false);
		}
		var blockName = nextBlockName(flowEngine, runtime);
		var response = new FlowEngineBridge().createBlock(flowEngine, blockName, runtime);
		var done = isSuccessResponse(response);
		if (done) {
			clearCatalogCache(flowEngine);
		}
		return new JSONObject()
				.put("done", done)
				.put("id", done ? flowEngine.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
	}

	private static JSONObject addTypeDefinition(DatabaseObject targetDbo) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof FlowEngine flowEngine) || !canAddTypeDefinition(targetDbo)) {
			return new JSONObject().put("done", false);
		}
		var typeName = nextTypeName(flowEngine);
		var response = new FlowEngineBridge().createType(flowEngine, typeName);
		var done = isSuccessResponse(response);
		if (done) {
			clearCatalogCache(flowEngine);
		}
		return new JSONObject()
				.put("done", done)
				.put("id", done ? flowEngine.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
	}

	private static JSONObject addPropertyDefinition(DatabaseObject targetDbo) throws Exception {
		var target = propertyDefinitionTarget(targetDbo);
		var root = flowAuthoringRoot(target);
		if (!(root instanceof FlowEngine flowEngine) || target == null) {
			return new JSONObject().put("done", false);
		}
		var name = nextPropertyName(target);
		var descriptor = new JSONObject()
				.put("label", name)
				.put("kind", "template")
				.put("type", "string")
				.put("description", "Block property \"" + name + "\".");
		var response = new FlowEngineBridge().applySourceMutation(flowEngine, sourcePath(target), new JSONObject()
				.put("op", "replace")
				.put("path", mutationPath(target) + "." + name)
				.put("value", descriptor));
		var done = isSuccessResponse(response);
		if (done) {
			clearCatalogCache(flowEngine);
		}
		return new JSONObject()
				.put("done", done)
				.put("id", done ? flowEngine.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
	}

	private static JSONObject addHelperDefinition(DatabaseObject targetDbo) throws Exception {
		var target = helperDefinitionTarget(targetDbo);
		var root = flowAuthoringRoot(target);
		if (!(root instanceof Flow flow) || target == null) {
			return new JSONObject().put("done", false);
		}
		var name = nextHelperName(flow);
		var helper = new JSONObject()
				.put("name", name)
				.put("params", new JSONArray().put("value"))
				.put("nodes", new JSONArray()
						.put(new JSONObject()
								.put("id", "returnValue")
								.put("block", "return")
								.put("value", "{{ value }}")));
		var response = applyMutation(flow, target, new JSONObject()
				.put("op", "append")
				.put("path", "helpers")
				.put("value", helper));
		var done = response.optBoolean("ok", false);
		if (done) {
			clearCatalogCache(flow);
		}
		return new JSONObject()
				.put("done", done)
				.put("id", done ? flow.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
	}

	private static JSONObject addFrontendBlock(DatabaseObject targetDbo, String position, JSONObject data) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof FlowEngine flowEngine)) {
			return new JSONObject().put("done", false);
		}
		var insert = frontendInsertValue(data);
		var sourceCreation = frontendSourceCreationSpec(targetDbo, insert);
		if (sourceCreation != null) {
			return createFrontendSource(flowEngine, targetDbo, sourceCreation);
		}
		var engineMutation = frontendEngineMutationFor(targetDbo, insert);
		if (engineMutation != null) {
			var response = new FlowEngineBridge().applyMutation(flowEngine, engineMutation);
			var done = isSuccessResponse(response);
			if (done) {
				clearCatalogCache(flowEngine);
			}
			return new JSONObject()
					.put("done", done)
					.put("id", done ? flowEngine.getFullQName() : "")
					.put("error", done ? JSONObject.NULL : response.opt("error"));
		}
		var mutation = frontendMutationFor(targetDbo, position, insert);
		if (mutation == null) {
			return new JSONObject().put("done", false);
		}
		var response = applyFrontendMutation(flowEngine, targetDbo, mutation);
		var done = response.optBoolean("ok", false);
		return new JSONObject()
				.put("done", done)
				.put("id", done ? flowEngine.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
	}

	private static JSONObject frontendInsertValue(JSONObject data) throws Exception {
		var insert = data == null ? null : data.optJSONObject("insert");
		if (insert == null) {
			insert = new JSONObject()
					.put("id", "widget")
					.put("kind", "text")
					.put("text", data == null ? "Widget" : data.optString("name", "Widget"));
		}
		return new JSONObject(insert.toString());
	}

	private static JSONObject frontendEngineMutationFor(DatabaseObject targetDbo, JSONObject insert) throws Exception {
		if (!(flowAuthoringRoot(targetDbo) instanceof FlowEngine)) {
			return null;
		}
		if (!(targetDbo instanceof FlowEngine)
				&& !(targetDbo instanceof FlowVirtualObject fvo && "frontends".equals(fvo.getVirtualType()))) {
			return null;
		}
		var path = firstNonBlank(insert, "__engineMutationPath");
		if (path.isBlank()) {
			return null;
		}
		var op = firstNonBlank(insert, "__engineMutationOp");
		if (op.isBlank()) {
			op = "merge";
		}
		return new JSONObject()
				.put("op", op)
				.put("path", path)
				.put("value", cleanFrontendInsertValue(insert));
	}

	private static JSONObject frontendSourceCreationSpec(DatabaseObject targetDbo, JSONObject insert) throws Exception {
		if (!(flowAuthoringRoot(targetDbo) instanceof FlowEngine)) {
			return null;
		}
		if (!frontendSourceCreationWritableTarget(targetDbo)) {
			return null;
		}
		var create = insert == null ? null : insert.optJSONObject("__frontendCreateSource");
		if (create == null) {
			return null;
		}
		var baseId = firstNonBlank(create, "baseId");
		var directory = firstNonBlank(create, "directory");
		var fileName = firstNonBlank(create, "fileName");
		var source = create.optString("source", "");
		if (baseId.isBlank() || directory.isBlank() || fileName.isBlank() || source.isBlank()) {
			return null;
		}
		var spec = new JSONObject(create.toString());
		var namespace = frontendSourceTargetNamespace(targetDbo);
		if (!namespace.isBlank()) {
			spec.put("__targetNamespace", namespace);
		}
		return spec;
	}

	private static JSONObject createFrontendSource(FlowEngine flowEngine, DatabaseObject targetDbo, JSONObject create) throws Exception {
		var builderName = firstNonBlank(create, "builder");
		if (builderName.isBlank()) {
			builderName = frontendBuilderName(targetDbo);
		}
		var baseId = firstNonBlank(create, "baseId");
		var targetNamespace = firstNonBlank(create, "__targetNamespace");
		if (!targetNamespace.isBlank()) {
			baseId = targetNamespace + "." + frontendSourceLocalName(baseId);
		}
		var blockId = uniqueFrontendSourceId(flowEngine, baseId);
		var values = frontendSourceTemplateValues(builderName, blockId);
		var directory = applyTemplate(firstNonBlank(create, "directory"), values);
		var fileName = applyTemplate(firstNonBlank(create, "fileName"), values);
		values.put("fileName", fileName);
		var source = applyTemplate(create.optString("source", ""), values);
		var project = flowEngine.getProject();
		var projectDir = project == null ? new File(".") : project.getDirFile();
		var rootDir = new File(projectDir, "libs/flow/frontbuilder/" + safeFileName(builderName));
		var dir = new File(rootDir, directory);
		var file = new File(dir, fileName);
		var rootPath = rootDir.getCanonicalPath();
		var filePath = file.getCanonicalPath();
		if (!filePath.startsWith(rootPath + File.separator)) {
			return new JSONObject()
					.put("done", false)
					.put("error", "Frontend source path escapes builder root: " + filePath);
		}
		if (file.isFile()) {
			return new JSONObject()
					.put("done", false)
					.put("error", "Frontend source already exists: " + file.getAbsolutePath());
		}
		file.getParentFile().mkdirs();
		FileUtils.writeStringToFile(file, source, "UTF-8");
		FlowEngineBridge.clearCaches();
		clearCatalogCache(flowEngine);
		return new JSONObject()
				.put("done", true)
				.put("id", flowEngine.getFullQName())
				.put("file", file.getAbsolutePath())
				.put("sourceId", blockId);
	}

	private static boolean frontendSourceCreationWritableTarget(DatabaseObject targetDbo) {
		if (targetDbo instanceof FlowEngine) {
			return true;
		}
		DatabaseObject cursor = targetDbo;
		while (cursor instanceof FlowVirtualObject fvo) {
			var sourceWritable = sourceFlagValue(fvo, "sourceWritable");
			if (sourceWritable != null) {
				return sourceWritable;
			}
			cursor = fvo.getParent();
		}
		return false;
	}

	private static String frontendSourceTargetNamespace(DatabaseObject targetDbo) {
		DatabaseObject cursor = targetDbo;
		while (cursor instanceof FlowVirtualObject fvo) {
			var definition = fvo.getDefinitionObject();
			if (definition != null && definition.has("namespace")) {
				return definition.optString("namespace", "");
			}
			cursor = fvo.getParent();
		}
		return "";
	}

	private static String frontendSourceLocalName(String blockId) {
		blockId = blockId == null || blockId.isBlank() ? "item" : blockId;
		var lastDot = blockId.lastIndexOf('.');
		return lastDot < 0 ? blockId : blockId.substring(lastDot + 1);
	}

	private static String uniqueFrontendSourceId(DatabaseObject root, String baseId) throws Exception {
		baseId = baseId == null || baseId.isBlank() ? "project.item" : baseId;
		var used = new HashSet<String>();
		var blocks = catalog(root).optJSONArray("frontendBlocks");
		if (blocks != null) {
			for (int i = 0; i < blocks.length(); i++) {
				var block = blocks.optJSONObject(i);
				if (block != null) {
					var id = firstNonBlank(block, "id", "name");
					if (!id.isBlank()) {
						used.add(id);
					}
				}
			}
		}
		var candidate = baseId;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = baseId + i;
		}
		return candidate;
	}

	private static Map<String, String> frontendSourceTemplateValues(String builderName, String blockId) {
		var values = new LinkedHashMap<String, String>();
		var lastDot = blockId.lastIndexOf('.');
		var namespace = lastDot < 0 ? "" : blockId.substring(0, lastDot);
		var localName = lastDot < 0 ? blockId : blockId.substring(lastDot + 1);
		var tag = frontendComponentTag(localName);
		values.put("builder", builderName);
		values.put("id", blockId);
		values.put("namespace", namespace);
		values.put("namespacePath", namespace.replace('.', '/'));
		values.put("localName", localName);
		values.put("LocalName", tag);
		values.put("tag", tag);
		values.put("actionName", lowerFirst(tag));
		return values;
	}

	private static String applyTemplate(String template, Map<String, String> values) {
		var out = template == null ? "" : template;
		for (var entry : values.entrySet()) {
			out = out.replace("${" + entry.getKey() + "}", entry.getValue());
		}
		return out;
	}

	private static JSONObject frontendMutationFor(DatabaseObject targetDbo, String position, JSONObject insert) throws Exception {
		if (!(targetDbo instanceof FlowVirtualObject fvo) || sourcePath(fvo).isBlank() || !sourceFlag(fvo, "sourceWritable")) {
			return null;
		}
		position = position == null || position.isBlank() ? "inside" : position;
		var insertValue = uniqueFrontendInsertValue(fvo, cleanFrontendInsertValue(insert));
		var insertSourcePath = firstNonBlank(insert, "__frontendSourcePath");
		if (insertSourcePath.isBlank()) {
			insertSourcePath = sourceValue(fvo, "frontendInsertSourcePath");
		}
		if (insertSourcePath.isBlank()) {
			insertSourcePath = sourcePath(fvo);
		}
		var insertMutationPath = firstNonBlank(insert, "__frontendMutationPath");
		if (insertMutationPath.isBlank()) {
			insertMutationPath = sourceValue(fvo, "frontendInsertMutationPath");
		}
		if (!insertSourcePath.isBlank() && !insertMutationPath.isBlank()) {
			return new JSONObject()
					.put("op", firstNonBlank(insert, "__frontendMutationOp").isBlank() ? "append" : firstNonBlank(insert, "__frontendMutationOp"))
					.put("path", insertMutationPath)
					.put("value", insertValue)
					.put("__sourcePath", insertSourcePath);
		}
		var referencedComponent = frontendReferencedComponent(fvo);
		if (referencedComponent != null && isSourceBackedTarget(referencedComponent)) {
			return new JSONObject()
					.put("op", "append")
					.put("path", mutationPath(referencedComponent) + ".widgets")
					.put("value", insertValue)
					.put("__sourcePath", sourcePath(referencedComponent));
		}
		if ("frontendComponent".equals(fvo.getVirtualKind()) && isSourceBackedTarget(fvo)) {
			return new JSONObject()
					.put("op", "append")
					.put("path", mutationPath(fvo) + ".widgets")
					.put("value", insertValue);
		}
		if ("frontendWidget".equals(fvo.getVirtualKind()) && isSourceBackedTarget(fvo)) {
			var arrayPath = parentArrayPath(mutationPath(fvo));
			if (arrayPath == null) {
				return null;
			}
			if ("inside".equals(position)) {
				position = "after";
			}
			var index = "before".equals(position) ? arrayIndex(mutationPath(fvo)) : arrayIndex(mutationPath(fvo)) + 1;
			return new JSONObject()
					.put("op", "insert")
					.put("path", arrayPath)
					.put("index", index)
					.put("value", insertValue);
		}
		var widgetsPath = frontendReferencedComponentWidgetsPath(fvo);
		if (widgetsPath != null) {
			return new JSONObject()
					.put("op", "append")
					.put("path", widgetsPath)
					.put("value", insertValue);
		}
		return null;
	}

	private static JSONObject cleanFrontendInsertValue(JSONObject insert) throws Exception {
		var value = new JSONObject(insert == null ? "{}" : insert.toString());
		var keys = new ArrayList<String>();
		for (var iterator = value.keys(); iterator.hasNext();) {
			keys.add(String.valueOf(iterator.next()));
		}
		for (var key : keys) {
			if (key.startsWith("__")) {
				value.remove(key);
			}
		}
		return value;
	}

	private static boolean frontendTargetKindsMatch(JSONObject data, DatabaseObject targetDbo) {
		var targetKinds = data == null ? null : data.optJSONArray("targetKinds");
		if (targetKinds == null || targetKinds.length() == 0) {
			return true;
		}
		var targetKind = "";
		var targetType = "";
		var acceptedKinds = new HashSet<String>();
		if (targetDbo instanceof FlowEngine) {
			targetKind = "flowEngine";
		} else if (targetDbo instanceof FlowVirtualObject fvo) {
			targetKind = fvo.getVirtualKind();
			targetType = fvo.getVirtualType();
			if ("frontendBlockImplementation".equals(targetKind)) {
				acceptedKinds.add("frontendComponent");
				acceptedKinds.add("frontendWidget");
			}
		}
		if (!targetKind.isBlank()) {
			acceptedKinds.add(targetKind);
		}
		if (!targetType.isBlank()) {
			acceptedKinds.add(targetType);
		}
		for (int i = 0; i < targetKinds.length(); i++) {
			var expected = targetKinds.optString(i);
			if (acceptedKinds.contains(expected)) {
				return true;
			}
		}
		return false;
	}

	private static FlowVirtualObject frontendReferencedComponent(FlowVirtualObject target) throws Exception {
		var componentId = frontendReferencedComponentId(target);
		if (componentId == null || componentId.isBlank()) {
			return null;
		}
		DatabaseObject cursor = target;
		while (cursor != null) {
			var found = findFrontendComponent(cursor, componentId);
			if (found != null) {
				return found;
			}
			cursor = cursor.getParent();
		}
		return findFrontendComponent(flowAuthoringRoot(target), componentId);
	}

	private static FlowVirtualObject findFrontendComponent(DatabaseObject candidate, String componentId) throws Exception {
		if (candidate == null) {
			return null;
		}
		if (candidate instanceof FlowVirtualObject fvo && "frontendComponent".equals(fvo.getVirtualKind())) {
			var definition = fvo.getDefinitionObject();
			var id = definition == null ? "" : definition.optString("id", "");
			if (componentId.equals(id) || componentId.equals(fvo.getName()) || componentId.equals(fvo.getSummary())) {
				return fvo;
			}
		}
		for (var child : candidate.getDatabaseObjectChildren()) {
			var found = findFrontendComponent(child, componentId);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static String frontendReferencedComponentWidgetsPath(FlowVirtualObject target) throws Exception {
		var componentId = frontendReferencedComponentId(target);
		if (componentId == null || componentId.isBlank()) {
			return null;
		}
		var model = new JSONObject(frontendSource(target));
		var components = model.optJSONArray("components");
		if (components == null) {
			return null;
		}
		for (int i = 0; i < components.length(); i++) {
			var component = components.optJSONObject(i);
			if (component != null && componentId.equals(component.optString("id", ""))) {
				return "components[" + i + "].widgets";
			}
		}
		return null;
	}

	private static String frontendReferencedComponentId(FlowVirtualObject target) {
		var definition = target.getDefinitionObject();
		if (definition == null) {
			return "";
		}
		return switch (target.getVirtualKind()) {
		case "frontendComponentRef" -> definition.optString("component", "");
		case "frontendRegion" -> firstString(definition.optJSONArray("components"));
		case "frontendPage" -> firstPageComponent(definition);
		default -> "";
		};
	}

	private static String firstPageComponent(JSONObject page) {
		var regions = page.optJSONObject("regions");
		if (regions != null) {
			var content = firstString(regions.optJSONArray("content"));
			if (!content.isBlank()) {
				return content;
			}
			var keys = regions.keys();
			while (keys.hasNext()) {
				var value = regions.opt(String.valueOf(keys.next()));
				if (value instanceof JSONArray array) {
					var first = firstString(array);
					if (!first.isBlank()) {
						return first;
					}
				}
			}
		}
		return firstString(page.optJSONArray("components"));
	}

	private static String firstString(JSONArray array) {
		return array == null || array.length() == 0 ? "" : array.optString(0, "");
	}

	private static JSONObject uniqueFrontendInsertValue(FlowVirtualObject target, JSONObject insert) throws Exception {
		var value = new JSONObject(insert.toString());
		var base = value.optString("id", value.optString("kind", "widget"));
		base = base.replaceAll("[^A-Za-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
		if (base.isBlank()) {
			base = "widget";
		}
		var used = new HashSet<String>();
		var component = "frontendComponent".equals(target.getVirtualKind()) ? target : target.getParent();
		if (component instanceof FlowVirtualObject componentFvo) {
			for (var child : componentFvo.getDatabaseObjectChildren()) {
				if (child instanceof FlowVirtualObject childFvo && "frontendWidget".equals(childFvo.getVirtualKind())) {
					var childDefinition = childFvo.getDefinitionObject();
					if (childDefinition != null) {
						var id = childDefinition.optString("id", "");
						if (!id.isBlank()) {
							used.add(id);
						}
					}
				}
			}
		}
		var referencedComponent = frontendReferencedComponent(target);
		if (referencedComponent != null && referencedComponent != component) {
			for (var child : referencedComponent.getDatabaseObjectChildren()) {
				if (child instanceof FlowVirtualObject childFvo && "frontendWidget".equals(childFvo.getVirtualKind())) {
					var childDefinition = childFvo.getDefinitionObject();
					if (childDefinition != null) {
						var id = childDefinition.optString("id", "");
						if (!id.isBlank()) {
							used.add(id);
						}
					}
				}
			}
		}
		used.addAll(frontendReferencedComponentWidgetIds(target));
		var candidate = base;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = base + i;
		}
		value.put("id", candidate);
		return value;
	}

	private static Set<String> frontendReferencedComponentWidgetIds(FlowVirtualObject target) {
		var used = new HashSet<String>();
		try {
			if (!sourcePath(target).endsWith(".front.json")) {
				return used;
			}
			var componentId = frontendReferencedComponentId(target);
			if (componentId == null || componentId.isBlank()) {
				return used;
			}
			var model = new JSONObject(frontendSource(target));
			var components = model.optJSONArray("components");
			if (components == null) {
				return used;
			}
			for (int i = 0; i < components.length(); i++) {
				var component = components.optJSONObject(i);
				if (component == null || !componentId.equals(component.optString("id", ""))) {
					continue;
				}
				var widgets = component.optJSONArray("widgets");
				if (widgets == null) {
					return used;
				}
				for (int j = 0; j < widgets.length(); j++) {
					var widget = widgets.optJSONObject(j);
					var id = widget == null ? "" : widget.optString("id", "");
					if (!id.isBlank()) {
						used.add(id);
					}
				}
				return used;
			}
		} catch (Exception e) {
			Engine.logStudio.debug("Unable to collect frontend widget ids", e);
		}
		return used;
	}

	private static boolean isSuccessResponse(JSONObject response) {
		return response != null && !response.has("error") && (!response.has("ok") || response.optBoolean("ok", false));
	}

	private static String frontendSource(FlowVirtualObject target) throws Exception {
		var sourcePath = sourcePath(target);
		var root = flowAuthoringRoot(target);
		if (root instanceof FlowEngine flowEngine) {
			return flowEngine.getFrontendSource(sourcePath);
		}
		return FileUtils.readFileToString(new File(sourcePath), "UTF-8");
	}

	public static JSONObject moveNode(DatabaseObject targetDbo, boolean up, int count) throws Exception {
		if (!(targetDbo instanceof FlowVirtualObject fvo) || !"node".equals(fvo.getVirtualKind())) {
			return new JSONObject().put("done", false);
		}
		var currentIndex = arrayIndex(mutationPath(fvo));
		var targetIndex = currentIndex + (up ? -Math.max(1, count) : Math.max(1, count));
		return moveNodeToIndex(fvo, targetIndex);
	}

	public static JSONObject moveNode(DatabaseObject sourceDbo, DatabaseObject targetDbo, boolean insertBefore) throws Exception {
		if (!(sourceDbo instanceof FlowVirtualObject source) || !(targetDbo instanceof FlowVirtualObject target)
				|| !canMoveNode(sourceDbo, targetDbo)) {
			return new JSONObject().put("done", false);
		}
		var sourceArrayPath = parentArrayPath(mutationPath(source));
		var targetArrayPath = parentArrayPath(mutationPath(target));
		var sourceIndex = arrayIndex(mutationPath(source));
		var targetIndex = arrayIndex(mutationPath(target));
		if (sourceIndex == targetIndex) {
			return new JSONObject().put("done", false);
		}
		var insertIndex = targetIndex + (insertBefore ? 0 : 1);
		if (sourceIndex < targetIndex) {
			insertIndex--;
		}
		return moveNodeToIndex(source, insertIndex);
	}

	public static boolean canMoveNode(DatabaseObject sourceDbo, DatabaseObject targetDbo) {
		if (!(sourceDbo instanceof FlowVirtualObject source) || !(targetDbo instanceof FlowVirtualObject target)
				|| !sameVirtualParent(source, target)
				|| !source.getVirtualKind().equals(target.getVirtualKind())) {
			return false;
		}
		var sourceArrayPath = parentArrayPath(mutationPath(source));
		var targetArrayPath = parentArrayPath(mutationPath(target));
		if (sourceArrayPath == null || !sourceArrayPath.equals(targetArrayPath)) {
			return false;
		}
		if ("node".equals(source.getVirtualKind()) && !isSourceBackedTarget(source) && !isSourceBackedTarget(target)) {
			return true;
		}
		return isSourceBackedTarget(source)
				&& isSourceBackedTarget(target)
				&& sourcePath(source).equals(sourcePath(target));
	}

	private static boolean sameVirtualParent(FlowVirtualObject source, FlowVirtualObject target) {
		var sourceParent = source.getParent();
		var targetParent = target.getParent();
		if (sourceParent == targetParent) {
			return true;
		}
		if (!(sourceParent instanceof FlowVirtualObject sourceFvo)
				|| !(targetParent instanceof FlowVirtualObject targetFvo)
				|| !sourceFvo.getVirtualKind().equals(targetFvo.getVirtualKind())
				|| !sourceFvo.getVirtualPath().equals(targetFvo.getVirtualPath())) {
			return false;
		}
		var sourceParentPath = sourcePath(sourceFvo);
		var targetParentPath = sourcePath(targetFvo);
		return sourceParentPath.isBlank()
				? targetParentPath.isBlank()
				: sourceParentPath.equals(targetParentPath);
	}

	private static JSONObject moveNodeToIndex(FlowVirtualObject fvo, int targetIndex) throws Exception {
		var root = flowAuthoringRoot(fvo);
		var path = mutationPath(fvo);
		var arrayPath = parentArrayPath(path);
		if (root == null || arrayPath == null) {
			return new JSONObject().put("done", false);
		}
		var parent = fvo.getParent();
		var siblingCount = parent == null ? 0 : parent.getDatabaseObjectChildren().size();
		var currentIndex = arrayIndex(path);
		targetIndex = Math.max(0, Math.min(siblingCount - 1, targetIndex));
		if (targetIndex == currentIndex) {
			return new JSONObject().put("done", false);
		}

		var response = applyMutation(root, fvo, new JSONObject()
				.put("op", "move")
				.put("from", path)
				.put("path", arrayPath)
				.put("index", targetIndex));
		var done = response.optBoolean("ok", false);
		return new JSONObject()
				.put("done", done)
				.put("id", done ? root.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
	}

	private static JSONObject paletteItem(JSONObject block) throws Exception {
		var blockId = firstNonBlank(block, "blockId", "name");
		var name = firstNonBlank(block, "localName", "name");
		var shortDescription = firstNonBlank(block, "shortDescription", "description");
		var longDescription = firstNonBlank(block, "longDescription");
		var propertiesDescription = propertiesDescription(block.optJSONObject("props"));
		var rawDescription = shortDescription;
		if (!longDescription.isBlank()) {
			rawDescription += "|" + longDescription;
		}
		var item = new JSONObject()
				.put("type", FLOW_BLOCK_TYPE)
				.put("id", FLOW_BLOCK_ID_PREFIX + blockId)
				.put("name", name)
				.put("classname", blockId)
				.put("block", blockId)
				.put("description", rawDescription)
				.put("shortDescriptionHtml", html(shortDescription))
				.put("shortDescriptionText", shortDescription)
				.put("longDescriptionHtml", html(longDescription))
				.put("longDescriptionText", longDescription)
				.put("propertiesDescriptionHtml", propertiesDescription)
				.put("icon", studioIcon(block))
				.put("tooltip", blockId)
				.put("builtin", "core".equals(block.optString("origin", "")))
				.put("additional", "project".equals(block.optString("origin", "")));
		var iconify = firstNonBlank(block, "iconify");
		if (!iconify.isBlank()) {
			item.put("iconify", iconify);
		}
		return item;
	}

	private static JSONObject blockDefinitionCategory(DatabaseObject targetDbo) throws Exception {
		return new JSONObject()
				.put("type", "Category")
				.put("name", "Flow block definitions")
				.put("items", new JSONArray()
						.put(blockDefinitionPaletteItem(targetDbo, "flow", "Flow block",
								"Composite block implemented with Flow nodes.", FLOW_ICON, "mdi:source-branch"))
						.put(blockDefinitionPaletteItem(targetDbo, "rhino", "JavaScript block",
								"Native block implemented with JavaScript on the Rhino runtime.", FLOW_SCRIPT_ICON, "mdi:language-javascript")));
	}

	private static JSONObject blockDefinitionPaletteItem(DatabaseObject targetDbo, String runtime, String name, String description, String iconPath, String iconify) throws Exception {
		var icon = cachedIconPath(targetDbo, iconify, iconPath);
		return new JSONObject()
				.put("type", FLOW_BLOCK_DEFINITION_TYPE)
				.put("id", FLOW_BLOCK_DEFINITION_ID_PREFIX + runtime)
				.put("name", name)
				.put("classname", name)
				.put("runtime", runtime)
				.put("description", description)
				.put("shortDescriptionHtml", html(description))
				.put("shortDescriptionText", description)
				.put("longDescriptionHtml", "")
				.put("longDescriptionText", "")
				.put("propertiesDescriptionHtml", "")
				.put("icon", icon)
				.put("iconFile32", icon)
				.put("iconify", iconify)
				.put("builtin", false)
				.put("additional", true);
	}

	private static JSONObject typeDefinitionCategory(DatabaseObject targetDbo) throws Exception {
		var icon = cachedIconPath(targetDbo, "mdi:form-textbox", FLOW_VIRTUAL_ICON);
		return new JSONObject()
				.put("type", "Category")
				.put("name", "Flow type definitions")
				.put("items", new JSONArray()
						.put(new JSONObject()
								.put("type", FLOW_TYPE_DEFINITION_TYPE)
								.put("id", FLOW_TYPE_DEFINITION_ID)
								.put("name", "Property type")
								.put("classname", "Property type")
								.put("description", "Project-local property type with an optional web editor.")
								.put("shortDescriptionHtml", html("Project-local property type with an optional web editor."))
								.put("shortDescriptionText", "Project-local property type with an optional web editor.")
								.put("longDescriptionHtml", "")
								.put("longDescriptionText", "")
								.put("propertiesDescriptionHtml", "")
								.put("icon", icon)
								.put("iconFile32", icon)
								.put("iconify", "mdi:form-textbox")
								.put("builtin", false)
								.put("additional", true)));
	}

	private static JSONObject propertyDefinitionCategory(DatabaseObject targetDbo) throws Exception {
		var icon = cachedIconPath(targetDbo, "mdi:form-textbox", FLOW_VIRTUAL_ICON);
		return new JSONObject()
				.put("type", "Category")
				.put("name", "Flow block properties")
				.put("items", new JSONArray()
						.put(new JSONObject()
								.put("type", FLOW_PROPERTY_DEFINITION_TYPE)
								.put("id", FLOW_PROPERTY_DEFINITION_ID)
								.put("name", "Block property")
								.put("classname", "Block property")
								.put("description", "Property exposed as block input.")
								.put("shortDescriptionHtml", html("Property exposed as block input."))
								.put("shortDescriptionText", "Property exposed as block input.")
								.put("longDescriptionHtml", "")
								.put("longDescriptionText", "")
								.put("propertiesDescriptionHtml", "")
								.put("icon", icon)
								.put("iconFile32", icon)
								.put("iconify", "mdi:form-textbox")
								.put("builtin", false)
								.put("additional", true)));
	}

	private static JSONObject helperDefinitionCategory(DatabaseObject targetDbo) throws Exception {
		var icon = cachedIconPath(targetDbo, "mdi:function-variant", FLOW_VIRTUAL_ICON);
		return new JSONObject()
				.put("type", "Category")
				.put("name", "Flow helper definitions")
				.put("items", new JSONArray()
						.put(new JSONObject()
								.put("type", FLOW_HELPER_DEFINITION_TYPE)
								.put("id", FLOW_HELPER_DEFINITION_ID)
								.put("name", "Helper function")
								.put("classname", "Helper function")
								.put("description", "Flow-local helper callable from this Flow.")
								.put("shortDescriptionHtml", html("Flow-local helper callable from this Flow."))
								.put("shortDescriptionText", "Flow-local helper callable from this Flow.")
								.put("longDescriptionHtml", "")
								.put("longDescriptionText", "")
								.put("propertiesDescriptionHtml", "")
								.put("icon", icon)
								.put("iconFile32", icon)
								.put("iconify", "mdi:function-variant")
								.put("builtin", false)
								.put("additional", true)));
	}

	private static String cachedIconPath(DatabaseObject targetDbo, String iconify, String fallback) {
		if (!isIconifyIcon(iconify)) {
			return fallback;
		}
		var root = flowAuthoringRoot(targetDbo);
		var engineQName = effectiveEngineQName(root);
		var dot = engineQName.indexOf('.');
		var projectName = dot == -1 ? engineQName : engineQName.substring(0, dot);
		var colon = iconify.indexOf(':');
		var provider = iconify.substring(0, colon);
		var name = iconify.substring(colon + 1);
		try {
			var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			var base = new File(project.getDirPath(), "libs/flow/icons/iconify/" + provider + "/" + name);
			for (var suffix : new String[] { "_32x32.png", "_16x16.png", ".png" }) {
				var file = new File(base.getAbsolutePath() + suffix);
				if (file.isFile()) {
					return file.getAbsolutePath();
				}
			}
		} catch (Exception e) {
		}
		return fallback;
	}

	private static String studioIcon(JSONObject object) {
		var iconFile = firstNonBlank(object, "iconFile32", "iconFile16", "iconFile");
		if (!iconFile.isBlank()) {
			return iconFile;
		}
		var icon = firstNonBlank(object, "icon");
		return icon.isBlank() || isIconifyIcon(icon) ? FLOW_ICON : icon;
	}

	private static boolean isIconifyIcon(String icon) {
		return icon != null && icon.matches("[A-Za-z][A-Za-z0-9_-]*:[A-Za-z0-9_.-]+");
	}

	private static String firstNonBlank(JSONObject object, String... keys) {
		if (object == null) {
			return "";
		}
		for (var key : keys) {
			var value = object.optString(key, "");
			if (!value.isBlank()) {
				return value;
			}
		}
		return "";
	}

	private static String propertiesDescription(JSONObject props) {
		if (props == null || props.length() == 0) {
			return "";
		}
		var list = new StringBuilder();
		for (var keys = props.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			var property = props.optJSONObject(key);
			if (property == null || property.optBoolean("hidden", false)) {
				continue;
			}
			var label = firstNonBlank(property, "label");
			if (label.isBlank()) {
				label = key;
			}
			var description = firstNonBlank(property, "shortDescription", "description", "longDescription");
			if (description.isBlank()) {
				description = "Flow property \"" + key + "\".";
			}
			list.append("<li><i>")
					.append(html(label))
					.append("</i></br>")
					.append(html(description))
					.append("</li>");
		}
		return list.length() == 0 ? "" : "<ul>" + list + "</ul>";
	}

	private static String html(String text) {
		return StringEscapeUtils.escapeHtml4(text == null ? "" : text);
	}

	private static void addDefaultProperties(DatabaseObject root, String blockName, JSONObject node) throws Exception {
		var block = blockDescriptor(root, blockName);
		if (block == null) {
			return;
		}
		copyDefaults(block.optJSONObject("defaults"), node);
		var props = block.optJSONObject("props");
		if (props == null) {
			return;
		}
		for (var keys = props.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			var descriptor = props.optJSONObject(key);
			if (descriptor != null && descriptor.has("default") && !node.has(key)) {
				node.put(key, descriptor.opt("default"));
			}
		}
	}

	private static void copyDefaults(JSONObject defaults, JSONObject node) throws Exception {
		if (defaults == null) {
			return;
		}
		for (var keys = defaults.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			if (!node.has(key)) {
				node.put(key, defaults.opt(key));
			}
		}
	}

	private static DatabaseObject flowAuthoringRoot(DatabaseObject dbo) {
		var current = dbo;
		while (current != null) {
			if (current instanceof Flow || current instanceof FlowEngine) {
				return current;
			}
			try {
				current = current.getParent();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}

	private static boolean isWritablePaletteTarget(DatabaseObject targetDbo) {
		if (targetDbo instanceof Flow) {
			return true;
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			var kind = fvo.getVirtualKind();
			if (isSourceBackedTarget(fvo)) {
				return "blockImplementation".equals(kind)
						|| "fragmentImplementation".equals(kind)
						|| "folder".equals(kind)
						|| "slot".equals(kind)
						|| "node".equals(kind);
			}
			if ("folder".equals(kind)) {
				return "flow".equals(fvo.getVirtualType());
			}
			return "slot".equals(kind) || "node".equals(kind);
		}
		return false;
	}

	private static boolean isFrontendPaletteTarget(DatabaseObject targetDbo) {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof FlowEngine)) {
			return false;
		}
		if (targetDbo instanceof FlowEngine) {
			return true;
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			var path = fvo.getVirtualPath();
			return "frontends".equals(path) || path.startsWith("frontends.");
		}
		return false;
	}

	private static String frontendBuilderName(DatabaseObject targetDbo) {
		if (targetDbo instanceof FlowVirtualObject fvo) {
			var path = fvo.getVirtualPath();
			var parts = path.split("\\.");
			if (parts.length >= 2 && "frontends".equals(parts[0]) && !parts[1].isBlank()) {
				return parts[1];
			}
			var info = fvo.getVirtualInfoObject();
			if (info != null) {
				var builder = info.optString("frontendBuilder", "");
				if (!builder.isBlank()) {
					return builder;
				}
			}
		}
		return "svelte";
	}

	private static boolean isBlockDefinitionPaletteTarget(DatabaseObject targetDbo) {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof FlowEngine)) {
			return false;
		}
		if (targetDbo instanceof FlowEngine) {
			return true;
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			var path = fvo.getVirtualPath();
			return "catalog".equals(path)
					|| "catalog.blocks".equals(path)
					|| "catalog.blocks.project".equals(path);
		}
		return false;
	}

	private static boolean isTypeDefinitionPaletteTarget(DatabaseObject targetDbo) {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof FlowEngine)) {
			return false;
		}
		if (targetDbo instanceof FlowEngine) {
			return true;
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			var path = fvo.getVirtualPath();
			return "catalog".equals(path) || "catalog.types".equals(path);
		}
		return false;
	}

	private static boolean isPropertyDefinitionPaletteTarget(DatabaseObject targetDbo) {
		return propertyDefinitionTarget(targetDbo) != null;
	}

	private static boolean isHelperDefinitionPaletteTarget(DatabaseObject targetDbo) {
		return helperDefinitionTarget(targetDbo) != null;
	}

	private static FlowVirtualObject propertyDefinitionTarget(DatabaseObject targetDbo) {
		if (!(flowAuthoringRoot(targetDbo) instanceof FlowEngine)) {
			return null;
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			if ("blockProperty".equals(fvo.getVirtualKind())) {
				var parent = fvo.getParent();
				return parent instanceof FlowVirtualObject parentFvo ? propertyDefinitionTarget(parentFvo) : null;
			}
			if ("folder".equals(fvo.getVirtualKind()) && "blockProperties".equals(fvo.getVirtualType()) && isSourceBackedTarget(fvo)) {
				return fvo;
			}
		}
		return null;
	}

	private static DatabaseObject helperDefinitionTarget(DatabaseObject targetDbo) {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof Flow)) {
			return null;
		}
		if (targetDbo instanceof Flow) {
			return targetDbo;
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			if ("helper".equals(fvo.getVirtualKind())) {
				var parent = fvo.getParent();
				return parent instanceof FlowVirtualObject parentFvo ? helperDefinitionTarget(parentFvo) : null;
			}
			if ("folder".equals(fvo.getVirtualKind()) && "helpers".equals(fvo.getVirtualType())) {
				return fvo;
			}
		}
		return null;
	}

	private static String effectiveEngineQName(DatabaseObject root) {
		if (root instanceof FlowEngine flowEngine) {
			var engineQName = flowEngine.getEngineQName();
			return engineQName == null || engineQName.isBlank() ? FlowEngineBridge.DEFAULT_ENGINE_QNAME : engineQName;
		}
		if (root != null && root.getProject() != null && root.getProject().getFlowEngine() != null) {
			var engineQName = root.getProject().getFlowEngine().getEngineQName();
			return engineQName == null || engineQName.isBlank() ? FlowEngineBridge.DEFAULT_ENGINE_QNAME : engineQName;
		}
		return FlowEngineBridge.DEFAULT_ENGINE_QNAME;
	}

	private static JSONObject insertionFor(DatabaseObject root, DatabaseObject targetDbo, String position, String blockName, JSONObject node) throws Exception {
		position = position == null || position.isBlank() ? "inside" : position;
		if (targetDbo instanceof Flow) {
			return mutationForArray("nodes", position.equals("first") ? 0 : -1, node);
		}
		if (!(targetDbo instanceof FlowVirtualObject fvo)) {
			return null;
		}

		var path = mutationPath(fvo);
		if (path.isBlank() || !isSourceBackedTarget(fvo) && path.startsWith("catalog")) {
			return null;
		}
		if ("inside".equals(position)) {
			if ("blockImplementation".equals(fvo.getVirtualKind()) || "folder".equals(fvo.getVirtualKind()) || "slot".equals(fvo.getVirtualKind())) {
				return mutationForArray(path, -1, node);
			}
			if ("node".equals(fvo.getVirtualKind())) {
				return mutationForNodeSlot(root, fvo, blockName, node);
			}
			return null;
		}
		var arrayPath = parentArrayPath(path);
		if (arrayPath == null) {
			return null;
		}
		var index = switch (position) {
		case "first" -> 0;
		case "before" -> arrayIndex(path);
		default -> arrayIndex(path) + 1;
		};
		return mutationForArray(arrayPath, index, node);
	}

	private static JSONObject mutationForNodeSlot(DatabaseObject root, FlowVirtualObject fvo, String blockName, JSONObject node) throws Exception {
		var slot = firstSlotName(root, fvo.getVirtualType());
		if (slot == null || slot.isBlank()) {
			return null;
		}
		var slotPath = mutationPath(fvo) + "." + slot;
		var definition = fvo.getDefinitionObject();
		var current = definition == null ? null : definition.optJSONArray(slot);
		if (current == null) {
			return new JSONObject()
					.put("op", "replace")
					.put("path", slotPath)
					.put("value", new JSONArray().put(node));
		}
		return mutationForArray(slotPath, -1, node);
	}

	private static String firstSlotName(DatabaseObject root, String blockName) throws Exception {
		var block = blockDescriptor(root, blockName);
		if (block != null) {
			var slots = block.optJSONArray("slots");
			if (slots == null) {
				slots = block.optJSONArray("children");
			}
			if (slots != null && slots.length() > 0) {
				var slot = slots.opt(0);
				if (slot instanceof JSONObject json) {
					return json.optString("name", "nodes");
				}
				return String.valueOf(slot);
			}
		}
		return null;
	}

	private static JSONObject blockDescriptor(DatabaseObject root, String blockName) throws Exception {
		var blocks = catalog(root).optJSONArray("blocks");
		if (blocks != null) {
			for (int i = 0; i < blocks.length(); i++) {
				var block = blocks.optJSONObject(i);
				if (block != null && blockName.equals(firstNonBlank(block, "blockId", "name"))) {
					return block;
				}
			}
		}
		return null;
	}

	private static JSONObject catalog(DatabaseObject root) throws Exception {
		var key = paletteKey(root);
		var cached = catalogCache.get(key);
		if (cached != null) {
			return cached;
		}
		var catalog = root instanceof Flow flow
				? new FlowEngineBridge().catalog(flow, false, false)
				: new FlowEngineBridge().catalog((FlowEngine) root, false, false);
		catalogCache.put(key, catalog);
		return catalog;
	}

	private static JSONObject applyMutation(DatabaseObject root, DatabaseObject targetDbo, JSONObject mutation) throws Exception {
		if (targetDbo instanceof FlowVirtualObject fvo && isSourceBackedTarget(fvo)) {
			var flowEngine = root instanceof FlowEngine engine ? engine : root.getProject().getFlowEngine();
			var sourcePath = sourcePath(fvo);
			var response = new FlowEngineBridge().applySourceMutation(flowEngine, sourcePath, mutation);
			if (response.optBoolean("ok", false)) {
				afterSourceMutation(flowEngine, sourcePath);
			}
			return response;
		}
		return root instanceof Flow flow
				? new FlowEngineBridge().applyMutation(flow, mutation)
				: new FlowEngineBridge().applyMutation((FlowEngine) root, mutation);
	}

	private static JSONObject applyFrontendMutation(FlowEngine flowEngine, DatabaseObject targetDbo, JSONObject mutation) throws Exception {
		var overrideSourcePath = mutation == null ? "" : mutation.optString("__sourcePath", "");
		if (!overrideSourcePath.isBlank()) {
			var response = new FlowEngineBridge().applySourceMutation(flowEngine, overrideSourcePath, cleanFrontendMutation(mutation));
			if (response.optBoolean("ok", false)) {
				afterSourceMutation(flowEngine, overrideSourcePath);
			}
			return response;
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			var sourcePath = sourcePath(fvo);
			if (!sourcePath.isBlank() && sourceFlag(fvo, "sourceWritable")) {
				var response = new FlowEngineBridge().applySourceMutation(flowEngine, sourcePath, mutation);
				if (response.optBoolean("ok", false)) {
					afterSourceMutation(flowEngine, sourcePath);
				}
				return response;
			}
		}
		return applyMutation(flowEngine, targetDbo, mutation);
	}

	private static JSONObject cleanFrontendMutation(JSONObject mutation) throws Exception {
		var clean = new JSONObject(mutation.toString());
		clean.remove("__sourcePath");
		return clean;
	}

	public static void afterSourceMutation(FlowEngine flowEngine, String sourcePath) {
		if (flowEngine == null || sourcePath == null
				|| !(sourcePath.endsWith(".front.json") || sourcePath.endsWith(".flow.svelte"))) {
			return;
		}
		var key = flowEngine.getQName() + "|" + sourcePath;
		var previous = frontendGenerationTasks.get(key);
		if (previous != null) {
			previous.cancel(false);
		}
		var task = frontendGenerationExecutor.schedule(() -> {
			try {
				var response = new FlowEngineBridge().contextAction(flowEngine, new JSONObject()
						.put("action", new JSONObject()
								.put("id", "frontbuilder.svelte.generate")
								.put("payload", new JSONObject()
										.put("changedSourcePath", sourcePath))));
				if (response.optBoolean("ok", false)) {
					flowStudioInfo("Regenerated frontend source after Flow mutation: " + sourcePath);
				} else {
					flowStudioWarn("Unable to regenerate frontend source after Flow mutation: " + flowErrorMessage(response));
				}
			} catch (Exception e) {
				flowStudioWarn("Unable to regenerate frontend source after Flow mutation.", e);
			} finally {
				frontendGenerationTasks.remove(key);
			}
		}, 350, TimeUnit.MILLISECONDS);
		frontendGenerationTasks.put(key, task);
	}

	private static void flowStudioInfo(String message) {
		var log = Engine.logStudio == null ? Engine.logBeans : Engine.logStudio;
		log.info(message);
	}

	private static void flowStudioWarn(String message) {
		var log = Engine.logStudio == null ? Engine.logBeans : Engine.logStudio;
		log.warn(message);
	}

	private static void flowStudioWarn(String message, Throwable e) {
		var log = Engine.logStudio == null ? Engine.logBeans : Engine.logStudio;
		log.warn(message, e);
	}

	private static String flowErrorMessage(JSONObject response) {
		var error = response == null ? null : response.optJSONObject("error");
		if (error == null) {
			return response == null ? "" : response.optString("message", response.optString("code", "Unknown error"));
		}
		var message = error.optString("message", "");
		return message.isBlank() ? error.optString("code", "Unknown error") : message;
	}

	private static boolean isSourceBackedTarget(FlowVirtualObject fvo) {
		var sourcePath = sourcePath(fvo);
		var sourceMutationPath = sourceMutationPath(fvo);
		return !sourcePath.isBlank() && !sourceMutationPath.isBlank() && sourceFlag(fvo, "sourceWritable");
	}

	private static String mutationPath(FlowVirtualObject fvo) {
		var sourceMutationPath = sourceMutationPath(fvo);
		return sourceMutationPath.isBlank() ? fvo.getVirtualPath() : sourceMutationPath;
	}

	private static String sourcePath(FlowVirtualObject fvo) {
		return sourceValue(fvo, "sourcePath");
	}

	private static String sourceMutationPath(FlowVirtualObject fvo) {
		return sourceValue(fvo, "sourceMutationPath");
	}

	private static boolean sourceFlag(FlowVirtualObject fvo, String key) {
		var value = sourceFlagValue(fvo, key);
		return value != null && value;
	}

	private static Boolean sourceFlagValue(FlowVirtualObject fvo, String key) {
		var info = fvo.getVirtualInfoObject();
		var definition = fvo.getDefinitionObject();
		if (info != null && info.has(key)) {
			return info.optBoolean(key, false);
		}
		if (definition != null && definition.has(key)) {
			return definition.optBoolean(key, false);
		}
		return null;
	}

	private static String sourceValue(FlowVirtualObject fvo, String key) {
		var info = fvo.getVirtualInfoObject();
		var value = info == null ? "" : info.optString(key, "");
		if (!value.isBlank()) {
			return value;
		}
		var definition = fvo.getDefinitionObject();
		return definition == null ? "" : definition.optString(key, "");
	}

	private static JSONObject mutationForArray(String path, int index, JSONObject node) throws Exception {
		var mutation = new JSONObject()
				.put("path", path)
				.put("value", node);
		if (index < 0) {
			return mutation.put("op", "append");
		}
		return mutation.put("op", "insert").put("index", index);
	}

	private static String parentArrayPath(String path) {
		var index = path.lastIndexOf('[');
		return index <= 0 ? null : path.substring(0, index);
	}

	private static int arrayIndex(String path) {
		var start = path.lastIndexOf('[');
		var end = path.lastIndexOf(']');
		if (start < 0 || end <= start) {
			return 0;
		}
		try {
			return Integer.parseInt(path.substring(start + 1, end));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String nextNodeId(DatabaseObject root, String blockName) {
		var base = blockName.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+", "_");
		base = base.replaceAll("^_+|_+$", "");
		if (base.isBlank()) {
			base = "node";
		}
		var used = new HashSet<String>();
		try {
			collectNodeIds(root.getDatabaseObjectChildren(), used);
		} catch (Exception e) {
		}
		var candidate = base;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = base + i;
		}
		return candidate;
	}

	private static String nextBlockName(DatabaseObject root, String runtime) throws Exception {
		var base = "project." + ("rhino".equals(runtime) ? "rhinoBlock" : "flowBlock");
		var used = new HashSet<String>();
		var blocks = catalog(root).optJSONArray("blocks");
		if (blocks != null) {
			for (int i = 0; i < blocks.length(); i++) {
				var block = blocks.optJSONObject(i);
				if (block != null) {
					var name = firstNonBlank(block, "blockId", "name");
					if (!name.isBlank()) {
						used.add(name);
					}
				}
			}
		}
		var candidate = base;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = base + i;
		}
		return candidate;
	}

	private static String frontendComponentTag(String value) {
		var builder = new StringBuilder();
		for (var part : value.split("[^A-Za-z0-9]+")) {
			if (part.isBlank()) {
				continue;
			}
			builder.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				builder.append(part.substring(1));
			}
		}
		var tag = builder.toString();
		return tag.isBlank() ? "Component" : tag;
	}

	private static String lowerFirst(String value) {
		return value == null || value.isBlank()
				? "component"
				: Character.toLowerCase(value.charAt(0)) + value.substring(1);
	}

	private static String safeFileName(String value) {
		var safe = value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]+", "_").replaceAll("_+", "_");
		safe = safe.replaceAll("^_+|_+$", "");
		return safe.isBlank() ? "project" : safe;
	}

	private static String nextTypeName(DatabaseObject root) throws Exception {
		var base = "project.customType";
		var used = new HashSet<String>();
		var types = catalog(root).optJSONArray("types");
		if (types != null) {
			for (int i = 0; i < types.length(); i++) {
				var type = types.optJSONObject(i);
				if (type != null) {
					var name = type.optString("name", "");
					if (!name.isBlank()) {
						used.add(name);
					}
				}
			}
		}
		var candidate = base;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = base + i;
		}
		return candidate;
	}

	private static String nextPropertyName(FlowVirtualObject target) {
		var used = new HashSet<String>();
		try {
			for (var child : target.getDatabaseObjectChildren()) {
				if (child instanceof FlowVirtualObject fvo && "blockProperty".equals(fvo.getVirtualKind())) {
					used.add(fvo.getVirtualType());
				}
			}
		} catch (Exception e) {
		}
		var base = "property";
		var candidate = base;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = base + i;
		}
		return candidate;
	}

	private static String nextHelperName(DatabaseObject root) {
		var used = new HashSet<String>();
		try {
			collectHelperNames(root.getDatabaseObjectChildren(), used);
		} catch (Exception e) {
		}
		var base = "helper";
		var candidate = base;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = base + i;
		}
		return candidate;
	}

	private static void collectHelperNames(Iterable<DatabaseObject> children, Set<String> used) {
		for (var child : children) {
			if (child instanceof FlowVirtualObject fvo && "helper".equals(fvo.getVirtualKind())) {
				var type = fvo.getVirtualType();
				if (type != null && !type.isBlank()) {
					used.add(type);
				}
			}
			try {
				collectHelperNames(child.getDatabaseObjectChildren(), used);
			} catch (Exception e) {
			}
		}
	}

	private static void collectNodeIds(Iterable<DatabaseObject> children, Set<String> used) {
		for (var child : children) {
			if (child instanceof FlowVirtualObject fvo) {
				var definition = fvo.getDefinitionObject();
				if ("node".equals(fvo.getVirtualKind()) && definition != null) {
					var id = definition.optString("id", "");
					if (!id.isBlank()) {
						used.add(id);
					}
				}
			}
			try {
				collectNodeIds(child.getDatabaseObjectChildren(), used);
			} catch (Exception e) {
			}
		}
	}
}
