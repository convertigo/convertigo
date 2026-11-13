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

package com.twinsoft.convertigo.engine.studio;

import java.util.function.Function;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.MobileComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UISharedComponent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.events.AdminEventBus;

/**
 * Shows live, in the web Studio, the objects an agent of the Assistant creates or changes, as the reveal mode of the
 * Assistant shows them in the Project Explorer of the Eclipse Studio. The web Studios that follow the agent select the
 * object in their tree, show the Flow of its sequence, or the preview of its NGX application on its page.
 * <p>
 * The MCP of the Assistant calls it through Rhino when its reveal mode is on and the Studio is not the Eclipse one,
 * which keeps its own reveal: {@code Packages.com.twinsoft.convertigo.engine.studio.StudioReveal.reveal(qname)}, or
 * {@code revealChange(qname, details)} to tell the change it made, which the web Studio shows in its activity feed of
 * the agent. An engine without this class reveals nothing. The entry points have no overloads, for Rhino, and answer a
 * JSON object as a string.
 */
public final class StudioReveal {
	/** the topic of the Admin events the web Studio follows */
	public static final String TOPIC = "studio.reveal";

	/** the changes an agent tells, as the web Studio shows them */
	static final String[] CHANGES = { "created", "changed", "renamed", "moved", "deleted" };

	public static final String PUBLISHED = "published";
	public static final String NOT_FOUND = "not_found";
	public static final String UNAVAILABLE = "unavailable";

	private StudioReveal() {
	}

	/**
	 * Reveals an object in the web Studios that follow the agent.
	 *
	 * @param qname the object, by its QName (Project.Sequence.Step), as the MCP names it, or its full QName, the id of
	 *        the tree of the web Studio
	 * @return a JSON object: status (published; not_found for an object not loaded; unavailable when the engine has no
	 *         web Studio), message, and once published, the event: project, id (the full QName), kind, sequence, page,
	 *         application, and listening (whether a web Studio of this engine receives it)
	 */
	public static String reveal(String qname) {
		return revealChange(qname, null);
	}

	/**
	 * Reveals an object an agent has just changed, telling the change, which the web Studio shows in the activity feed
	 * of the agent and as the light of the object in its tree. Without details, as {@link #reveal(String)}: the web
	 * Studio infers the change.
	 *
	 * @param qname the object, as for {@link #reveal(String)}; for a deletion, the parent of the object removed, or the
	 *        object removed itself, whose nearest loaded parent is revealed then
	 * @param details a JSON object as a string, all its keys optional: change (created, changed, renamed, moved or
	 *        deleted), previous (the QName of the object before a rename or a move, or the QName of the object
	 *        removed), tool (the tool of the MCP), summary (what it did, in a few words) and touched (an array of the
	 *        QNames of the other objects the same tool call changed); or only the change, as "created"
	 * @return as {@link #reveal(String)}, the event carrying the details given: change, previous, tool, summary and
	 *         touched (the full QNames of the objects found)
	 */
	public static String revealChange(String qname, String details) {
		var name = qname == null ? "" : qname.trim();
		if (!Engine.isWebStudioAllowed()) {
			return result(UNAVAILABLE, "This engine has no web Studio.", null);
		}
		var change = changeOf(details, StudioReveal::loaded);
		var databaseObject = loaded(name);
		if (databaseObject == null && "deleted".equals(change.optString("change"))) {
			// the object removed: its parent shows where it was
			databaseObject = nearestLoaded(name, StudioReveal::loaded);
			if (databaseObject != null && !change.has("previous")) {
				try {
					change.put("previous", name);
				} catch (JSONException e) {
					// a string key and value
				}
			}
		}
		if (databaseObject == null) {
			return result(NOT_FOUND, "No loaded object named " + name + ".", null);
		}
		return publish(databaseObject, change);
	}

	/**
	 * @param qname a QName, short or full
	 * @return the loaded object of the QName, null for none
	 */
	private static DatabaseObject loaded(String qname) {
		try {
			if (qname != null && !qname.isEmpty() && Engine.theApp != null
					&& Engine.theApp.databaseObjectsManager != null) {
				return Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
			}
		} catch (Exception e) {
			// a project not loaded, or an object it does not have
		}
		return null;
	}

	/**
	 * @param qname the QName of an object removed
	 * @param lookup the loaded object of a QName, null for none
	 * @return its nearest loaded parent, its project at last, null for none
	 */
	static DatabaseObject nearestLoaded(String qname, Function<String, DatabaseObject> lookup) {
		var name = qname == null ? "" : qname.trim();
		for (var dot = name.lastIndexOf('.'); dot > 0; dot = name.lastIndexOf('.')) {
			name = name.substring(0, dot);
			var databaseObject = lookup.apply(name);
			if (databaseObject != null) {
				return databaseObject;
			}
		}
		return null;
	}

	/**
	 * @param details the details of a change, as {@link #revealChange(String, String)} takes them
	 * @param lookup the loaded object of a QName, null for none, which names the objects touched by their full QNames
	 * @return the keys of the event telling the change, only the valid ones
	 */
	static JSONObject changeOf(String details, Function<String, DatabaseObject> lookup) {
		var change = new JSONObject();
		var text = details == null ? "" : details.trim();
		if (text.isEmpty()) {
			return change;
		}
		try {
			var given = text.startsWith("{") ? new JSONObject(text) : new JSONObject().put("change", text);
			var kind = given.optString("change", "").trim().toLowerCase();
			for (var known : CHANGES) {
				if (known.equals(kind)) {
					change.put("change", kind);
				}
			}
			putText(change, given, "previous", 400);
			putText(change, given, "tool", 80);
			putText(change, given, "summary", 200);
			var touched = given.optJSONArray("touched");
			if (touched != null) {
				var ids = new JSONArray();
				for (int i = 0; i < touched.length() && ids.length() < 50; i++) {
					var databaseObject = lookup.apply(touched.optString(i, "").trim());
					if (databaseObject != null) {
						ids.put(databaseObject.getFullQName());
					}
				}
				if (ids.length() > 0) {
					change.put("touched", ids);
				}
			}
		} catch (JSONException e) {
			if (Engine.logStudio != null) {
				Engine.logStudio.debug("(StudioReveal) Ignored the details of a change: " + text, e);
			}
		}
		return change;
	}

