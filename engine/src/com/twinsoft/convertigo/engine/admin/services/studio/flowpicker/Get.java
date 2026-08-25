/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.flowpicker;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.flow.FlowEngineBridge;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

@ServiceDefinition(name = "Get", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Get extends JSonService {

	private record PickerTarget(Flow flow, FlowEngine flowEngine, String qName) {
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var property = request.getParameter("propertyName");
		var value = request.getParameter("value");
		if (id == null || id.isBlank()) {
			throw new ServiceException("missing id parameter");
		}
		if (property == null || property.isBlank()) {
			throw new ServiceException("missing propertyName parameter");
		}

		var resolved = FlowStudioSupport.resolveTreeObject(id);
		if (!(resolved instanceof FlowVirtualObject object)) {
			throw new ServiceException("Flow picker target is not a Flow virtual object");
		}
		var target = findTarget(object);
		if (target == null || target.flowEngine() == null) {
			throw new ServiceException("Unable to resolve parent Flow or FlowEngine");
		}

		var bridge = new FlowEngineBridge();
		var editor = target.flow() == null
				? bridge.propertyEditor(target.flowEngine())
				: bridge.propertyEditor(target.flow());
		var html = editor.optString("html", "");
		if (html.isBlank()) {
			throw new ServiceException("Flow property editor is not available");
		}

		var info = pickerInfo(bridge, target, object, property);
		var definitions = info.optJSONObject("propertyDefinitions");
		var propertyDefinition = definitions == null ? new JSONObject() : definitions.optJSONObject(property);
		if (propertyDefinition == null) {
			propertyDefinition = new JSONObject();
		}
		var pickerDefinitions = new JSONObject().put(property, propertyDefinition);
		var pickerInfo = pickerInfoForProperty(info, property, pickerDefinitions);
		if (value == null) {
			var current = object.getDefinitionProperty(property);
			value = current == null ? "" : String.valueOf(current);
		}
		var pickerDefinition = new JSONObject().put(property,
				valueOrNull(object.getDefinitionProperty(property)));

		var state = new JSONObject()
				.put("mode", "picker")
				.put("singleProperty", true)
				.put("property", property)
				.put("propertyDefinition", propertyDefinition)
				.put("virtualKind", object.getVirtualKind())
				.put("virtualType", object.getVirtualType())
				.put("virtualPath", object.getVirtualPath())
				.put("summary", object.getSummary())
				.put("flowQName", target.qName())
				.put("definition", pickerDefinition)
				.put("info", pickerInfo)
				.put("applied", new JSONObject().put("property", property).put("value", value));

		var requests = new JSONObject();
		if (needsContext(pickerDefinitions) && !isFrontendSource(object)) {
			var context = context(bridge, target, object, property);
			state.put("context", context);
			requests.put("context", new JSONObject().put("ok", true).put("context", context));
		}
		if (hasKind(pickerDefinitions, "requestable")) {
			var requestables = target.flow() == null
					? bridge.requestables(target.flowEngine())
					: bridge.requestables(target.flow());
			state.put("requestables", requestables);
			requests.put("requestables", new JSONObject().put("ok", true).put("requestables", requestables));
		}
		if (hasKind(pickerDefinitions, "icon")) {
			var options = new JSONObject().put("provider", "mdi").put("limit", 500);
			var icons = target.flow() == null
					? bridge.icons(target.flowEngine(), options)
					: bridge.icons(target.flow(), options);
			requests.put("icons", icons);
		}

		response.put("html", html);
		response.put("state", state);
		response.put("requests", requests);
	}

	static JSONObject pickerInfoForProperty(JSONObject info, String property, JSONObject definitions) throws Exception {
		var filtered = info == null ? new JSONObject() : new JSONObject(info.toString());
		filtered.put("propertyDefinitions", definitions == null ? new JSONObject() : definitions);
		filtered.put("propertyOrder", new JSONArray().put(property));
		return filtered;
	}

	private static JSONObject pickerInfo(FlowEngineBridge bridge, PickerTarget target, FlowVirtualObject object,
			String property) throws Exception {
		var info = valueOrObject(object.getVirtualInfoObject());
		if (!isFrontendSource(object) || !requiresFrontendProjection(info, property)) {
			return info;
		}
		var virtualPath = object.getVirtualPath();
		var tree = bridge.authoringTree(target.flowEngine(), new JSONObject()
				.put("surface", "frontend")
				.put("builder", frontendBuilder(virtualPath))
				.put("focusPath", virtualPath)
				.put("detail", "full")
				.put("maxDepth", 0)
				.put("property", property)
				.put("includeBindings", true)
				.put("includeFrontendCatalog", false)
				.put("includeFlowCatalog", false));
		var focused = findNode(tree.optJSONArray("children"), virtualPath);
		var enriched = focused == null ? null : objectValue(focused.opt("info"));
		return enriched == null || enriched.length() == 0 ? info : enriched;
	}

	static boolean requiresFrontendProjection(JSONObject info, String property) throws Exception {
		var definitions = info == null ? null : info.optJSONObject("propertyDefinitions");
		var definition = definitions == null ? null : definitions.optJSONObject(property);
		return definition != null && needsContext(new JSONObject().put(property, definition));
	}

	private static JSONObject findNode(JSONArray children, String virtualPath) {
		if (children == null) {
			return null;
		}
		for (int i = 0; i < children.length(); i++) {
			var child = children.optJSONObject(i);
			if (child == null) {
				continue;
			}
			if (virtualPath.equals(child.optString("path", ""))) {
				return child;
			}
			var nested = findNode(child.optJSONArray("children"), virtualPath);
			if (nested != null) {
				return nested;
			}
		}
		return null;
	}

	private static JSONObject objectValue(Object value) {
		if (value instanceof JSONObject object) {
			return object;
		}
		if (value instanceof String text && !text.isBlank()) {
			try {
				return new JSONObject(text);
			} catch (Exception e) {
			}
		}
		return null;
	}

	private static String frontendBuilder(String virtualPath) {
		var parts = virtualPath == null ? new String[0] : virtualPath.split("\\.", 3);
		return parts.length >= 2 && "frontends".equals(parts[0]) ? parts[1] : "svelte";
	}

	private static JSONObject context(FlowEngineBridge bridge, PickerTarget target, FlowVirtualObject object,
			String property) throws Exception {
		var request = new JSONObject()
				.put("include", new JSONArray().put("input").put("config").put("local").put("current").put("result"))
				.put("property", property)
				.put("detail", "normal")
				.put("position", "before");
		if (target.flow() != null && !isFlowImplementationSource(object)) {
			request.put("path", object.getVirtualPath());
			return bridge.context(target.flow(), request);
		}

		var info = object.getVirtualInfoObject();
		var sourcePath = infoString(info, "sourcePath");
		var sourceMutationPath = infoString(info, "sourceMutationPath");
		if (sourcePath.isBlank() || sourceMutationPath.isBlank()) {
			return new JSONObject();
		}
		request.put("flowSource", Files.readString(Path.of(sourcePath)));
		request.put("path", sourceMutationPath);
		var sourceBlockName = infoString(info, "sourceBlockName");
		if (!sourceBlockName.isBlank()) {
			request.put("sourceBlockName", sourceBlockName);
		}
		return bridge.context(target.flowEngine(), request);
	}

	private static PickerTarget findTarget(DatabaseObject object) {
		for (var current = object; current != null; current = current.getParent()) {
			if (current instanceof Flow flow) {
				var project = flow.getProject();
				return new PickerTarget(flow, project == null ? null : project.getFlowEngine(), flow.getQName());
			}
			if (current instanceof FlowEngine flowEngine) {
				return new PickerTarget(null, flowEngine, flowEngine.getQName());
			}
		}
		return null;
	}

	private static boolean needsContext(JSONObject definitions) {
		return hasKind(definitions, "binding") || hasKind(definitions, "expression")
				|| hasKind(definitions, "path") || hasKind(definitions, "template")
				|| hasKind(definitions, "value");
	}

	private static boolean hasKind(JSONObject definitions, String expected) {
		if (definitions == null) {
			return false;
		}
		for (var keys = definitions.keys(); keys.hasNext();) {
			var definition = definitions.optJSONObject(String.valueOf(keys.next()));
			if (definition != null && (expected.equals(definition.optString("kind", ""))
					|| expected.equals(definition.optString("type", "")))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isFlowImplementationSource(FlowVirtualObject object) {
		var info = object.getVirtualInfoObject();
		return info != null && info.optBoolean("flowImplementation", false)
				&& !info.optString("sourcePath", "").isBlank()
				&& !info.optString("sourceMutationPath", "").isBlank();
	}

	private static boolean isFrontendSource(FlowVirtualObject object) {
		var info = object.getVirtualInfoObject();
		if (info == null) {
			return false;
		}
		if (info.optBoolean("frontendModel", false)) {
			return true;
		}
		var path = info.optString("sourcePath", "").replace('\\', '/');
		return path.contains("/libs/flow/frontbuilder/")
				&& (path.endsWith(".flow.svelte") || path.endsWith(".flow.css")
						|| path.endsWith(".front.json") || path.endsWith(".uiblock.json"));
	}

	private static String infoString(JSONObject info, String key) {
		return info == null ? "" : info.optString(key, "");
	}

	private static JSONObject valueOrObject(JSONObject object) {
		return object == null ? new JSONObject() : object;
	}

	private static Object valueOrNull(Object value) {
		return value == null ? JSONObject.NULL : value;
	}
}
