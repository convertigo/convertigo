/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.tags.TagManager;

/** Thin tree adapter for the same real-collection projection used by the web Studio. */
final class TagTreeProjection {
	boolean enabled;
	private final Map<String, TagTreeObject> rows = new HashMap<>();
	private final Map<TreeParent, java.util.Set<String>> childrenByParent = new java.util.IdentityHashMap<>();
	Object[] children(TreeParent presentation, TreeParent real, boolean workspace) {
		var children = real.getChildren();
		if (workspace && "true".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_HIDE_LIB_PROJECTS))) children.removeIf(child -> child.getName().startsWith("lib_"));
		if (!enabled) { rows.clear(); childrenByParent.clear(); return children.toArray(); }
		Map<String, TreeObject> targets = new LinkedHashMap<>(); List<TreeObject> others = new ArrayList<>();
		for (TreeObject child : children) {
			if (child instanceof DatabaseObjectTreeObject dbo && dbo.getObject().isOriginal()
					&& com.twinsoft.convertigo.engine.tags.TagPolicy.supports(workspace ? TagManager.Scope.workspaceProjects : TagManager.Scope.projectObjects, dbo.getObject())) targets.put(dbo.getObject().getFullQName(), child);
			else if (workspace && child instanceof UnloadedProjectTreeObject) targets.put(child.getName(), child);
			else others.add(child);
		}
		if (targets.isEmpty()) return retain(presentation, occurrences(presentation, real, children));
		try {
			DatabaseObject parent = real.getObject() instanceof DatabaseObject dbo ? dbo : real.getParent() != null && real.getParent().getObject() instanceof DatabaseObject dbo ? dbo : null;
			String project = workspace ? null : parent == null ? null : parent.getProject().getName();
			if (!workspace && project == null) return retain(presentation, occurrences(presentation, real, children));
			String collection = workspace ? "workspace" : (parent.getFullQName() + ":" + real.getName());
			var projection = TagManager.get().collection(workspace ? TagManager.Scope.workspaceProjects : TagManager.Scope.projectObjects, project, collection, new ArrayList<>(targets.keySet()));
			if (projection.path("groups").isEmpty()) return retain(presentation, occurrences(presentation, real, children));
			List<TreeObject> result = new ArrayList<>();
			String prefix = presentation instanceof TagTreeObject occurrence ? occurrence.rowId + "/" : "";
			for (var group : projection.path("groups")) {
				String key = prefix + group.path("rowId").asText();
				TagTreeObject folder = rows.computeIfAbsent(key, id -> new TagTreeObject(real.viewer, id, presentation, null, group));
				folder.parent = presentation; folder.group = group; result.add(folder);
				List<TreeObject> members = new ArrayList<>();
				for (var member : group.path("members")) {
					String row = prefix + member.path("rowId").asText(); TreeObject target = targets.get(member.path("targetId").asText());
					TagTreeObject occurrence = rows.computeIfAbsent(row, id -> new TagTreeObject(real.viewer, id, folder, target, null));
					occurrence.parent = folder; occurrence.target = target; members.add(occurrence);
				}
				folder.replaceChildren(members);
			}
			result.addAll(java.util.Arrays.asList(occurrences(presentation, real, others))); return retain(presentation, result.toArray());
		} catch (Exception e) { ConvertigoPlugin.logException(e, "Unable to project tags in this collection"); return retain(presentation, occurrences(presentation, real, children)); }
	}
	/** Discard removed occurrences and their cached descendants without scanning other projects. */
	private Object[] retain(TreeParent parent, Object[] children) {
		var current = new java.util.HashSet<String>();
		for (Object child : children) if (child instanceof TagTreeObject row) current.add(row.rowId);
		var previous = childrenByParent.put(parent, current);
		if (previous != null) for (String id : previous) if (!current.contains(id)) forget(id);
		return children;
	}
	private void forget(String id) {
		TagTreeObject row = rows.remove(id);
		if (row == null) return;
		var descendants = childrenByParent.remove(row);
		if (descendants != null) for (String child : descendants) forget(child);
		for (TreeObject child : row.getChildren()) if (child instanceof TagTreeObject occurrence) forget(occurrence.rowId);
	}
	List<TagTreeObject> occurrencesOf(TreeObject target) {
		return rows.values().stream().filter(row -> row.target == target).toList();
	}
	private TreeObject[] occurrences(TreeParent presentation, TreeParent real, List<? extends TreeObject> children) {
		if (!(presentation instanceof TagTreeObject parent)) return children.toArray(TreeObject[]::new);
		List<TreeObject> result = new ArrayList<>();
		for (int index = 0; index < children.size(); index++) {
			TreeObject child = children.get(index);
			String key = parent.rowId + "/technical:" + child.getClass().getSimpleName() + ":" + child.getName() + ":" + index;
			TagTreeObject row = rows.computeIfAbsent(key, id -> new TagTreeObject(real.viewer, id, presentation, child, null));
			row.parent = presentation; row.target = child; result.add(row);
		}
		return result.toArray(TreeObject[]::new);
	}
}
