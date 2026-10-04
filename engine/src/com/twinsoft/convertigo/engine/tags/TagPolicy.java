/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.tags;

import java.io.IOException;
import java.util.Map;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;

/** The v1 whitelist is shared by the domain and both Studio adapters. */
public final class TagPolicy {
	private TagPolicy() {}
	private static final Map<TagManager.Scope, Class<? extends DatabaseObject>> TYPES = Map.of(
			TagManager.Scope.workspaceProjects, Project.class,
			TagManager.Scope.projectObjects, Sequence.class);

	public static boolean supports(TagManager.Scope scope, DatabaseObject dbo) {
		return dbo != null && TYPES.get(scope).isInstance(dbo);
	}
	public static boolean supports(DatabaseObject dbo) {
		return TYPES.values().stream().anyMatch(type -> type.isInstance(dbo));
	}
	public static TagManager.Scope scope(DatabaseObject dbo) {
		return TYPES.keySet().stream().filter(scope -> supports(scope, dbo)).findFirst().orElse(null);
	}
	public static void require(TagManager.Scope scope, DatabaseObject dbo) throws IOException {
		if (!supports(scope, dbo)) throw new IOException("Tags are only supported on "
				+ (scope == TagManager.Scope.projectObjects ? "sequences" : "projects")
				+ "; unsupported target: " + dbo.getFullQName());
	}
}
