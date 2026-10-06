/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;

/**
 * The whitelist shared by the domain and both Studio adapters. The objects of a project share the tags of that
 * project, so one tag can mark back-end and front-end objects of the same functional area.
 */
public final class TagPolicy {
	private TagPolicy() {}
	/** The kinds of project objects, back end then front end, as both Studios name them. */
	private static final Map<Class<? extends DatabaseObject>, String> OBJECT_KINDS = new LinkedHashMap<>();
	static {
		OBJECT_KINDS.put(Sequence.class, "Sequence");
		OBJECT_KINDS.put(Transaction.class, "Transaction");
		OBJECT_KINDS.put(PageComponent.class, "Page");
		OBJECT_KINDS.put(UISharedComponent.class, "Shared component");
		OBJECT_KINDS.put(UIActionStack.class, "Shared action");
	}

	public static boolean supports(TagManager.Scope scope, DatabaseObject dbo) {
		return dbo != null && scope == scope(dbo);
	}
	public static boolean supports(DatabaseObject dbo) {
		return scope(dbo) != null;
	}
	public static TagManager.Scope scope(DatabaseObject dbo) {
		return dbo instanceof Project ? TagManager.Scope.workspaceProjects : kind(dbo) != null ? TagManager.Scope.projectObjects : null;
	}
	/** The kind of a project object taking the tags of its project, or null. */
	public static String kind(DatabaseObject dbo) {
		for (var kind : OBJECT_KINDS.entrySet()) if (kind.getKey().isInstance(dbo)) return kind.getValue();
		return null;
	}
	/** The objects of a project taking its tags, by kind then by name, a transaction after the name of its connector. */
	public static List<DatabaseObject> objects(Project project) {
		var objects = new ArrayList<DatabaseObject>(project.getSequencesList());
		for (var connector : project.getConnectorsList()) objects.addAll(connector.getTransactionsList());
		var mobile = project.getMobileApplication();
		if (mobile != null && mobile.getApplicationComponent() instanceof ApplicationComponent application) {
			objects.addAll(application.getPageComponentList());
			objects.addAll(application.getSharedComponentList());
			objects.addAll(application.getSharedActionList());
		}
		var kinds = new ArrayList<>(OBJECT_KINDS.values());
		objects.sort(Comparator.comparing((DatabaseObject dbo) -> kinds.indexOf(kind(dbo))).thenComparing(TagPolicy::label, String.CASE_INSENSITIVE_ORDER));
		return objects;
	}
	/** The name of a project object as both Studios list it: a transaction is named after its connector. */
	public static String label(DatabaseObject dbo) {
		return dbo instanceof Transaction && dbo.getParent() != null ? dbo.getParent().getName() + " › " + dbo.getName() : dbo.getName();
	}
	public static void require(TagManager.Scope scope, DatabaseObject dbo) throws IOException {
		if (!supports(scope, dbo)) throw new IOException("Tags are only supported on "
				+ (scope == TagManager.Scope.projectObjects ? "sequences, transactions, pages, shared components and shared actions" : "projects")
				+ "; unsupported target: " + dbo.getFullQName());
	}
}
