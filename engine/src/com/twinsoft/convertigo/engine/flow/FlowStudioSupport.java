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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

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
	private static final int PALETTE_CATEGORIES_CACHE_LIMIT = 64;
	private static final Map<String, String> paletteCategoriesCache = Collections.synchronizedMap(
			new LinkedHashMap<String, String>(PALETTE_CATEGORIES_CACHE_LIMIT, 0.75f, true) {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
					return size() > PALETTE_CATEGORIES_CACHE_LIMIT;
				}
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

	public static JSONObject authoringReference(DatabaseObject databaseObject) {
		if (!(databaseObject instanceof FlowVirtualObject fvo)) {
			return null;
		}
		if (!isFrontendSourcePath(sourcePath(fvo))) {
			return null;
		}
		var sourceRelativePath = sourceRelativePath(fvo);
		var sourceMutationPath = sourceMutationPath(fvo);
		if (sourceRelativePath.isBlank() || sourceMutationPath.isBlank()) {
			return null;
		}
		var nodeId = fvo.getDefinitionProperty("id");
		try {
			var reference = new JSONObject()
					.put("nodeId", nodeId == null ? fvo.getName() : String.valueOf(nodeId))
					.put("sourceRelativePath", sourceRelativePath)
					.put("sourceMutationPath", sourceMutationPath);
			var project = fvo.getProject();
			return project == null ? reference : reference.put("sourceProject", project.getName());
		} catch (Exception e) {
			return null;
		}
	}

	public static DatabaseObject resolveAuthoringReference(String projectName, JSONObject reference) throws Exception {
		if (reference == null || projectName == null || projectName.isBlank()) {
			return null;
		}
		var sourceProject = reference.optString("sourceProject", projectName);
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(sourceProject, false);
		if (project == null || project.getFlowEngine() == null) {
			return null;
		}
		var sourceRelativePath = normalizeSourcePath(reference.optString("sourceRelativePath", ""));
		var sourceMutationPath = reference.optString("sourceMutationPath", "");
		if (sourceRelativePath.isBlank() || sourceMutationPath.isBlank()) {
			return null;
		}
		for (var child : project.getFlowEngine().getDatabaseObjectChildren()) {
			var resolved = resolveAuthoringReference(child, sourceRelativePath, sourceMutationPath);
			if (resolved != null) {
				return resolved;
			}
		}
		return null;
	}

	private static DatabaseObject resolveAuthoringReference(DatabaseObject candidate, String sourceRelativePath,
			String sourceMutationPath) {
		if (candidate instanceof FlowVirtualObject fvo
				&& sourceRelativePath.equals(sourceRelativePath(fvo))
				&& sourceMutationPath.equals(sourceMutationPath(fvo))) {
			return fvo;
		}
		try {
			for (var child : candidate.getDatabaseObjectChildren()) {
				var resolved = resolveAuthoringReference(child, sourceRelativePath, sourceMutationPath);
				if (resolved != null) {
					return resolved;
				}
			}
		} catch (Exception e) {
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
		if (root instanceof FlowEngine flowEngine) {
			flowEngine.clearFlowVirtualChildrenCache();
		}
		if (root instanceof Flow flow && flow.getProject() != null) {
			var flowEngine = flow.getProject().getFlowEngine();
			if (flowEngine != null) {
				flowEngine.clearFlowVirtualChildrenCache();
			}
			var flowPrefix = flow.getProject().getName() + "|" + effectiveEngineQName(flow) + "|" + flow.getFullQName() + "|";
			catalogCache.keySet().removeIf(key -> key.startsWith(flowPrefix));
			clearPaletteCategoriesCache(flowPrefix);
			return;
		}
		var key = paletteKey(dbo);
		if (key.isBlank()) {
			catalogCache.clear();
			paletteCategoriesCache.clear();
			return;
		}
		catalogCache.remove(key);
		clearPaletteCategoriesCache(key);
	}

	private static void clearPaletteCategoriesCache(String prefix) {
		synchronized (paletteCategoriesCache) {
			paletteCategoriesCache.keySet().removeIf(key -> key.startsWith(prefix));
		}
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
		var mutation = response.optJSONObject("mutation");
		if (response.optBoolean("ok", false) && mutation != null) {
			var mutationResult = applyMutation(root, targetDbo, mutation);
			response.put("mutationResult", mutationResult);
			if (!mutationResult.optBoolean("ok", false)) {
				response.put("ok", false)
						.put("refresh", false)
						.put("message", flowErrorMessage(mutationResult));
				var error = mutationResult.optJSONObject("error");
				if (error != null) {
					response.put("error", error);
				}
			} else {
				response.put("changed", sourceMutationChanged(mutationResult))
						.put("refresh", true);
			}
		}
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
			var targetSlot = data == null ? null : data.optJSONObject("targetSlot");
			if (targetSlot == null && !frontendTargetKindsMatch(data, targetDbo)) {
				return false;
			}
			if (!frontendAcceptedPositionMatch(data, position)) {
				return false;
			}
			var insert = frontendInsertValue(data);
			return frontendSourceCreationSpec(targetDbo, insert) != null
					|| frontendEngineMutationFor(targetDbo, insert) != null
					|| frontendMutationFor(targetDbo, position, insert, targetSlot) != null;
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
		var cacheKey = paletteCategoriesCacheKey(targetDbo);
		if (!cacheKey.isBlank()) {
			var cached = paletteCategoriesCache.get(cacheKey);
			if (cached != null) {
				return new JSONArray(cached);
			}
		}
		var categories = computePaletteCategories(targetDbo);
		if (!cacheKey.isBlank()) {
			paletteCategoriesCache.put(cacheKey, categories.toString());
		}
		return categories;
	}

	private static String paletteCategoriesCacheKey(DatabaseObject targetDbo) {
		var key = paletteKey(targetDbo);
		if (key.isBlank()) {
			return "";
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			return key + "|" + fvo.getVirtualKind() + "|" + fvo.getVirtualPath();
		}
		return key + "|" + targetDbo.getClass().getName();
	}

	private static JSONArray computePaletteCategories(DatabaseObject targetDbo) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		var categories = new JSONArray();
		if (root == null) {
			return categories;
		}
		if (targetDbo instanceof FlowEngine flowEngine) {
			if (isBlockDefinitionPaletteTarget(targetDbo)) {
				categories.put(blockDefinitionCategory(targetDbo));
			}
			if (isTypeDefinitionPaletteTarget(targetDbo)) {
				categories.put(typeDefinitionCategory(targetDbo));
			}
			if (isPropertyDefinitionPaletteTarget(targetDbo)) {
				categories.put(propertyDefinitionCategory(targetDbo));
			}
			if (!hasConfiguredFrontendBuilder(flowEngine)) {
				categories.put(frontendBuilderBootstrapCategory(targetDbo));
			}
			return categories;
		}
		if (isFrontendPaletteTarget(targetDbo)) {
			var frontendCategories = frontendBlockCategories(root, targetDbo);
			for (int i = 0; i < frontendCategories.length(); i++) {
				categories.put(frontendCategories.get(i));
			}
			return categories;
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

	private static boolean hasConfiguredFrontendBuilder(FlowEngine flowEngine) {
		var source = flowEngine.getEngineSource();
		var frontbuilderIndent = -1;
		for (var line : source.split("\\R")) {
			var comment = line.indexOf('#');
			var content = comment < 0 ? line : line.substring(0, comment);
			var trimmed = content.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			var indent = content.length() - content.stripLeading().length();
			if (frontbuilderIndent < 0) {
				if ("frontbuilder:".equals(trimmed)) {
					frontbuilderIndent = indent;
				} else if (trimmed.startsWith("frontbuilder:")) {
					var inlineValue = trimmed.substring("frontbuilder:".length()).trim();
					return !inlineValue.isEmpty() && !"{}".equals(inlineValue) && !"null".equals(inlineValue);
				}
				continue;
			}
			if (indent <= frontbuilderIndent) {
				return false;
			}
			if (trimmed.indexOf(':') > 0) {
				return true;
			}
		}
		return false;
	}

	private static JSONObject frontendBuilderBootstrapCategory(DatabaseObject targetDbo) throws Exception {
		var descriptor = new JSONObject()
				.put("id", "frontbuilder.svelte.builder")
				.put("label", "Svelte builder")
				.put("category", "Svelte / Builders")
				.put("descriptorKind", "create")
				.put("createAction", true)
				.put("icon", "mdi:application-braces-outline")
				.put("description", "Adds the Svelte frontend builder configuration to this Flow engine.")
				.put("targetKinds", new JSONArray().put("flowEngine").put("frontends"))
				.put("acceptedPositions", new JSONArray().put("inside"))
				.put("insert", new JSONObject()
						.put("__engineMutationPath", "config.frontbuilder.svelte")
						.put("__engineMutationOp", "merge")
						.put("target", "svelte5")
						.put("resourceRoot", "libs/flow/frontbuilder/svelte")
						.put("privateDir", "_private/svelte")
						.put("modelPath", "libs/flow/frontbuilder/svelte/model/SvelteFrontend/src/routes/+page.flow.svelte")
						.put("buildOutput", "DisplayObjects/mobile"));
		return new JSONObject()
				.put("type", "Category")
				.put("name", "Frontend create actions - Svelte / Builders")
				.put("items", new JSONArray().put(frontendPaletteItem(targetDbo, descriptor)));
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
		if (!(root instanceof FlowEngine flowEngine)) {
			return categories;
		}
		var palette = new FlowEngineBridge().authoringPalette(flowEngine, new JSONObject()
				.put("surface", "frontend")
				.put("focusPath", frontendFocusPath(targetDbo))
				.put("position", "inside")
				.put("detail", "compact")
				.put("applyFallback", true));
		if (!palette.optBoolean("ok", false)) {
			var error = palette.optJSONObject("error");
			if (targetDbo instanceof FlowEngine && error != null
					&& "AUTHORING_FOCUS_NOT_FOUND".equals(error.optString("code"))) {
				return categories;
			}
			throw new EngineException("Unable to compute frontend authoring palette: " + palette.opt("error"));
		}
		var focus = palette.optJSONObject("focus");
		var focusPath = focus == null ? "" : focus.optString("path", "");
		var focusSourcePath = focus == null ? "" : focus.optString("sourcePath", "");
		var items = palette.optJSONArray("items");
		if (items == null) {
			return categories;
		}
		for (int i = 0; i < items.length(); i++) {
			var block = items.optJSONObject(i);
			if (block == null) {
				continue;
			}
			if (!focusPath.isBlank()) {
				block = new JSONObject(block.toString()).put("targetPath", focusPath);
			}
			if (!focusSourcePath.isBlank()) {
				block.put("targetSourcePath", focusSourcePath);
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
			category.getJSONArray("items").put(frontendPaletteItem(targetDbo, block));
		}
		for (var category : grouped.values()) {
			if (category.optJSONArray("items") != null && category.getJSONArray("items").length() > 0) {
				categories.put(category);
			}
		}
		return categories;
	}

	private static String frontendFocusPath(DatabaseObject targetDbo) {
		if (targetDbo instanceof FlowEngine) {
			return "frontends";
		}
		if (!(targetDbo instanceof FlowVirtualObject fvo)) {
			return "";
		}
		var path = fvo.getVirtualPath();
		return "frontends".equals(path) ? "frontends." + frontendBuilderName(targetDbo) : path;
	}

	static String frontendBlockCategoryName(JSONObject block) {
		if (block.optBoolean("createAction", false) || "create".equals(firstNonBlank(block, "descriptorKind"))) {
			var category = firstNonBlank(block, "category");
			return category.isBlank() ? "Frontend setup" : category;
		}
		var provider = firstNonBlank(block, "provider");
		var category = firstNonBlank(block, "category");
		return category.isBlank() ? humanizeProviderName(provider) : category;
	}

	private static String humanizeProviderName(String provider) {
		if (provider == null || provider.isBlank()) {
			return "Components";
		}
		var label = provider
				.replaceFirst("(?i)^lib[_-]flow[_-]frontend[_-]", "")
				.replaceFirst("(?i)^lib[_-]flow[_-]", "")
				.replaceFirst("(?i)[_-]svelte$", "")
				.replace('_', ' ')
				.replace('-', ' ')
				.trim();
		if (label.isBlank()) {
			return "Components";
		}
		return Character.toUpperCase(label.charAt(0)) + label.substring(1);
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

	private static JSONObject frontendPaletteItem(DatabaseObject targetDbo, JSONObject block) throws Exception {
		var blockId = firstNonBlank(block, "id", "name");
		var label = firstNonBlank(block, "label", "name", "id");
		var description = firstNonBlank(block, "description");
		var icon = frontendPaletteIcon(targetDbo, block);
		var insert = block.optJSONObject("insert") == null ? new JSONObject() : new JSONObject(block.getJSONObject("insert").toString());
		var targetSourcePath = firstNonBlank(block, "targetSourcePath");
		if (!targetSourcePath.isBlank() && firstNonBlank(insert, "__frontendSourcePath").isBlank()) {
			insert.put("__frontendSourcePath", targetSourcePath);
		}
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
					.put("iconFile32", icon)
					.put("tooltip", blockId)
					.put("builtin", false)
					.put("additional", true)
					.put("canContainChildren", frontendBlockCanContainChildren(block))
					.put("insert", insert);
		if (block.optBoolean("createAction", false) || "create".equals(firstNonBlank(block, "descriptorKind"))) {
			item.put("createAction", true);
		}
		if (block.optJSONArray("targetKinds") != null) {
			item.put("targetKinds", block.getJSONArray("targetKinds"));
		}
		if (block.optJSONArray("acceptedPositions") != null) {
			item.put("acceptedPositions", block.getJSONArray("acceptedPositions"));
		}
		if (block.optJSONObject("targetSlot") != null) {
			item.put("targetSlot", block.getJSONObject("targetSlot"));
		}
		var targetPath = firstNonBlank(block, "targetPath");
		if (!targetPath.isBlank()) {
			item.put("targetPath", targetPath);
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

	/**
	 * Exposes the static container capability needed by Studio while an inserted
	 * frontend block is still only an optimistic tree node. This deliberately
	 * derives from the provider contract instead of component names.
	 */
	static boolean frontendBlockCanContainChildren(JSONObject block) {
		if (block == null) {
			return false;
		}
		var slots = block.optJSONObject("slots");
		if (slots != null && slots.length() > 0) {
			return true;
		}
		var traits = block.optJSONArray("traits");
		if (traits != null) {
			for (int i = 0; i < traits.length(); i++) {
				if ("ui.container".equals(traits.optString(i))) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean frontendBlockCanContainChildren(FlowVirtualObject block) {
		return block != null && ("frontendContainerBlock".equals(block.getVirtualKind())
				|| frontendBlockCanContainChildren(block.getDefinitionObject())
				|| frontendBlockCanContainChildren(block.getVirtualInfoObject()));
	}

	private static String frontendPaletteIcon(DatabaseObject targetDbo, JSONObject block) {
		var iconFile = firstNonBlank(block, "iconFile32", "iconFile16", "iconFile");
		if (!iconFile.isBlank()) {
			return iconFile;
		}
		var iconify = firstNonBlank(block, "iconify", "icon");
		return cachedIconPath(targetDbo, iconify, FLOW_VIRTUAL_ICON);
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
		return withProjectionMetadata(new JSONObject()
				.put("done", done)
				.put("id", done ? root.getFullQName() : "")
				.put("selectionId", done ? node.optString("id", "") : "")
				.put("error", done ? JSONObject.NULL : response.opt("error")), response);
	}

	private static JSONObject addBlockDefinition(DatabaseObject targetDbo, String runtime) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof FlowEngine flowEngine) || !canAddBlockDefinition(targetDbo, runtime)) {
			return new JSONObject().put("done", false);
		}
		var projectionRoot = engineProjectionRoot(flowEngine, "catalog");
		var blockName = nextBlockName(flowEngine, runtime);
		var response = new FlowEngineBridge().createBlock(flowEngine, blockName, runtime);
		var done = isSuccessResponse(response);
		var result = new JSONObject()
				.put("done", done)
				.put("id", done ? flowEngine.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
		return done ? refreshEngineProjection(flowEngine, projectionRoot, result, "catalog", blockName) : result;
	}

	private static JSONObject addTypeDefinition(DatabaseObject targetDbo) throws Exception {
		var root = flowAuthoringRoot(targetDbo);
		if (!(root instanceof FlowEngine flowEngine) || !canAddTypeDefinition(targetDbo)) {
			return new JSONObject().put("done", false);
		}
		var projectionRoot = engineProjectionRoot(flowEngine, "catalog");
		var typeName = nextTypeName(flowEngine);
		var response = new FlowEngineBridge().createType(flowEngine, typeName);
		var done = isSuccessResponse(response);
		var result = new JSONObject()
				.put("done", done)
				.put("id", done ? flowEngine.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
		return done ? refreshEngineProjection(flowEngine, projectionRoot, result, "catalog", typeName) : result;
	}

	private static JSONObject addPropertyDefinition(DatabaseObject targetDbo) throws Exception {
		var target = propertyDefinitionTarget(targetDbo);
		var root = flowAuthoringRoot(target);
		if (!(root instanceof FlowEngine flowEngine) || target == null) {
			return new JSONObject().put("done", false);
		}
		var projectionRoot = engineProjectionRoot(flowEngine, "catalog");
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
		var result = new JSONObject()
				.put("done", done)
				.put("id", done ? flowEngine.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
		return done ? refreshEngineProjection(flowEngine, projectionRoot, result, "catalog", "") : result;
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
			var projectionRoot = engineProjectionRoot(flowEngine, "frontends");
			var response = new FlowEngineBridge().applyMutation(flowEngine, engineMutation);
			var done = isSuccessResponse(response);
			var result = new JSONObject()
					.put("done", done)
					.put("id", done ? flowEngine.getFullQName() : "")
					.put("error", done ? JSONObject.NULL : response.opt("error"));
			return done ? refreshEngineProjection(flowEngine, projectionRoot, result, "frontends", "") : result;
		}
		var mutation = frontendMutationFor(targetDbo, position, insert, data.optJSONObject("targetSlot"));
		if (mutation == null) {
			flowStudioWarn("Flow frontend DnD palette insert rejected: target=" + flowMoveTargetSummary(targetDbo)
					+ " position=" + position + " data=" + data);
			return new JSONObject().put("done", false);
		}
		flowStudioInfo("Flow frontend DnD palette insert mutation: target=" + flowMoveTargetSummary(targetDbo)
				+ " position=" + position + " mutation=" + mutation);
		var selectionSourcePath = frontendMutationSourcePath(targetDbo, mutation);
		var selectionMutationPath = frontendSelectionMutationPath(mutation);
		var selectionId = frontendMutationValueId(mutation);
		var projectionRoot = frontendProjectionRoot(targetDbo, selectionSourcePath);
		var response = applyFrontendMutation(flowEngine, targetDbo, mutation);
		var done = response.optBoolean("ok", false) && sourceMutationChanged(response);
		flowStudioInfo("Flow frontend DnD palette insert response: done=" + done
				+ " ok=" + response.optBoolean("ok", false)
				+ " changed=" + response.opt("changed")
				+ " debug=" + response.opt("debug")
				+ " error=" + response.opt("error"));
		return withProjectedSelection(withProjectionMetadata(new JSONObject()
					.put("done", done)
					.put("id", done ? flowEngine.getFullQName() : "")
					.put("selectionSourcePath", done ? selectionSourcePath : "")
					.put("selectionMutationPath", done ? selectionMutationPath : "")
					.put("selectionId", done ? selectionId : "")
					.put("error", done ? JSONObject.NULL : response.has("error") ? response.opt("error")
							: "Frontend source mutation did not change the source."), response), projectionRoot);
	}

	private static String frontendMutationSourcePath(DatabaseObject targetDbo, JSONObject mutation) {
		var overrideSourcePath = mutation == null ? "" : mutation.optString("__sourcePath", "");
		if (!overrideSourcePath.isBlank()) {
			return overrideSourcePath;
		}
		return targetDbo instanceof FlowVirtualObject flowObject ? sourcePath(flowObject) : "";
	}

	private static String frontendSelectionMutationPath(JSONObject mutation) {
		if (mutation == null || !"insert".equals(mutation.optString("op", ""))) {
			return "";
		}
		var path = mutation.optString("path", "");
		return path.isBlank() ? "" : path + "[" + Math.max(0, mutation.optInt("index", 0)) + "]";
	}

	private static String frontendMutationValueId(JSONObject mutation) {
		var value = mutation == null ? null : mutation.optJSONObject("value");
		return value == null ? "" : value.optString("id", "");
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
		var projectionRoot = engineProjectionRoot(flowEngine, "frontends");
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
		var project = flowEngine.getProject();
		var projectDir = project == null ? new File(".") : project.getDirFile();
		var rootDir = new File(projectDir, "libs/flow/frontbuilder/" + safeFileName(builderName));
		var rootPath = rootDir.getCanonicalPath();
		Map<String, String> values = null;
		File file = null;
		String source = "";
		for (int attempt = 0; attempt < 100; attempt++) {
			var candidateId = attempt == 0 ? blockId : blockId + (attempt + 1);
			values = frontendSourceTemplateValues(builderName, candidateId);
			var directory = applyTemplate(firstNonBlank(create, "directory"), values);
			var fileName = applyTemplate(firstNonBlank(create, "fileName"), values);
			values.put("fileName", fileName);
			source = applyTemplate(create.optString("source", ""), values);
			var dir = new File(rootDir, directory);
			file = new File(dir, fileName);
			var filePath = file.getCanonicalPath();
			if (!filePath.startsWith(rootPath + File.separator)) {
				return new JSONObject()
						.put("done", false)
						.put("error", "Frontend source path escapes builder root: " + filePath);
			}
			if (!file.isFile()) {
				blockId = candidateId;
				break;
			}
			file = null;
		}
		if (file == null) {
			return new JSONObject()
					.put("done", false)
					.put("error", "Unable to allocate a unique frontend source for " + baseId);
		}
		file.getParentFile().mkdirs();
		FileUtils.writeStringToFile(file, source, "UTF-8");
		FlowEngineBridge.invalidateDataCaches();
		var result = new JSONObject()
				.put("done", true)
				.put("id", flowEngine.getFullQName())
				.put("file", file.getAbsolutePath())
				.put("sourceId", blockId);
		return refreshEngineProjection(flowEngine, projectionRoot, result, "frontends", blockId);
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
		return frontendMutationFor(targetDbo, position, insert, null);
	}

	private static JSONObject frontendMutationFor(DatabaseObject targetDbo, String position, JSONObject insert, JSONObject targetSlot) throws Exception {
		if (!(targetDbo instanceof FlowVirtualObject fvo) || sourcePath(fvo).isBlank()) {
			return null;
		}
		var writable = targetSlot == null || !targetSlot.has("sourceWritable")
				? sourceFlag(fvo, "sourceWritable")
				: targetSlot.optBoolean("sourceWritable", sourceFlag(fvo, "sourceWritable"));
		if (!writable) {
			return null;
		}
		position = position == null || position.isBlank() ? "inside" : position;
		var cleanedInsert = cleanFrontendInsertValue(insert);
		var insertValue = frontendInsertNeedsGeneratedId(fvo, cleanedInsert) ? uniqueFrontendInsertValue(fvo, cleanedInsert) : cleanedInsert;
		var siblingInsertion = frontendSiblingInsertion(fvo, position);
		var insertSourcePath = firstNonBlank(insert, "__frontendSourcePath");
		if (siblingInsertion != null) {
			insertSourcePath = sourcePath(fvo);
		}
		if (insertSourcePath.isBlank()) {
			insertSourcePath = sourceValue(fvo, "frontendInsertSourcePath");
		}
		if (insertSourcePath.isBlank()) {
			insertSourcePath = sourcePath(fvo);
		}
		var insertMutationPath = firstNonBlank(insert, "__frontendMutationPath");
		if (siblingInsertion != null) {
			insertMutationPath = siblingInsertion.path;
		}
		if (insertMutationPath.isBlank() && targetSlot != null) {
			insertMutationPath = firstNonBlank(targetSlot, "sourceMutationPath");
		}
		if (insertMutationPath.isBlank()) {
			insertMutationPath = sourceValue(fvo, "frontendInsertMutationPath");
		}
		if (!insertSourcePath.isBlank() && !insertMutationPath.isBlank()) {
			if (insert.optBoolean("__frontendPropertyDefinition", false)) {
				var propertyName = uniqueFrontendPropertyName(fvo, firstNonBlank(insert, "name", "id"));
				var propertyValue = new JSONObject(insertValue.toString());
				propertyValue.remove("name");
				propertyValue.remove("id");
				return new JSONObject()
						.put("op", "replace")
						.put("path", insertMutationPath + "." + propertyName)
						.put("value", propertyValue)
						.put("__sourcePath", insertSourcePath);
			}
			if (siblingInsertion != null) {
				return new JSONObject()
						.put("op", "insert")
						.put("path", insertMutationPath)
						.put("index", siblingInsertion.index)
						.put("value", insertValue)
						.put("__sourcePath", insertSourcePath);
			}
			if (targetSlot != null && targetSlot.has("index")) {
				return new JSONObject()
						.put("op", "insert")
						.put("path", insertMutationPath)
						.put("index", Math.max(0, targetSlot.optInt("index", 0)))
						.put("value", insertValue)
						.put("__sourcePath", insertSourcePath);
			}
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

	private static String uniqueFrontendPropertyName(FlowVirtualObject target, String baseName) throws Exception {
		baseName = baseName == null || baseName.isBlank() ? "property" : baseName.trim();
		baseName = safeFileName(baseName).replace('-', '_');
		if (baseName.isBlank()) {
			baseName = "property";
		}
		var used = new HashSet<String>();
		var folder = "frontendBlockProperties".equals(target.getVirtualType()) ? target
				: target.getParent() instanceof FlowVirtualObject parent ? parent : target;
		for (var child : folder.getDatabaseObjectChildren()) {
			if (child instanceof FlowVirtualObject fvo) {
				var definition = fvo.getDefinitionObject();
				var name = definition == null ? "" : definition.optString("name", "");
				if (!name.isBlank()) {
					used.add(name);
				}
			}
		}
		var candidate = baseName;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = baseName + i;
		}
		return candidate;
	}

	private static boolean frontendInsertNeedsGeneratedId(FlowVirtualObject target, JSONObject insert) {
		if (insert != null && insert.optBoolean("__frontendPropertyDefinition", false)) {
			return false;
		}
		var targetKind = target == null ? "" : target.getVirtualKind();
		if ("frontendColumns".equals(targetKind) || "frontendActionVariables".equals(targetKind)) {
			return false;
		}
		return insert != null && (insert.has("id") || insert.has("kind") || insert.has("tag"));
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

	private static boolean frontendAcceptedPositionMatch(JSONObject data, String position) {
		var acceptedPositions = data == null ? null : data.optJSONArray("acceptedPositions");
		if (acceptedPositions == null || acceptedPositions.length() == 0) {
			return true;
		}
		position = position == null || position.isBlank() ? "inside" : position;
		var acceptsInside = false;
		for (int i = 0; i < acceptedPositions.length(); i++) {
			var accepted = acceptedPositions.optString(i);
			if (position.equals(accepted)) {
				return true;
			}
			acceptsInside |= "inside".equals(accepted);
		}
		return ("before".equals(position) || "after".equals(position)) && acceptsInside;
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
		var used = new HashSet<String>();
		var targetSourcePath = sourcePath(target);
		var projectionRoot = targetSourcePath.isBlank() ? null : frontendProjectionRoot(target, targetSourcePath);
		if (projectionRoot != null) {
			collectFrontendWidgetIds(projectionRoot, used);
		}
		var component = "frontendComponent".equals(target.getVirtualKind()) ? target : target.getParent();
		if (component instanceof FlowVirtualObject componentFvo) {
			collectFrontendWidgetIds(componentFvo, used);
		}
		var referencedComponent = frontendReferencedComponent(target);
		if (referencedComponent != null && referencedComponent != component) {
			collectFrontendWidgetIds(referencedComponent, used);
		}
		used.addAll(frontendReferencedComponentWidgetIds(target));
		return uniqueFrontendInsertValue(insert, used);
	}

	static void collectFrontendWidgetIds(DatabaseObject candidate, Set<String> used) throws Exception {
		if (candidate == null || used == null) {
			return;
		}
		if (candidate instanceof FlowVirtualObject flowObject && "frontendWidget".equals(flowObject.getVirtualKind())) {
			var definition = flowObject.getDefinitionObject();
			var id = definition == null ? "" : definition.optString("id", "");
			if (!id.isBlank()) {
				used.add(id);
			}
		}
		for (var child : candidate.getDatabaseObjectChildren()) {
			collectFrontendWidgetIds(child, used);
		}
	}

	static JSONObject uniqueFrontendInsertValue(JSONObject insert, Set<String> used) throws Exception {
		var value = new JSONObject(insert.toString());
		var base = value.optString("id", value.optString("kind", "widget"));
		base = base.replaceAll("[^A-Za-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_+|_+$", "");
		if (base.isBlank()) {
			base = "widget";
		}
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

	public static boolean canRemoveNode(DatabaseObject targetDbo) {
		if (!(targetDbo instanceof FlowVirtualObject fvo) || flowAuthoringRoot(fvo) == null) {
			return false;
		}
		if (!sourcePath(fvo).isBlank() && !sourceFlag(fvo, "sourceWritable")) {
			return false;
		}
		return removeNodeMutation(fvo) != null;
	}

	public static JSONObject removeNode(DatabaseObject targetDbo) throws Exception {
		if (!(targetDbo instanceof FlowVirtualObject fvo) || !canRemoveNode(fvo)) {
			return new JSONObject()
					.put("done", false)
					.put("error", "The selected Flow item is not backed by a removable source entry.");
		}
		var root = flowAuthoringRoot(fvo);
		var mutation = removeNodeMutation(fvo);
		var removedId = fvo.getFullQName();
		var parentId = fvo.getParent() == null ? "" : fvo.getParent().getFullQName();
		flowStudioInfo("Flow source remove mutation: target=" + flowMoveTargetSummary(fvo)
				+ " sourcePath=" + sourcePath(fvo) + " mutation=" + mutation);
		var mutationResponse = applyMutation(root, fvo, mutation);
		var done = mutationResponse.optBoolean("ok", false) && sourceMutationChanged(mutationResponse);
		return withProjectionMetadata(new JSONObject()
				.put("done", done)
				.put("id", done ? removedId : "")
				.put("parentId", done ? parentId : "")
				.put("error", done ? JSONObject.NULL : mutationResponse.has("error")
						? mutationResponse.opt("error")
						: "Flow item removal did not change the source."), mutationResponse);
	}

	static JSONObject removeNodeMutation(FlowVirtualObject fvo) {
		var path = fvo == null ? "" : mutationPath(fvo);
		if (path.isBlank() || parentArrayPath(path) == null) {
			return null;
		}
		try {
			return new JSONObject().put("op", "delete").put("path", path);
		} catch (JSONException e) {
			return null;
		}
	}

	public static JSONObject moveNode(DatabaseObject sourceDbo, DatabaseObject targetDbo, boolean insertBefore) throws Exception {
		return moveNode(sourceDbo, targetDbo, insertBefore ? "before" : "after");
	}

	public static JSONObject moveNode(DatabaseObject sourceDbo, DatabaseObject targetDbo, String position) throws Exception {
		if (!(sourceDbo instanceof FlowVirtualObject source) || !(targetDbo instanceof FlowVirtualObject target)
				|| !canMoveNode(sourceDbo, targetDbo, position)) {
			flowStudioInfo("Flow frontend DnD move rejected: source=" + flowMoveTargetSummary(sourceDbo)
					+ " target=" + flowMoveTargetSummary(targetDbo) + " position=" + position);
			return new JSONObject().put("done", false);
		}
		position = normalizeMovePosition(position);
		if ("inside".equals(position)) {
			return moveNodeInside(source, target);
		}
		var sourceCollectionPath = parentCollectionPath(mutationPath(source));
		var targetCollectionPath = parentCollectionPath(mutationPath(target));
		if (sourceCollectionPath == null || targetCollectionPath == null) {
			return new JSONObject().put("done", false);
		}
		var sameCollection = sourceCollectionPath.equals(targetCollectionPath);
		var sourceIndex = collectionIndex(source);
		var targetIndex = collectionIndex(target);
		if (sameCollection && sourceIndex == targetIndex) {
			flowStudioInfo("Flow frontend DnD move no-op before mutation: source=" + flowMoveTargetSummary(source)
					+ " target=" + flowMoveTargetSummary(target) + " sourceIndex=" + sourceIndex
					+ " targetIndex=" + targetIndex);
			return new JSONObject().put("done", false);
		}
		var insertBefore = "before".equals(position);
		var insertIndex = targetIndex + (insertBefore ? 0 : 1);
		if (sameCollection && sourceIndex < targetIndex) {
			insertIndex--;
		}
		flowStudioInfo("Flow frontend DnD move request: source=" + flowMoveTargetSummary(source)
				+ " target=" + flowMoveTargetSummary(target) + " position=" + position
				+ " sourceIndex=" + sourceIndex + " targetIndex=" + targetIndex
				+ " targetCollectionPath=" + targetCollectionPath + " insertIndex=" + insertIndex);
		return moveNodeToCollectionIndex(source, targetCollectionPath, insertIndex);
	}

	public static boolean canMoveNode(DatabaseObject sourceDbo, DatabaseObject targetDbo) {
		return canMoveNode(sourceDbo, targetDbo, "before")
				|| canMoveNode(sourceDbo, targetDbo, "after")
				|| canMoveNode(sourceDbo, targetDbo, "inside");
	}

	public static boolean canMoveNode(DatabaseObject sourceDbo, DatabaseObject targetDbo, String position) {
		if (!(sourceDbo instanceof FlowVirtualObject source) || !(targetDbo instanceof FlowVirtualObject target)
				|| !compatibleMoveKinds(source, target)) {
			return false;
		}
		position = normalizeMovePosition(position);
		if ("inside".equals(position)) {
			return canMoveNodeInside(source, target);
		}
		var sourcePath = mutationPath(source);
		var targetPath = mutationPath(target);
		var sourceCollectionPath = parentCollectionPath(mutationPath(source));
		var targetCollectionPath = parentCollectionPath(mutationPath(target));
		if (sourceCollectionPath == null || targetCollectionPath == null
				|| targetCollectionPath.equals(sourcePath) || targetCollectionPath.startsWith(sourcePath + ".")) {
			return false;
		}
		if ("node".equals(source.getVirtualKind()) && !isSourceBackedTarget(source) && !isSourceBackedTarget(target)) {
			return true;
		}
		return isSourceBackedTarget(source)
				&& isSourceBackedTarget(target)
				&& sourcePath(source).equals(sourcePath(target));
	}

	private static String normalizeMovePosition(String position) {
		return position == null || position.isBlank() ? "inside" : position;
	}

	private static boolean canMoveNodeInside(FlowVirtualObject source, FlowVirtualObject target) {
		var sourcePath = mutationPath(source);
		var sourceCollectionPath = parentCollectionPath(sourcePath);
		var targetCollectionPath = moveInsideCollectionPath(target);
		if (sourcePath.isBlank() || sourceCollectionPath == null || targetCollectionPath == null || source == target
				|| targetCollectionPath.equals(sourcePath) || targetCollectionPath.startsWith(sourcePath + ".")) {
			return false;
		}
		if ("node".equals(source.getVirtualKind()) && !isSourceBackedTarget(source) && !isSourceBackedTarget(target)) {
			return true;
		}
		return isSourceBackedTarget(source)
				&& isSourceBackedTarget(target)
				&& sourcePath(source).equals(sourcePath(target));
	}

	private static JSONObject moveNodeInside(FlowVirtualObject source, FlowVirtualObject target) throws Exception {
		var root = flowAuthoringRoot(source);
		var sourcePath = mutationPath(source);
		var targetPath = moveInsideCollectionPath(target);
		if (root == null || sourcePath.isBlank() || targetPath == null || targetPath.isBlank()) {
			return new JSONObject().put("done", false);
		}

		var selectionSourcePath = sourcePath(source);
		var selectionId = flowVirtualObjectId(source);
		var projectionRoot = frontendProjectionRoot(source, selectionSourcePath);
		var mutation = new JSONObject()
				.put("op", "move")
				.put("from", sourcePath)
				.put("fromId", flowVirtualObjectId(source))
				.put("path", targetPath);
		flowStudioInfo("Flow frontend DnD move inside mutation: source=" + flowMoveTargetSummary(source)
				+ " target=" + flowMoveTargetSummary(target)
				+ " sourcePath=" + sourcePath + " targetPath=" + targetPath
				+ " mutation=" + mutation);
		var response = applyMutation(root, source, mutation);
		var done = response.optBoolean("ok", false) && sourceMutationChanged(response);
		flowStudioInfo("Flow frontend DnD move inside response: done=" + done
				+ " ok=" + response.optBoolean("ok", false)
				+ " changed=" + response.opt("changed")
				+ " debug=" + response.opt("debug")
				+ " error=" + response.opt("error"));
		return withProjectedSelection(withProjectionMetadata(new JSONObject()
				.put("done", done)
				.put("id", done ? root.getFullQName() : "")
				.put("selectionSourcePath", done ? selectionSourcePath : "")
				.put("selectionId", done ? selectionId : "")
				.put("error", done ? JSONObject.NULL : response.has("error") ? response.opt("error")
						: "Flow node move did not change the source."), response), projectionRoot);
	}

	private static String moveInsideCollectionPath(FlowVirtualObject target) {
		var path = mutationPath(target);
		if (path.isBlank()) {
			return null;
		}
		var root = flowAuthoringRoot(target);
		if (!(root instanceof Flow) && !sourceFlag(target, "sourceWritable")) {
			return null;
		}
		var kind = target.getVirtualKind();
		if ("blockImplementation".equals(kind) || "folder".equals(kind) || "slot".equals(kind)
				|| "frontendStructure".equals(kind) || "frontendSlot".equals(kind)
				|| "frontendEvents".equals(kind) || "frontendActionVariables".equals(kind)
				|| "frontendColumns".equals(kind) || "frontendDataBindings".equals(kind)) {
			return path;
		}
		var firstSlot = firstSourceSlotMutationPath(target);
		return firstSlot.isBlank() ? null : firstSlot;
	}

	private static String firstSourceSlotMutationPath(FlowVirtualObject target) {
		var info = target.getVirtualInfoObject();
		var path = firstSourceSlotMutationPath(info);
		if (!path.isBlank()) {
			return path;
		}
		return firstSourceSlotMutationPath(target.getDefinitionObject());
	}

	private static String firstSourceSlotMutationPath(JSONObject object) {
		if (object == null) {
			return "";
		}
		var slots = object.optJSONObject("slots");
		if (slots == null || slots.length() == 0) {
			return "";
		}
		for (var keys = slots.keys(); keys.hasNext();) {
			var slot = slots.optJSONObject(String.valueOf(keys.next()));
			var path = firstNonBlank(slot, "sourceMutationPath");
			if (!path.isBlank() && slot.optBoolean("sourceWritable", true)) {
				return path;
			}
		}
		return "";
	}

	private static boolean compatibleMoveKinds(FlowVirtualObject source, FlowVirtualObject target) {
		if (source.getVirtualKind().equals(target.getVirtualKind())) {
			return true;
		}
		if ("node".equals(source.getVirtualKind()) && ("slot".equals(target.getVirtualKind())
				|| "blockImplementation".equals(target.getVirtualKind()) || "folder".equals(target.getVirtualKind()))) {
			return true;
		}
		return isFrontendVirtualKind(source.getVirtualKind()) && isFrontendVirtualKind(target.getVirtualKind());
	}

	private static boolean isFrontendVirtualKind(String kind) {
		return kind != null && kind.startsWith("frontend");
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
		var collectionPath = parentCollectionPath(path);
		if (root == null || collectionPath == null) {
			return new JSONObject().put("done", false);
		}
		var parent = fvo.getParent();
		var siblingCount = parent == null ? 0 : parent.getDatabaseObjectChildren().size();
		var currentIndex = collectionIndex(fvo);
		targetIndex = Math.max(0, Math.min(siblingCount - 1, targetIndex));
		if (targetIndex == currentIndex) {
			flowStudioInfo("Flow frontend DnD move no-op after clamp: source=" + flowMoveTargetSummary(fvo)
					+ " currentIndex=" + currentIndex + " targetIndex=" + targetIndex
					+ " siblingCount=" + siblingCount);
			return new JSONObject().put("done", false);
		}

		return moveNodeToCollectionIndex(fvo, collectionPath, targetIndex);
	}

	private static JSONObject moveNodeToCollectionIndex(FlowVirtualObject fvo, String collectionPath, int targetIndex) throws Exception {
		var root = flowAuthoringRoot(fvo);
		var path = mutationPath(fvo);
		if (root == null || path.isBlank() || collectionPath == null || collectionPath.isBlank()) {
			return new JSONObject().put("done", false);
		}
		targetIndex = Math.max(0, targetIndex);
		var selectionSourcePath = sourcePath(fvo);
		var selectionId = flowVirtualObjectId(fvo);
		var selectionMutationPath = collectionPath + "[" + targetIndex + "]";
		var projectionRoot = frontendProjectionRoot(fvo, selectionSourcePath);
		var mutation = new JSONObject()
				.put("op", "move")
				.put("from", path)
				.put("fromId", flowVirtualObjectId(fvo))
				.put("path", collectionPath)
				.put("index", targetIndex);
		flowStudioInfo("Flow frontend DnD move mutation: source=" + flowMoveTargetSummary(fvo)
				+ " sourcePath=" + sourcePath(fvo) + " mutation=" + mutation);
		var response = applyMutation(root, fvo, mutation);
		var done = response.optBoolean("ok", false) && sourceMutationChanged(response);
		flowStudioInfo("Flow frontend DnD move response: done=" + done
				+ " ok=" + response.optBoolean("ok", false)
				+ " changed=" + response.opt("changed")
				+ " debug=" + response.opt("debug")
				+ " error=" + response.opt("error"));
		return withProjectedSelection(withProjectionMetadata(new JSONObject()
				.put("done", done)
				.put("id", done ? root.getFullQName() : "")
				.put("selectionSourcePath", done ? selectionSourcePath : "")
				.put("selectionMutationPath", done ? selectionMutationPath : "")
				.put("selectionId", done ? selectionId : "")
				.put("error", done ? JSONObject.NULL : response.has("error") ? response.opt("error")
						: "Flow node move did not change the source."), response), projectionRoot);
	}

	private static String flowVirtualObjectId(FlowVirtualObject fvo) {
		if (fvo == null) {
			return "";
		}
		var definition = fvo.getDefinitionObject();
		if (definition != null) {
			var id = definition.optString("id", "");
			if (!id.isBlank()) {
				return id;
			}
		}
		return fvo.getName();
	}

	private static String flowMoveTargetSummary(DatabaseObject dbo) {
		if (!(dbo instanceof FlowVirtualObject fvo)) {
			return dbo == null ? "null" : dbo.getClass().getSimpleName() + ":" + dbo.getName();
		}
		return "{summary=" + fvo.getSummary()
				+ ", kind=" + fvo.getVirtualKind()
				+ ", type=" + fvo.getVirtualType()
				+ ", path=" + fvo.getVirtualPath()
				+ ", sourcePath=" + sourcePath(fvo)
				+ ", mutationPath=" + mutationPath(fvo) + "}";
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
		if (!(root instanceof FlowEngine flowEngine)) {
			return false;
		}
		if (targetDbo instanceof FlowEngine) {
			return true;
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			var path = fvo.getVirtualPath();
			var projectName = flowEngine.getProject() == null ? "" : flowEngine.getProject().getName();
			var projectProviderPath = projectName.isBlank() ? "" : "catalog.blocks." + projectName;
			if ("catalog".equals(path)
					|| "catalog.blocks".equals(path)
					|| "catalog.blocks.project".equals(path)
					|| (!projectProviderPath.isBlank() && projectProviderPath.equals(path))) {
				return true;
			}
			return !projectProviderPath.isBlank()
					&& path.startsWith(projectProviderPath + ".")
					&& "folder".equals(fvo.getVirtualKind())
					&& "namespace".equals(fvo.getVirtualType());
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
			return applyProjectedSourceMutation(flowEngine, targetDbo, sourcePath, mutation);
		}
		return root instanceof Flow flow
				? new FlowEngineBridge().applyMutation(flow, mutation)
				: new FlowEngineBridge().applyMutation((FlowEngine) root, mutation);
	}

	private static JSONObject applyFrontendMutation(FlowEngine flowEngine, DatabaseObject targetDbo, JSONObject mutation) throws Exception {
		var overrideSourcePath = mutation == null ? "" : mutation.optString("__sourcePath", "");
		if (!overrideSourcePath.isBlank()) {
			flowStudioInfo("Flow frontend DnD apply override mutation: sourcePath=" + overrideSourcePath
					+ " mutation=" + cleanFrontendMutation(mutation));
			return applyProjectedSourceMutation(flowEngine, targetDbo, overrideSourcePath, mutation);
		}
		if (targetDbo instanceof FlowVirtualObject fvo) {
			var sourcePath = sourcePath(fvo);
			if (!sourcePath.isBlank() && sourceFlag(fvo, "sourceWritable")) {
				flowStudioInfo("Flow frontend DnD apply target mutation: target=" + flowMoveTargetSummary(fvo)
						+ " mutation=" + mutation);
				return applyProjectedSourceMutation(flowEngine, targetDbo, sourcePath, mutation);
			}
		}
		return applyMutation(flowEngine, targetDbo, mutation);
	}

	public static JSONObject applyProjectedSourceMutation(FlowEngine flowEngine, DatabaseObject targetDbo,
			String sourcePath, JSONObject mutation) throws Exception {
		var cleanMutation = cleanFrontendMutation(mutation);
		var projectionRoot = frontendProjectionRoot(targetDbo, sourcePath);
		var response = new FlowEngineBridge().applySourceMutation(flowEngine, sourcePath, cleanMutation,
				projectionRoot == null ? "" : projectionRoot.getVirtualPath());
		if (response.optBoolean("ok", false) && sourceMutationChanged(response)) {
			afterSourceMutation(flowEngine, sourcePath);
			applyFrontendProjection(projectionRoot, sourcePath, response);
		} else if (response.optBoolean("ok", false)) {
			flowStudioWarn("Flow frontend source mutation made no changes: sourcePath=" + sourcePath
					+ " mutation=" + cleanMutation + " debug=" + response.opt("debug"));
		}
		return response;
	}

	private static FlowVirtualObject frontendProjectionRoot(DatabaseObject targetDbo, String sourcePath) {
		for (var current = targetDbo; current instanceof FlowVirtualObject fvo; current = current.getParent()) {
			if (sourcePath.equals(sourcePath(fvo)) && "frontAst".equals(sourceMutationPath(fvo))) {
				return fvo;
			}
		}
		return null;
	}

	private static void applyFrontendProjection(FlowVirtualObject projectionRoot, String sourcePath, JSONObject response) {
		if (projectionRoot == null || response == null) {
			return;
		}
		var tree = response.optJSONObject("authoringTree");
		var children = tree == null || !tree.optBoolean("ok", false) ? null : tree.optJSONArray("children");
		var projected = children == null || children.length() != 1 ? null : children.optJSONObject(0);
		if (projected == null || !projectionRoot.replaceProjectedTree(projected)) {
			flowStudioWarn("Flow frontend source projection could not replace the in-memory root: sourcePath="
					+ sourcePath + " rootPath=" + projectionRoot.getVirtualPath());
			return;
		}
		try {
			response.put("projected", true)
					.put("projectedRootPath", projectionRoot.getVirtualPath())
					.put("projectedSourcePath", sourcePath);
		} catch (JSONException e) {
			flowStudioWarn("Unable to expose Flow frontend projection metadata.", e);
		}
	}

	private static JSONObject withProjectionMetadata(JSONObject result, JSONObject response) throws JSONException {
		return result
				.put("projected", response != null && response.optBoolean("projected", false))
				.put("projectedRootPath", response == null ? "" : response.optString("projectedRootPath", ""))
				.put("projectedSourcePath", response == null ? "" : response.optString("projectedSourcePath", ""))
				.put("selectionVirtualPath", response == null ? "" : response.optString("selectionVirtualPath", ""));
	}

	private static JSONObject withProjectedSelection(JSONObject result, FlowVirtualObject projectionRoot) {
		if (result == null || projectionRoot == null || !result.optBoolean("done", false)) {
			return result;
		}
		try {
			var selected = findProjectedSelection(projectionRoot,
					result.optString("selectionSourcePath", result.optString("projectedSourcePath", "")),
					result.optString("selectionMutationPath", ""), result.optString("selectionId", ""),
					result.optString("selectionVirtualPath", ""));
			if (selected == null) {
				return result;
			}
			result.put("id", selected.getFullQName())
					.put("selectionVirtualPath", selected.getVirtualPath());
			if (selected.getParent() != null) {
				result.put("parentId", selected.getParent().getFullQName());
			}
		} catch (Exception e) {
			flowStudioWarn("Unable to resolve the projected Flow selection.", e);
		}
		return result;
	}

	private static FlowVirtualObject findProjectedSelection(DatabaseObject root, String sourcePath,
			String mutationPath, String id, String virtualPath) {
		var selected = findProjectedSelectionMatch(root, sourcePath, "", "", virtualPath);
		if (selected == null) {
			selected = findProjectedSelectionMatch(root, sourcePath, mutationPath, "", "");
		}
		if (selected == null) {
			selected = findProjectedSelectionMatch(root, sourcePath, "", id, "");
		}
		return selected;
	}

	private static FlowVirtualObject findProjectedSelectionMatch(DatabaseObject candidate, String sourcePath,
			String mutationPath, String id, String virtualPath) {
		if (candidate instanceof FlowVirtualObject flowObject
				&& matchesProjectedSelection(flowObject, sourcePath, mutationPath, id, virtualPath)) {
			return flowObject;
		}
		try {
			for (var child : candidate.getDatabaseObjectChildren()) {
				var selected = findProjectedSelectionMatch(child, sourcePath, mutationPath, id, virtualPath);
				if (selected != null) {
					return selected;
				}
			}
		} catch (Exception e) {
			flowStudioWarn("Unable to inspect a projected Flow selection candidate.", e);
		}
		return null;
	}

	static boolean matchesProjectedSelection(FlowVirtualObject candidate, String sourcePath,
			String mutationPath, String id, String virtualPath) {
		if (candidate == null) {
			return false;
		}
		if (virtualPath != null && !virtualPath.isBlank()) {
			return virtualPath.equals(candidate.getVirtualPath());
		}
		var sourceMatches = sourcePath == null || sourcePath.isBlank() || sourcePath.equals(sourcePath(candidate));
		if (!sourceMatches) {
			return false;
		}
		if (mutationPath != null && !mutationPath.isBlank()) {
			return mutationPath.equals(sourceMutationPath(candidate));
		}
		if (id == null || id.isBlank()) {
			return false;
		}
		var definition = candidate.getDefinitionObject();
		return definition != null && id.equals(definition.optString("id", ""));
	}

	private static FlowVirtualObject engineProjectionRoot(FlowEngine flowEngine, String virtualPath) {
		if (flowEngine == null || virtualPath == null || virtualPath.isBlank()) {
			return null;
		}
		try {
			for (var child : flowEngine.getDatabaseObjectChildren()) {
				var found = engineProjectionRoot(child, virtualPath);
				if (found != null) {
					return found;
				}
			}
		} catch (Exception e) {
			flowStudioWarn("Unable to capture Flow engine projection root: " + virtualPath, e);
		}
		return null;
	}

	private static FlowVirtualObject engineProjectionRoot(DatabaseObject candidate, String virtualPath) {
		if (candidate instanceof FlowVirtualObject flowObject && virtualPath.equals(flowObject.getVirtualPath())) {
			return flowObject;
		}
		try {
			for (var child : candidate.getDatabaseObjectChildren()) {
				var found = engineProjectionRoot(child, virtualPath);
				if (found != null) {
					return found;
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	private static JSONObject refreshEngineProjection(FlowEngine flowEngine, FlowVirtualObject projectionRoot,
			JSONObject result, String virtualPath, String selectionType) throws Exception {
		clearCatalogCache(flowEngine);
		if (projectionRoot == null) {
			return result;
		}
		var tree = new FlowEngineBridge().describeTree(flowEngine);
		var projected = findTreeNode(tree, virtualPath);
		if (projected == null || !projectionRoot.replaceProjectedTree(projected)) {
			flowStudioWarn("Flow engine projection could not replace the in-memory root: " + virtualPath);
			return result;
		}
		result
				.put("projected", true)
				.put("projectedRootPath", virtualPath)
				.put("projectedSourcePath", "");
		var selection = findTreeNodeByType(projected, selectionType);
		if (selection != null) {
			result.put("selectionVirtualPath", selection.optString("path", ""));
		}
		return result;
	}

	private static JSONObject findTreeNodeByType(JSONObject tree, String type) {
		if (tree == null || type == null || type.isBlank()) {
			return null;
		}
		if (type.equals(tree.optString("type", ""))) {
			return tree;
		}
		var children = tree.optJSONArray("children");
		if (children == null) {
			return null;
		}
		for (int i = 0; i < children.length(); i++) {
			var found = findTreeNodeByType(children.optJSONObject(i), type);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static JSONObject findTreeNode(JSONObject tree, String virtualPath) {
		if (tree == null || virtualPath == null || virtualPath.isBlank()) {
			return null;
		}
		if (virtualPath.equals(tree.optString("path", ""))) {
			return tree;
		}
		var children = tree.optJSONArray("children");
		if (children == null) {
			return null;
		}
		for (int i = 0; i < children.length(); i++) {
			var found = findTreeNode(children.optJSONObject(i), virtualPath);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static JSONObject cleanFrontendMutation(JSONObject mutation) throws Exception {
		var clean = new JSONObject(mutation.toString());
		clean.remove("__sourcePath");
		return clean;
	}

	private static boolean sourceMutationChanged(JSONObject response) {
		return response == null || !response.has("changed") || response.optBoolean("changed", true);
	}

	public static void afterSourceMutation(FlowEngine flowEngine, String sourcePath) {
		clearCatalogCache(flowEngine);
		if (flowEngine == null || !isFrontendSourcePath(sourcePath)) {
			return;
		}
		for (var target : frontendDevSyncTargets(flowEngine)) {
			clearCatalogCache(target);
			try {
				var response = new FlowEngineBridge().contextAction(target, new JSONObject()
						.put("frontendSourceDrafts", FlowEngineBridge.frontendSourceDrafts(target, flowEngine))
						.put("sourcePath", sourcePath)
						.put("action", new JSONObject()
								.put("id", "frontbuilder.svelte.dev.sync")
								.put("payload", new JSONObject()
										.put("sourcePath", sourcePath))));
				var browser = response.optJSONObject("browser");
				if (browser != null) {
					FlowEngineBridge.notifyStudioBrowser(browser.toString());
				}
				var project = target.getProject() == null ? "" : target.getProject().getName();
				if (response.optBoolean("generated", false)) {
					flowStudioInfo("Flow frontend dev source regenerated after mutation: project=" + project
							+ " sourcePath=" + sourcePath + " details=" + response.opt("details"));
				} else if (!response.optBoolean("ok", true)) {
					flowStudioWarn("Flow frontend dev source regeneration failed after mutation: project=" + project
							+ " sourcePath=" + sourcePath + " response=" + response);
				}
			} catch (Exception e) {
				flowStudioWarn("Unable to update Flow frontend dev source after mutation: " + e.getMessage());
			}
		}
	}

	private static ArrayList<FlowEngine> frontendDevSyncTargets(FlowEngine source) {
		var targets = new ArrayList<FlowEngine>();
		targets.add(source);
		var sourceProject = source.getProject();
		if (sourceProject == null || Engine.theApp == null || Engine.theApp.databaseObjectsManager == null) {
			return targets;
		}
		var sourceName = sourceProject.getName();
		for (var projectName : Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false)) {
			var project = Engine.theApp.databaseObjectsManager.getLoadedProjectByName(projectName);
			if (project == null || project == sourceProject || project.getFlowEngine() == null) {
				continue;
			}
			if (referencesProject(project, sourceName, new HashSet<>())) {
				targets.add(project.getFlowEngine());
			}
		}
		return targets;
	}

	private static boolean referencesProject(Project project, String sourceName, Set<String> visited) {
		if (project == null || !visited.add(project.getName())) {
			return false;
		}
		for (var reference : project.getReferenceList()) {
			if (!(reference instanceof ProjectSchemaReference projectReference)) {
				continue;
			}
			var referencedName = projectReference.getParser().getProjectName();
			if (referencedName == null || referencedName.isBlank()) {
				continue;
			}
			if (sourceName.equals(referencedName)) {
				return true;
			}
			var referenced = Engine.theApp.databaseObjectsManager.getLoadedProjectByName(referencedName);
			if (referencesProject(referenced, sourceName, visited)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isFrontendSourcePath(String sourcePath) {
		if (sourcePath == null || sourcePath.isBlank()) {
			return false;
		}
		var path = sourcePath.replace('\\', '/');
		return (path.startsWith("libs/flow/frontbuilder/") || path.contains("/libs/flow/frontbuilder/"))
				&& (path.endsWith(".flow.svelte") || path.endsWith(".flow.css")
						|| path.endsWith(".front.json") || path.endsWith(".uiblock.json"));
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

	private static String sourceRelativePath(FlowVirtualObject fvo) {
		var relativePath = sourceValue(fvo, "sourceRelativePath");
		if (!relativePath.isBlank()) {
			return normalizeSourcePath(relativePath);
		}
		var sourcePath = sourcePath(fvo);
		Project project = fvo.getProject();
		if (sourcePath.isBlank() || project == null || project.getDirFile() == null) {
			return "";
		}
		try {
			return normalizeSourcePath(project.getDirFile().toPath().toAbsolutePath().normalize()
					.relativize(new File(sourcePath).toPath().toAbsolutePath().normalize()).toString());
		} catch (Exception e) {
			return "";
		}
	}

	private static String normalizeSourcePath(String path) {
		var normalized = path == null ? "" : path.replace('\\', '/').trim();
		while (normalized.startsWith("./")) {
			normalized = normalized.substring(2);
		}
		return normalized;
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

	private static FrontendSiblingInsertion frontendSiblingInsertion(FlowVirtualObject fvo, String position) {
		if (!"before".equals(position) && !"after".equals(position)) {
			return null;
		}
		var path = mutationPath(fvo);
		var arrayPath = parentArrayPath(path);
		if (arrayPath == null) {
			return null;
		}
		var index = "before".equals(position) ? arrayIndex(path) : arrayIndex(path) + 1;
		return new FrontendSiblingInsertion(arrayPath, index);
	}

	private static class FrontendSiblingInsertion {
		private final String path;
		private final int index;

		private FrontendSiblingInsertion(String path, int index) {
			this.path = path;
			this.index = Math.max(0, index);
		}
	}

	private static String parentCollectionPath(String path) {
		var arrayPath = parentArrayPath(path);
		if (arrayPath != null) {
			return arrayPath;
		}
		var index = path.lastIndexOf('.');
		return index <= 0 ? null : path.substring(0, index);
	}

	private static int collectionIndex(FlowVirtualObject fvo) {
		var path = mutationPath(fvo);
		if (parentArrayPath(path) != null) {
			return arrayIndex(path);
		}
		var parent = fvo.getParent();
		if (parent == null) {
			return 0;
		}
		try {
			var children = parent.getDatabaseObjectChildren();
			for (var i = 0; i < children.size(); i++) {
				var child = children.get(i);
				if (child instanceof FlowVirtualObject other
						&& other.getVirtualPath().equals(fvo.getVirtualPath())
						&& other.getVirtualKind().equals(fvo.getVirtualKind())
						&& other.getVirtualType().equals(fvo.getVirtualType())) {
					return i;
				}
			}
		} catch (Exception e) {
			Engine.logStudio.trace("Unable to compute Flow virtual collection index", e);
		}
		return 0;
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
