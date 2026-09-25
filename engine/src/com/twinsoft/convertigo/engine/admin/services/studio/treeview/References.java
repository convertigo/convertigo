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
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIActionStack;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicAction;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicInvoke;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

/**
 * The references of an object, as the References view of the Eclipse Studio shows them: the objects it
 * requires, grouped by the project that holds them, and the objects that use it, grouped by their
 * project. A reference is a call step, a URL mapping operation, a use of a shared component, an
 * invocation of a shared action, a call action of an application or a Javelin handler of a screen class.
 * <ul>
 * <li>id: the tree id of the object</li>
 * </ul>
 */
@ServiceDefinition(name = "References", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class References extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		if (id == null || id.isBlank()) {
			throw new ServiceException("missing id parameter");
		}
		var selection = Utils.getDbo(id);
		if (selection == null) {
			throw new ServiceException("The object " + id + " does not exist.");
		}

		var requires = new Section();
		var usedBy = new Section();

		// the objects the selection requires are referenced from inside it
		walk(selection, (source, target) -> requires.add(target.getProject(), target, source));

		// the objects that use the selection are in any project
		var dbom = Engine.theApp.databaseObjectsManager;
		for (var projectName : dbom.getAllProjectNamesList()) {
			Project project;
			try {
				project = dbom.getOriginalProjectByName(projectName);
			} catch (Exception e) {
				Engine.logAdmin.debug("(studio.treeview.References) cannot open the project " + projectName, e);
				continue;
			}
			if (project == null) {
				continue;
			}
			walk(project, (source, target) -> {
				if (isInside(target, selection)) {
					usedBy.add(source.getProject(), target, source);
				}
			});
			if (selection instanceof ScreenClass screenClass) {
				addJavelinHandlers(project, screenClass, usedBy);
			}
		}

		response.put("object", node(selection));
		response.put("requires", requires.toJson());
		response.put("usedBy", usedBy.toJson());
	}

	private interface ReferenceConsumer {
		void accept(DatabaseObject source, DatabaseObject target) throws Exception;
	}

	/**
	 * Walks the objects of a root and gives each reference with the object it resolves to.
	 */
	private static void walk(DatabaseObject root, ReferenceConsumer consumer) throws Exception {
		new WalkHelper() {

			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				var target = target(databaseObject);
				if (target != null) {
					consumer.accept(databaseObject, target);
				}
				super.walk(databaseObject);
			}
		}.init(root);
	}

	/**
	 * @return the object a reference resolves to, or null when the object references nothing or its
	 *         target does not exist
	 */
	private static DatabaseObject target(DatabaseObject source) {
		try {
			var ownProject = source.getProject() == null ? "" : source.getProject().getName();
			if (source instanceof SequenceStep step) {
				var project = project(orDefault(step.getProjectName(), ownProject));
				return project == null ? null : project.getSequenceByName(step.getSequenceName());
			}
			if (source instanceof TransactionStep step) {
				return requestable(orDefault(step.getProjectName(), ownProject), step.getConnectorName(),
						step.getTransactionName());
			}
			if (source instanceof UrlMappingOperation operation) {
				return requestable(operation.getTargetRequestable());
			}
			if (source instanceof UIUseShared useShared) {
				var parts = parts(useShared.getSharedComponentQName());
				if (parts == null) {
					return null;
				}
				var application = application(parts[0]);
				if (application != null) {
					for (var component : application.getSharedComponentList()) {
						if (parts[parts.length - 1].equals(component.getName())) {
							return component;
						}
					}
				}
				return null;
			}
			if (source instanceof UIDynamicInvoke invoke) {
				var parts = parts(invoke.getSharedActionQName());
				if (parts == null) {
					return null;
				}
				var application = application(parts[0]);
				if (application != null) {
					for (var stack : application.getSharedActionList()) {
						if (parts[parts.length - 1].equals(stack.getName())) {
							return stack;
						}
					}
				}
				return null;
			}
			if (source instanceof UIDynamicAction action && !(source instanceof UIDynamicInvoke)) {
				var ionBean = action.getIonBean();
				var property = ionBean == null ? null : ionBean.getProperty("requestable");
				return property == null ? null : requestable(normalize(property.getSmartValue()));
			}
		} catch (Exception e) {
			// a broken reference has no target
		}
		return null;
	}

	/**
	 * @return the target of a "project.sequence", "project.connector" or "project.connector.transaction"
	 *         reference
	 */
	private static DatabaseObject requestable(String reference) {
		var parts = parts(reference);
		if (parts == null) {
			return null;
		}
		if (parts.length == 2) {
			var project = project(parts[0]);
			if (project == null) {
				return null;
			}
			try {
				return project.getSequenceByName(parts[1]);
			} catch (Exception notSequence) {
				return requestable(parts[0], parts[1], null);
			}
		}
		return requestable(parts[0], parts[1], parts[2]);
	}

	/**
	 * @return the transaction of a connector, or the connector itself when the transaction is missing
	 */
	private static DatabaseObject requestable(String projectName, String connectorName, String transactionName) {
		var project = project(projectName);
		if (project == null) {
			return null;
		}
		Connector connector;
		try {
			connector = project.getConnectorByName(connectorName);
		} catch (Exception e) {
			return null;
		}
		if (transactionName != null && !transactionName.isEmpty()) {
			try {
				return connector.getTransactionByName(transactionName);
			} catch (Exception noTransaction) {
				// the connector is the target
			}
		}
		return connector;
	}

	private static Project project(String name) {
		try {
			return Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name);
		} catch (Exception e) {
			return null;
		}
	}

	private static ApplicationComponent application(String projectName) {
		var project = project(projectName);
		var mobileApplication = project == null ? null : project.getMobileApplication();
		return mobileApplication != null && mobileApplication.getApplicationComponent() instanceof ApplicationComponent application
				? application
				: null;
	}

	private static String orDefault(String value, String defaultValue) {
		return value == null || value.isEmpty() ? defaultValue : value;
	}

	private static String[] parts(String reference) {
		var normalized = normalize(reference);
		if (normalized == null) {
			return null;
		}
		var parts = normalized.split("\\.");
		return parts.length >= 2 ? parts : null;
	}

	/**
	 * Strips the mode of a smart value ("plain:", "script:") and its quotes.
	 */
	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		var reference = value.trim();
		var colon = reference.indexOf(':');
		if (colon >= 0) {
			reference = reference.substring(colon + 1).trim();
		}
		reference = reference.replaceAll("^['\"]|['\"]$", "");
		return reference.isEmpty() ? null : reference;
	}

	private static boolean isInside(DatabaseObject dbo, DatabaseObject ancestor) {
		for (var current = dbo; current != null; current = current.getParent()) {
			if (current == ancestor) {
				return true;
			}
		}
		return false;
	}

	private static void addJavelinHandlers(Project project, ScreenClass screenClass, Section usedBy) throws Exception {
		var name = screenClass.getName();
		new WalkHelper() {

			@Override
			protected void walk(DatabaseObject databaseObject) throws Exception {
				if (databaseObject instanceof JavelinTransaction transaction) {
					var handlers = transaction.handlers == null ? "" : transaction.handlers;
					if (handlers.contains("function on" + name + "Entry()") || handlers.contains("function on" + name + "Exit()")) {
						usedBy.add(project, screenClass, transaction);
					}
				}
				super.walk(databaseObject);
			}
		}.init(project);
	}

	private static JSONObject node(DatabaseObject dbo) throws Exception {
		return new JSONObject()
				.put("id", dbo.getQName(true))
				.put("name", dbo.getName())
				.put("type", dbo.getDatabaseType())
				.put("icon", "studio.dbo.GetIcon?iconPath=" + Get.iconPath(dbo));
	}

	/**
	 * The references of a section, grouped by project, each one once.
	 */
	private static class Section {
		private final Map<Project, JSONArray> entries = new LinkedHashMap<>();
		private final Set<String> keys = new HashSet<>();

		void add(Project project, DatabaseObject target, DatabaseObject source) throws Exception {
			if (project == null || target == null || source == null
					|| !keys.add(target.getFullQName() + "\u0000" + source.getFullQName())) {
				return;
			}
			entries.computeIfAbsent(project, key -> new JSONArray())
					.put(new JSONObject().put("target", node(target)).put("source", node(source)));
		}

		JSONArray toJson() throws Exception {
			var groups = new JSONArray();
			for (var entry : entries.entrySet()) {
				groups.put(new JSONObject().put("project", node(entry.getKey())).put("entries", entry.getValue()));
			}
			return groups;
		}
	}
}
