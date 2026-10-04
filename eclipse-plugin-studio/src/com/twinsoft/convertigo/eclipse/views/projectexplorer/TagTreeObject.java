/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import org.eclipse.jface.viewers.Viewer;
import com.fasterxml.jackson.databind.JsonNode;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

/** A presentation occurrence. The target retains its actual tree and DBO parents. */
public final class TagTreeObject extends TreeParent {
	public final String rowId;
	public TreeObject target;
	public JsonNode group;
	public TagTreeObject(Viewer viewer, String rowId, TreeParent presentationParent, TreeObject target, JsonNode group) {
		super(viewer, rowId); this.rowId = rowId; this.parent = presentationParent; this.target = target; this.group = group;
	}
	@Override public TreeObject check() { return target == null ? this : target.check(); }
	@Override public Object getObject() { return target == null ? super.getObject() : target.getObject(); }
	@Override public String getName() { return target == null ? group.path("label").asText() : target.getName(); }
	@Override public String toString() { return getName(); }
	/** The enclosing group already names this membership. Other memberships remain visible. */
	public String currentTagId() {
		return parent instanceof TagTreeObject folder && folder.target == null ? folder.group.path("tagId").asText() : "";
	}
	@Override public Object getAdapter(Class adapter) { return target == null ? super.getAdapter(adapter) : target.getAdapter(adapter); }
}
