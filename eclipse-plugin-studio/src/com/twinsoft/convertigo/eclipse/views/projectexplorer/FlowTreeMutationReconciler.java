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
 * MERCHANTABILITY  or  FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FlowVirtualObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public final class FlowTreeMutationReconciler {

	public record Selection(String key, String sourcePath, String mutationPath, String fallbackTreePath) {
		public static final Selection NONE = new Selection("", "", "", "");
	}

	private FlowTreeMutationReconciler() {
	}

	public static Selection selection(TreeObject treeObject) {
		return selection(treeObject, "");
	}

	public static Selection selection(TreeObject treeObject, String mutationPath) {
		if (treeObject instanceof DatabaseObjectTreeObject databaseTreeObject
				&& databaseTreeObject.getObject() instanceof FlowVirtualObject flowObject) {
			return new Selection(selectionKey(flowObject), flowObject.getSourcePath(),
					valueOrEmpty(mutationPath), treeObject.getPath());
		}
		return new Selection("", "", valueOrEmpty(mutationPath), treeObject == null ? "" : treeObject.getPath());
	}

	public static boolean reconcile(ProjectExplorerView explorerView, TreeObject context, JSONObject response,
			Selection selection) throws EngineException, IOException {
		if (explorerView == null || response == null || !response.optBoolean("projected", false)) {
			return false;
		}
		var sourcePath = response.optString("projectedSourcePath", "");
		var rootPath = response.optString("projectedRootPath", "");
		var projectedRoot = findProjectedRoot(context, sourcePath, rootPath);
		if (!(projectedRoot instanceof DatabaseObjectTreeObject projectedDatabaseTreeObject)) {
			Engine.logStudio.warn("Flow projected tree root was not found: sourcePath=" + sourcePath
					+ " rootPath=" + rootPath);
			return false;
		}
		var started = System.currentTimeMillis();
		if (projectedRoot instanceof FlowVirtualObjectTreeObject flowRoot) {
			var created = new HashSet<TreeObject>();
			reconcileProjectedChildren(flowRoot, flowRoot.getObject().getDatabaseObjectChildren(), created);
			explorerView.refreshProjectedFlowTreeObject(projectedRoot, created);
		} else {
			explorerView.forceReloadTreeObject(projectedRoot);
		}
		var effectiveSelection = selection == null ? Selection.NONE : selection;
		var responseSelectionSourcePath = response.optString("selectionSourcePath",
				effectiveSelection.sourcePath());
		TreeObject selected = findByVirtualPath(projectedRoot,
				response.optString("selectionVirtualPath", ""));
		if (selected == null) {
			selected = findByMutationPath(projectedRoot, responseSelectionSourcePath,
				response.optString("selectionMutationPath", ""));
		}
		if (selected == null) {
			selected = findBySourceId(projectedRoot, responseSelectionSourcePath,
					response.optString("selectionId", ""));
		}
		if (selected == null) {
			selected = findBySelectionKey(projectedRoot, effectiveSelection.key());
		}
		if (selected == null) {
			selected = findByMutationPath(projectedRoot, effectiveSelection.sourcePath(),
					effectiveSelection.mutationPath());
		}
		if (selected == null && projectedRoot instanceof TreeParent treeParent
				&& !effectiveSelection.fallbackTreePath().isBlank()) {
			selected = explorerView.findTreeObjectByPath(treeParent, effectiveSelection.fallbackTreePath());
		}
		if (selected == null) {
			selected = projectedDatabaseTreeObject;
		}
		explorerView.setSelectedTreeObject(selected);
		Engine.logStudio.info("Flow projected tree reconciled: root=" + projectedRoot.getPath()
				+ " selected=" + selected.getPath()
				+ " elapsedMs=" + (System.currentTimeMillis() - started));
		return true;
	}

	private static void reconcileProjectedChildren(FlowVirtualObjectTreeObject parent,
			Iterable<DatabaseObject> projectedChildren, Set<TreeObject> created) {
		Map<String, ArrayDeque<FlowVirtualObjectTreeObject>> existing = new HashMap<>();
		for (var child : parent.getChildren()) {
			if (child instanceof FlowVirtualObjectTreeObject flowChild) {
				existing.computeIfAbsent(selectionKey(flowChild.getObject()), key -> new ArrayDeque<>())
						.add(flowChild);
			}
		}
		var replacements = new ArrayList<TreeObject>();
		for (var projectedChild : projectedChildren) {
			if (!(projectedChild instanceof FlowVirtualObject flowObject)) {
				continue;
			}
			var candidates = existing.get(selectionKey(flowObject));
			var flowChild = candidates == null || candidates.isEmpty() ? null : candidates.removeFirst();
			if (flowChild == null) {
				flowChild = new FlowVirtualObjectTreeObject(parent.viewer, flowObject, false);
				created.add(flowChild);
			} else {
				flowChild.replaceFlowObject(flowObject);
			}
			reconcileProjectedChildren(flowChild, flowObject.getDatabaseObjectChildren(), created);
			replacements.add(flowChild);
		}
		parent.replaceChildren(replacements);
	}

	private static TreeObject findProjectedRoot(TreeObject context, String sourcePath, String rootPath) {
		for (var current = context; current != null; current = current.getParent()) {
			if (matchesProjectedRoot(current, sourcePath, rootPath)) {
				return current;
			}
		}
		var searchRoot = flowEngineTreeObject(context);
		return searchRoot == null ? null : findProjectedRootInTree(searchRoot, sourcePath, rootPath);
	}

	private static TreeParent flowEngineTreeObject(TreeObject context) {
		TreeParent fallback = null;
		for (var current = context; current != null; current = current.getParent()) {
			if (current instanceof TreeParent treeParent) {
				fallback = treeParent;
			}
			if (current instanceof DatabaseObjectTreeObject databaseTreeObject
					&& databaseTreeObject.getObject() instanceof FlowEngine) {
				return databaseTreeObject;
			}
		}
		return fallback;
	}

	private static TreeObject findProjectedRootInTree(TreeParent parent, String sourcePath, String rootPath) {
		if (matchesProjectedRoot(parent, sourcePath, rootPath)) {
			return parent;
		}
		for (var child : parent.getChildren()) {
			if (matchesProjectedRoot(child, sourcePath, rootPath)) {
				return child;
			}
			if (child instanceof TreeParent treeParent) {
				var found = findProjectedRootInTree(treeParent, sourcePath, rootPath);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static boolean matchesProjectedRoot(TreeObject treeObject, String sourcePath, String rootPath) {
		return treeObject instanceof DatabaseObjectTreeObject databaseTreeObject
				&& databaseTreeObject.getObject() instanceof FlowVirtualObject flowObject
				&& rootPath.equals(flowObject.getVirtualPath())
				&& (sourcePath.isBlank() || sourcePath.equals(flowObject.getSourcePath()));
	}

	private static TreeObject findBySelectionKey(TreeObject root, String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		if (root instanceof DatabaseObjectTreeObject databaseTreeObject
				&& databaseTreeObject.getObject() instanceof FlowVirtualObject flowObject
				&& key.equals(selectionKey(flowObject))) {
			return root;
		}
		if (root instanceof TreeParent treeParent) {
			for (var child : treeParent.getChildren()) {
				var found = findBySelectionKey(child, key);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static TreeObject findByVirtualPath(TreeObject root, String virtualPath) {
		if (virtualPath == null || virtualPath.isBlank()) {
			return null;
		}
		if (root instanceof DatabaseObjectTreeObject databaseTreeObject
				&& databaseTreeObject.getObject() instanceof FlowVirtualObject flowObject
				&& virtualPath.equals(flowObject.getVirtualPath())) {
			return root;
		}
		if (root instanceof TreeParent treeParent) {
			for (var child : treeParent.getChildren()) {
				var found = findByVirtualPath(child, virtualPath);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static TreeObject findByMutationPath(TreeObject root, String sourcePath, String mutationPath) {
		if (mutationPath == null || mutationPath.isBlank()) {
			return null;
		}
		if (root instanceof DatabaseObjectTreeObject databaseTreeObject
				&& databaseTreeObject.getObject() instanceof FlowVirtualObject flowObject
				&& mutationPath.equals(flowObject.getSourceMutationPath())
				&& (sourcePath == null || sourcePath.isBlank() || sourcePath.equals(flowObject.getSourcePath()))) {
			return root;
		}
		if (root instanceof TreeParent treeParent) {
			for (var child : treeParent.getChildren()) {
				var found = findByMutationPath(child, sourcePath, mutationPath);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static TreeObject findBySourceId(TreeObject root, String sourcePath, String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		if (root instanceof DatabaseObjectTreeObject databaseTreeObject
				&& databaseTreeObject.getObject() instanceof FlowVirtualObject flowObject) {
			var definition = flowObject.getDefinitionObject();
			if (definition != null && id.equals(definition.optString("id", ""))
					&& (sourcePath == null || sourcePath.isBlank() || sourcePath.equals(flowObject.getSourcePath()))) {
				return root;
			}
		}
		if (root instanceof TreeParent treeParent) {
			for (var child : treeParent.getChildren()) {
				var found = findBySourceId(child, sourcePath, id);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	private static String selectionKey(FlowVirtualObject object) {
		var definition = object.getDefinitionObject();
		var id = definition == null ? "" : definition.optString("id", "");
		if (!id.isBlank()) {
			return "id\u0000" + object.getSourcePath() + "\u0000" + object.getVirtualKind() + "\u0000"
					+ object.getVirtualType() + "\u0000" + id;
		}
		return "path\u0000" + object.getSourcePath() + "\u0000" + object.getVirtualPath() + "\u0000"
				+ object.getVirtualKind() + "\u0000" + object.getVirtualType();
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
}