	private static void putText(JSONObject change, JSONObject given, String key, int max) throws JSONException {
		var value = given.optString(key, "").trim();
		if (!value.isEmpty()) {
			change.put(key, value.length() > max ? value.substring(0, max) : value);
		}
	}

	/**
	 * Reveals the NGX application of a project, as the MCP opens its builder: the web Studios that follow the agent
	 * show its preview.
	 *
	 * @param projectName the project
	 * @return as {@link #reveal(String)}, not_found for a project without an NGX application
	 */
	public static String revealApplication(String projectName) {
		var name = projectName == null ? "" : projectName.trim();
		if (!Engine.isWebStudioAllowed()) {
			return result(UNAVAILABLE, "This engine has no web Studio.", null);
		}
		DatabaseObject application = null;
		try {
			var project = name.isEmpty() || Engine.theApp == null || Engine.theApp.databaseObjectsManager == null ? null
					: Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name);
			if (project != null && project.getMobileApplication() != null
					&& project.getMobileApplication().getApplicationComponent() instanceof ApplicationComponent app) {
				application = app;
			}
		} catch (Exception e) {
			application = null;
		}
		if (application == null) {
			return result(NOT_FOUND, "The project " + name + " has no NGX application.", null);
		}
		return publish(application);
	}

	/**
	 * Publishes the reveal of an object.
	 *
	 * @return as {@link #reveal(String)}
	 */
	static String publish(DatabaseObject databaseObject) {
		return publish(databaseObject, null);
	}

	/**
	 * Publishes the reveal of an object, with the change an agent made to it.
	 *
	 * @param change the keys telling the change, from {@link #changeOf(String, Function)}, null for none
	 * @return as {@link #reveal(String)}
	 */
	static String publish(DatabaseObject databaseObject, JSONObject change) {
		try {
			var payload = describe(databaseObject);
			if (change != null) {
				for (var keys = change.keys(); keys.hasNext();) {
					var key = String.valueOf(keys.next());
					payload.put(key, change.get(key));
				}
			}
			AdminEventBus.publish(TOPIC, payload);
			return result(PUBLISHED, "", payload);
		} catch (Exception e) {
			if (Engine.logStudio != null) {
				Engine.logStudio.debug("(StudioReveal) Unable to reveal " + databaseObject.getQName(), e);
			}
			return result(NOT_FOUND, "Unable to reveal " + databaseObject.getQName() + ": " + e.getMessage(), null);
		}
	}

	/**
	 * @return what the web Studio needs to show an object: its project, its id (the full QName, as its tree names it),
	 *         its kind (project, connector, transaction, sequence, sequence.child, ngx.application, ngx.page,
	 *         ngx.shared, ngx.component or object), and the full QNames of the sequence, the NGX page and the NGX
	 *         application holding it, empty for none
	 */
	static JSONObject describe(DatabaseObject databaseObject) throws JSONException {
		var sequence = "";
		var page = "";
		var application = "";
		var shared = false;
		for (var current = databaseObject; current != null; current = current.getParent()) {
			if (sequence.isEmpty() && current instanceof Sequence) {
				sequence = current.getFullQName();
			}
			if (page.isEmpty() && current instanceof PageComponent) {
				page = current.getFullQName();
			}
			if (application.isEmpty() && current instanceof ApplicationComponent) {
				application = current.getFullQName();
			}
			shared |= current instanceof UISharedComponent;
		}
		String kind;
		if (databaseObject instanceof Project) {
			kind = "project";
		} else if (databaseObject instanceof Sequence) {
			kind = "sequence";
		} else if (!sequence.isEmpty()) {
			kind = "sequence.child";
		} else if (databaseObject instanceof ApplicationComponent) {
			kind = "ngx.application";
		} else if (databaseObject instanceof PageComponent) {
			kind = "ngx.page";
		} else if (databaseObject instanceof UISharedComponent) {
			kind = "ngx.shared";
		} else if (databaseObject instanceof MobileComponent && !application.isEmpty()) {
			// a component of a page, of the menus, of a shared component or a shared action
			kind = shared && page.isEmpty() ? "ngx.shared" : "ngx.component";
		} else if (databaseObject instanceof Connector) {
			kind = "connector";
		} else if (databaseObject instanceof Transaction) {
			kind = "transaction";
		} else {
			kind = "object";
		}
		var project = databaseObject.getProject();
		return new JSONObject()
				.put("project", project == null ? "" : project.getName())
				.put("id", databaseObject.getFullQName())
				.put("kind", kind)
				.put("sequence", sequence)
				.put("page", page)
				.put("application", application);
	}

	private static String result(String status, String message, JSONObject event) {
		try {
			var result = new JSONObject().put("status", status).put("message", message);
			if (event != null) {
				for (var keys = event.keys(); keys.hasNext();) {
					var key = String.valueOf(keys.next());
					result.put(key, event.get(key));
				}
				result.put("listening", AdminEventBus.hasSubscribers(TOPIC));
			}
			return result.toString();
		} catch (JSONException e) {
			return "{\"status\":\"" + status + "\"}";
		}
	}
}
