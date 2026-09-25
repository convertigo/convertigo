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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.studio.dbo.CreateStub;
import com.twinsoft.convertigo.engine.admin.services.studio.project.ImportWsReference;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;

/**
 * The actions of the tree menu of the Eclipse Studio on the objects outside the Flows, for the web Studio:
 * each item follows the menu protocol of the Flow actions (id, label, description, group, enabled, icon),
 * and running it changes the object as the Eclipse action does.
 */
public class ObjectActions {
	private static final String GROUP = "Object";

	private ObjectActions() {
	}

	/**
	 * @return whether the web Studio shows the actions of this class for the object, not the Flow ones
	 */
	static boolean handles(DatabaseObject dbo) {
		return dbo != null && !(dbo instanceof FlowVirtualObject) && !(dbo instanceof Project);
	}

	static JSONArray items(DatabaseObject dbo) throws Exception {
		var items = new JSONArray();
		if (!handles(dbo)) {
			return items;
		}
		if (dbo instanceof Connector connector) {
			add(items, "object.defaultConnector", "Set as default connector",
					"Use this connector when a request names none.", !connector.isDefault, "mdi:check");
		}
		if (dbo instanceof Transaction transaction) {
			add(items, "object.defaultTransaction", "Set as default transaction",
					"Run this transaction when a request names none.", !transaction.isDefault, "mdi:check");
		}
		if (dbo instanceof PageComponent page) {
			add(items, "object.rootPage", "Set as root page", "Open the application on this page.", !page.isRoot,
					"mdi:check");
		}
		if (dbo instanceof SequenceStep || dbo instanceof TransactionStep) {
			add(items, "object.importVariables", "Import the variables of the requestable",
					"Add the variables of the called sequence or transaction to this step.", true, "mdi:import");
		} else if (dbo instanceof TestCase && dbo.getParent() instanceof RequestableObject) {
			add(items, "object.importVariables", "Import the variables of the requestable",
					"Add the variables of the sequence or transaction to this test case.", true, "mdi:import");
		}
		if (dbo instanceof WebServiceReference || dbo instanceof RestServiceReference) {
			add(items, "object.updateReference", "Update the web service",
					"Read the definition of the web service again and update its connector and transactions.", true,
					"mdi:reload");
		}
		if (dbo instanceof RequestableObject && (dbo instanceof Sequence || dbo instanceof Transaction)) {
			add(items, "object.emptyStub", "Create an empty stub",
					"Save an empty response as the stub answering the requests run from stub.", true,
					"mdi:file-outline");
		}
		for (var target : ChangeTo.targets(dbo).entrySet()) {
			add(items, "object.changeTo:" + target.getKey().getSimpleName(), "Change to " + target.getValue(),
					"Replace this object by a " + target.getValue() + " keeping its properties and children.", true,
					"mdi:swap-horizontal", "Change to");
		}
		if (dbo.getParent() instanceof IContainerOrdered) {
			add(items, "object.moveUp", "Move up", "Move this object before the previous one.", true,
					"mdi:arrow-up-bold-outline");
			add(items, "object.moveDown", "Move down", "Move this object after the next one.", true,
					"mdi:arrow-down-bold-outline");
		}
		return items;
	}

	static JSONObject run(DatabaseObject dbo, String actionId) throws Exception {
		if (!handles(dbo)) {
			return result(false, "This action is not available for this object.");
		}
		var parent = dbo.getParent();
		if (actionId.startsWith("object.changeTo:")) {
			var replacement = ChangeTo.run(dbo, actionId.substring("object.changeTo:".length()));
			return result(true, "").put("changed", true).put("refresh", true).put("selectedId", replacement.getQName(true));
		}
		switch (actionId) {
		case "object.defaultConnector" -> {
			if (!(dbo instanceof Connector connector)) {
				return result(false, "This object is not a connector.");
			}
			connector.getProject().setDefaultConnector(connector);
			changed(connector.getProject());
		}
		case "object.defaultTransaction" -> {
			if (!(dbo instanceof Transaction transaction) || !(parent instanceof Connector connector)) {
				return result(false, "This object is not a transaction.");
			}
			connector.setDefaultTransaction(transaction);
			changed(connector);
		}
		case "object.rootPage" -> {
			if (!(dbo instanceof PageComponent page) || !(parent instanceof ApplicationComponent application)) {
				return result(false, "This object is not a page of an application.");
			}
			application.setRootPage(page);
			application.updateSourceFiles();
			changed(application);
		}
		case "object.importVariables" -> {
			if (dbo instanceof SequenceStep step) {
				step.importVariableDefinition();
			} else if (dbo instanceof TransactionStep step) {
				step.importVariableDefinition();
			} else if (dbo instanceof TestCase testCase && parent instanceof RequestableObject requestable) {
				testCase.importRequestableVariables(requestable);
			} else {
				return result(false, "This object has no variables to import.");
			}
			if (!dbo.hasChanged) {
				return result(true, "The variables were already imported.").put("changed", false);
			}
		}
		case "object.updateReference" -> {
			if (!(dbo instanceof RemoteFileReference reference)
					|| !(dbo instanceof WebServiceReference || dbo instanceof RestServiceReference)) {
				return result(false, "This object is not a web service reference.");
			}
			var connector = ImportWsReference.importInto(dbo.getProject(), reference, null);
			return result(true, connector == null ? "The web service is up to date."
					: "The connector " + connector.getName() + " is updated.").put("changed", true).put("refresh", true);
		}
		case "object.emptyStub" -> {
			if (!(dbo instanceof RequestableObject requestable)) {
				return result(false, "Only a sequence or a transaction has a stub.");
			}
			var file = CreateStub.write(requestable, CreateStub.emptyStub(requestable), false);
			if (file == null) {
				return result(false, "The stub stubs/" + requestable.getDefaultStubFileName() + " already exists.");
			}
			return result(true, "The empty stub stubs/" + file.getName() + " is saved.").put("changed", false);
		}
		case "object.moveUp", "object.moveDown" -> {
			if (!(parent instanceof IContainerOrdered container)) {
				return result(false, "This object cannot move.");
			}
			if ("object.moveUp".equals(actionId)) {
				container.increasePriority(dbo);
			} else {
				container.decreasePriority(dbo);
			}
			if (!parent.hasChanged) {
				return result(true, "The object is already at this end.").put("changed", false);
			}
			BuilderUtils.dboMoved(parent, parent, dbo);
		}
		default -> {
			return result(false, "Unknown action " + actionId);
		}
		}
		Engine.logStudio.debug("(ObjectActions) " + actionId + " on " + dbo.getQName());
		return result(true, "").put("changed", true).put("refresh", true);
	}

	private static void changed(DatabaseObject dbo) {
		dbo.hasChanged = true;
	}

	private static JSONObject result(boolean ok, String message) throws Exception {
		return new JSONObject().put("ok", ok).put("message", message);
	}

	private static void add(JSONArray items, String id, String label, String description, boolean enabled,
			String icon) throws Exception {
		add(items, id, label, description, enabled, icon, GROUP);
	}

	private static void add(JSONArray items, String id, String label, String description, boolean enabled,
			String icon, String group) throws Exception {
		items.put(new JSONObject()
				.put("id", id)
				.put("label", label)
				.put("description", description)
				.put("group", group)
				.put("enabled", enabled)
				.put("payload", new JSONObject())
				.put("confirm", "")
				.put("icon", icon));
	}
}
