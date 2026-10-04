/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.admin.services.studio.tags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.tags.TagManager;

/** Maps the common projection onto tree DTOs, retaining folders and all real target IDs. */
public final class TreeProjection {
	private TreeProjection() {}
	public static JSONArray apply(String collection, JSONArray children) throws Exception {
		boolean workspace = collection == null;
		String project = workspace ? null : collection.split("[.:/]", 2)[0];
		var stable = new LinkedHashMap<String, JSONObject>(); var others = new JSONArray();
		for (int i = 0; i < children.length(); i++) {
			JSONObject node = children.getJSONObject(i); String id = node.optString("id");
			if (workspace) stable.put(id, node);
			else {
				int separator = id.lastIndexOf(':');
				boolean folder = separator >= 0 && FolderType.parse(id.substring(separator + 1)) != null;
				if (folder) {
					if (node.opt("children") instanceof JSONArray nested) node.put("children", apply(id, nested));
					others.put(node);
				} else if (!id.contains("/") && !id.isEmpty()) {
					var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
					if (dbo != null && dbo.getFullQName().equals(id) && dbo.getOriginal() == dbo
							&& com.twinsoft.convertigo.engine.tags.TagPolicy.supports(TagManager.Scope.projectObjects, dbo)) stable.put(id, node);
					else others.put(node);
				} else others.put(node);
			}
		}
		if (stable.isEmpty()) return others;
		var groups = TagManager.get().collection(workspace ? TagManager.Scope.workspaceProjects : TagManager.Scope.projectObjects,
				project, collection == null ? "workspace" : collection, new ArrayList<>(stable.keySet())).path("groups");
		if (groups.isEmpty()) return children;
		var result = new JSONArray();
		for (var group : groups) {
			JSONObject folder = new JSONObject(group.toString()); folder.put("id", group.path("rowId").asText()); folder.put("icon", "folder");
			JSONArray occurrences = new JSONArray();
			for (var member : group.path("members")) {
				JSONObject occurrence = new JSONObject(stable.get(member.path("targetId").asText()).toString());
				occurrence.put("rowId", member.path("rowId").asText()); occurrence.put("tagId", group.path("tagId").asText());
				occurrence.put("targetId", member.path("targetId").asText()); occurrences.put(occurrence);
			}
			folder.remove("members"); folder.put("children", occurrences); result.put(folder);
		}
		for (int i = 0; i < others.length(); i++) result.put(others.get(i));
		return result;
	}
}
