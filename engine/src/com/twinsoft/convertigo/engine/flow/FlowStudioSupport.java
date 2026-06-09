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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;

public class FlowStudioSupport {

	private static final String FLOW_BLOCK_TYPE = "FlowBlock";
	private static final String FLOW_BLOCK_ID_PREFIX = "flowblock:";
	private static final String FLOW_ICON = "/com/twinsoft/convertigo/beans/sequences/images/genericsequence_color_32x32.png";

	private FlowStudioSupport() {
	}

	public static boolean isFlowPaletteTarget(DatabaseObject dbo) {
		return flowRoot(dbo) != null;
	}

	public static boolean canAddBlock(DatabaseObject targetDbo, String position, String blockName) {
		var flow = flowRoot(targetDbo);
		if (flow == null || !isWritablePaletteTarget(targetDbo) || blockName == null || blockName.isBlank()) {
			return false;
		}
		try {
			return insertionFor(flow, targetDbo, position, blockName, new JSONObject()) != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static JSONArray paletteCategories(DatabaseObject targetDbo) throws Exception {
		var flow = flowRoot(targetDbo);
		var categories = new JSONArray();
		if (flow == null || !isWritablePaletteTarget(targetDbo)) {
			return categories;
		}

		var grouped = new LinkedHashMap<String, JSONObject>();
		var blocks = new FlowEngineBridge().catalog(flow).optJSONArray("blocks");
		if (blocks != null) {
			for (int i = 0; i < blocks.length(); i++) {
				var block = blocks.optJSONObject(i);
				if (block != null) {
					var category = grouped.computeIfAbsent(originCategoryName(block.optString("origin", "")), name -> {
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

	private static String originCategoryName(String origin) {
		return switch (origin == null ? "" : origin) {
		case "core" -> "Flow blocks - core engine";
		case "project" -> "Flow blocks - project";
		default -> "Flow blocks - libraries";
		};
	}

	public static boolean isFlowPaletteData(JSONObject transfer) {
		var data = transfer == null ? null : transfer.optJSONObject("data");
		return "paletteData".equals(transfer == null ? "" : transfer.optString("type"))
				&& FLOW_BLOCK_TYPE.equals(data == null ? "" : data.optString("type"));
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
		var flow = flowRoot(targetDbo);
		var data = transfer == null ? null : transfer.optJSONObject("data");
		if (flow == null || data == null) {
			return new JSONObject().put("done", false);
		}

		var blockName = data.optString("block", data.optString("classname", ""));
		if (blockName.isBlank()) {
			return new JSONObject().put("done", false);
		}

		var node = new JSONObject()
				.put("id", nextNodeId(flow, blockName))
				.put("block", blockName);
		addDefaultProperties(flow, blockName, node);
		var insertion = insertionFor(flow, targetDbo, position, blockName, node);
		if (insertion == null) {
			return new JSONObject().put("done", false);
		}

		var response = new FlowEngineBridge().applyMutation(flow, insertion);
		var done = response.optBoolean("ok", false);
		return new JSONObject()
				.put("done", done)
				.put("id", done ? flow.getFullQName() : "")
				.put("error", done ? JSONObject.NULL : response.opt("error"));
	}

	private static JSONObject paletteItem(JSONObject block) throws Exception {
		var name = block.optString("name", "");
		var shortDescription = firstNonBlank(block, "shortDescription", "description");
		var longDescription = firstNonBlank(block, "longDescription");
		var propertiesDescription = propertiesDescription(block.optJSONObject("props"));
		var rawDescription = shortDescription;
		if (!longDescription.isBlank()) {
			rawDescription += "|" + longDescription;
		}
		var item = new JSONObject()
				.put("type", FLOW_BLOCK_TYPE)
				.put("id", FLOW_BLOCK_ID_PREFIX + name)
				.put("name", name)
				.put("classname", name)
				.put("block", name)
				.put("description", rawDescription)
				.put("shortDescriptionHtml", html(shortDescription))
				.put("shortDescriptionText", shortDescription)
				.put("longDescriptionHtml", html(longDescription))
				.put("longDescriptionText", longDescription)
				.put("propertiesDescriptionHtml", propertiesDescription)
				.put("icon", studioIcon(block))
				.put("builtin", "core".equals(block.optString("origin", "")))
				.put("additional", "project".equals(block.optString("origin", "")));
		var iconify = firstNonBlank(block, "iconify");
		if (!iconify.isBlank()) {
			item.put("iconify", iconify);
		}
		return item;
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

	private static void addDefaultProperties(Flow flow, String blockName, JSONObject node) throws Exception {
		var block = blockDescriptor(flow, blockName);
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

	private static Flow flowRoot(DatabaseObject dbo) {
		var current = dbo;
		while (current != null) {
			if (current instanceof Flow flow) {
				return flow;
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
			return "folder".equals(kind) || "slot".equals(kind) || "node".equals(kind);
		}
		return false;
	}

	private static JSONObject insertionFor(Flow flow, DatabaseObject targetDbo, String position, String blockName, JSONObject node) throws Exception {
		position = position == null || position.isBlank() ? "inside" : position;
		if (targetDbo instanceof Flow) {
			return mutationForArray("nodes", position.equals("first") ? 0 : -1, node);
		}
		if (!(targetDbo instanceof FlowVirtualObject fvo)) {
			return null;
		}

		var path = fvo.getVirtualPath();
		if (path.isBlank() || path.startsWith("catalog")) {
			return null;
		}
		if ("inside".equals(position)) {
			if ("folder".equals(fvo.getVirtualKind()) || "slot".equals(fvo.getVirtualKind())) {
				return mutationForArray(path, -1, node);
			}
			if ("node".equals(fvo.getVirtualKind())) {
				return mutationForNodeSlot(flow, fvo, blockName, node);
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

	private static JSONObject mutationForNodeSlot(Flow flow, FlowVirtualObject fvo, String blockName, JSONObject node) throws Exception {
		var slot = firstSlotName(flow, fvo.getVirtualType());
		var slotPath = fvo.getVirtualPath() + "." + slot;
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

	private static String firstSlotName(Flow flow, String blockName) throws Exception {
		var block = blockDescriptor(flow, blockName);
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
		return "nodes";
	}

	private static JSONObject blockDescriptor(Flow flow, String blockName) throws Exception {
		var blocks = new FlowEngineBridge().catalog(flow).optJSONArray("blocks");
		if (blocks != null) {
			for (int i = 0; i < blocks.length(); i++) {
				var block = blocks.optJSONObject(i);
				if (block != null && blockName.equals(block.optString("name", ""))) {
					return block;
				}
			}
		}
		return null;
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

	private static String nextNodeId(Flow flow, String blockName) {
		var base = blockName.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+", "_");
		base = base.replaceAll("^_+|_+$", "");
		if (base.isBlank()) {
			base = "node";
		}
		var used = new HashSet<String>();
		collectNodeIds(flow.getFlowVirtualChildren(), used);
		var candidate = base;
		for (int i = 2; used.contains(candidate); i++) {
			candidate = base + i;
		}
		return candidate;
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
