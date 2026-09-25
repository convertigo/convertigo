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
import com.twinsoft.convertigo.beans.couchdb.DesignDocument;
import com.twinsoft.convertigo.beans.couchdb.DesignDocumentFunction;
import com.twinsoft.convertigo.beans.couchdb.DesignDocumentView;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.beans.core.IVariableContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.UrlMappingOperation;
import com.twinsoft.convertigo.beans.core.UrlMappingParameter;
import com.twinsoft.convertigo.beans.references.RemoteFileReference;
import com.twinsoft.convertigo.beans.references.RestServiceReference;
import com.twinsoft.convertigo.beans.references.WebServiceReference;
import com.twinsoft.convertigo.beans.core.TestCase;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.flow.FlowVirtualObject;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.PageComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIUseShared;
import com.twinsoft.convertigo.beans.rest.FormParameter;
import com.twinsoft.convertigo.beans.rest.PostOperation;
import com.twinsoft.convertigo.beans.rest.PutOperation;
import com.twinsoft.convertigo.beans.rest.QueryParameter;
import com.twinsoft.convertigo.beans.steps.AttributeStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
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
			add(items, "object.exportVariables", "Export the variables to the sequence",
					"Add the variables of this step the sequence does not have to the sequence.", true, "mdi:export");
		} else if (dbo instanceof UrlMappingOperation operation) {
			add(items, "object.importParameters", "Import the variables of the requestable as parameters",
					"Add a parameter to this operation for each variable of its target sequence or transaction.",
					!operation.getTargetRequestable().isEmpty(), "mdi:import");
		} else if (dbo instanceof TestCase && dbo.getParent() instanceof RequestableObject) {
			add(items, "object.importVariables", "Import the variables of the requestable",
					"Add the variables of the sequence or transaction to this test case.", true, "mdi:import");
		}
		if (dbo instanceof Step step && !(dbo instanceof AttributeStep) && !(dbo instanceof XMLAttributeStep)) {
			var output = !step.isOutput();
			add(items, "object.output", "Output " + output, "Set the output of this step to " + output + ".", true,
					output ? "mdi:eye-outline" : "mdi:eye-off-outline");
			if (dbo instanceof StepWithExpressions) {
				add(items, "object.outputRecursively", "Output " + output + " recursively",
						"Set the output of this step and of all its steps to " + output + ".", true,
						output ? "mdi:eye-outline" : "mdi:eye-off-outline");
			}
		}
		if (dbo instanceof DesignDocument designDocument) {
			add(items, "object.createView", "Create a view", "Add a view with a map function to this design document.",
					true, "mdi:plus");
			add(items, "object.createFunction:" + DesignDocumentFunction.FILTERS, "Create a filter",
					"Add a filter function choosing the documents to replicate or to listen to.", true, "mdi:plus");
			add(items, "object.createFunction:" + DesignDocumentFunction.UPDATES, "Create an update function",
					"Add an update function changing a document on the server.", true, "mdi:plus");
			add(items, "object.createFunction:" + DesignDocumentFunction.VALIDATE, "Create the validate function",
					"Add the function validating each document written to the database.",
					!designDocument.getJSONObject().has(DesignDocumentFunction.VALIDATE), "mdi:plus");
		}
		if (dbo instanceof DesignDocumentView view) {
			if (view.hasReduce()) {
				add(items, "object.removeReduce", "Remove the reduce function", "Keep only the map function of this view.",
						true, "mdi:minus");
			} else {
				add(items, "object.addReduce", "Add a reduce function", "Reduce the rows the map function of this view emits.",
						true, "mdi:plus");
			}
		}
		if (NgxI18n.handles(dbo)) {
			add(items, "object.i18n:true", "Enable I18n recursively",
					"Translate the texts and the automatic menu items under this component.", true, "mdi:translate");
			add(items, "object.i18n:false", "Disable I18n recursively",
					"Stop translating the texts and the automatic menu items under this component.", true, "mdi:translate-off");
		}
		if (dbo instanceof ApplicationComponent) {
			add(items, "object.translations", "Create the translations files…",
					"Write the texts of the application in the translations files of its languages.", true, "mdi:translate")
					.put("clientAction", "dialog.translations");
		}
		if (NgxImports.importsVariables(dbo)) {
			add(items, "object.importNgxVariables", "Import variables from the targeted object",
					"Add a variable for each variable of the called sequence, invoked shared action or used shared component.",
					true, "mdi:import");
		}
		if (NgxImports.importsEvents(dbo)) {
			add(items, "object.importNgxEvents", "Import events from the targeted object",
					"Add an event for each event of the used shared component.", true, "mdi:import");
		}
		if (Variables.handles(dbo)) {
			add(items, "object.variables", dbo instanceof AbstractHttpTransaction ? "Add or remove dynamic variables…" : "Add variables…",
					"Choose the variables of this transaction among the ones it can use.", true, "mdi:variable")
					.put("clientAction", "dialog.variables");
		}
		if (dbo instanceof WebServiceReference || dbo instanceof RestServiceReference) {
			add(items, "object.updateReference", "Update the web service",
					"Read the definition of the web service again and update its connector and transactions.", true,
					"mdi:reload");
		}
		if (dbo instanceof RequestableObject && (dbo instanceof Sequence || dbo instanceof Transaction)
				&& !(dbo instanceof SiteClipperTransaction)) {
			add(items, "object.emptyStub", "Create an empty stub",
					"Save an empty response as the stub answering the requests run from stub.", true,
					"mdi:file-outline");
		}
		for (var target : ChangeTo.targets(dbo).entrySet()) {
			add(items, "object.changeTo:" + target.getKey().getSimpleName(), "Change to " + target.getValue(),
					"Replace this object by a " + target.getValue() + " keeping its properties and children.", true,
					"mdi:swap-horizontal", "Change to").put("confirm", ChangeTo.confirm(dbo, target.getKey()));
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
		if (actionId.startsWith("object.i18n:")) {
			var changed = NgxI18n.i18n(dbo, Boolean.parseBoolean(actionId.substring("object.i18n:".length())));
			if (changed == 0) {
				return result(true, "The texts are already set so.").put("changed", false);
			}
			return result(true, changed + " text" + (changed > 1 ? "s are" : " is") + " changed.").put("changed", true).put("refresh", true);
		}
		if (actionId.startsWith("object.createFunction:")) {
			if (!(dbo instanceof DesignDocument designDocument)) {
				return result(false, "This object is not a design document.");
			}
			var kind = actionId.substring("object.createFunction:".length());
			var function = designDocument.getFunction(kind, designDocument.addFunction(kind));
			return result(true, "").put("changed", true).put("refresh", true)
					.put("selectedId", function == null ? dbo.getFullQName() : function.getFullQName());
		}
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
			var variables = dbo instanceof IVariableContainer container ? container.numberOfVariables() : 0;
			if (dbo instanceof SequenceStep step) {
				step.importVariableDefinition();
			} else if (dbo instanceof TransactionStep step) {
				step.importVariableDefinition();
			} else if (dbo instanceof TestCase testCase && parent instanceof RequestableObject requestable) {
				testCase.importRequestableVariables(requestable);
			} else {
				return result(false, "This object has no variables to import.");
			}
			if (((IVariableContainer) dbo).numberOfVariables() == variables) {
				return result(true, "The variables were already imported.").put("changed", false);
			}
		}
		case "object.importNgxVariables", "object.importNgxEvents" -> {
			var added = "object.importNgxEvents".equals(actionId) && dbo instanceof UIUseShared useShared
					? NgxImports.importEvents(useShared)
					: NgxImports.importVariables(dbo);
			if (added == 0) {
				return result(true, "The component already has them.").put("changed", false);
			}
		}
		case "object.createView" -> {
			if (!(dbo instanceof DesignDocument designDocument)) {
				return result(false, "This object is not a design document.");
			}
			var view = designDocument.getView(designDocument.addView());
			return result(true, "").put("changed", true).put("refresh", true)
					.put("selectedId", view == null ? dbo.getFullQName() : view.getFullQName());
		}
		case "object.addReduce", "object.removeReduce" -> {
			if (!(dbo instanceof DesignDocumentView view)) {
				return result(false, "This object is not a view.");
			}
			view.setDynamicProperty("reduce", "object.addReduce".equals(actionId) ? DesignDocument.DEFAULT_REDUCE : "");
		}
		case "object.exportVariables" -> {
			if (!(dbo instanceof RequestableStep step)) {
				return result(false, "This object has no variables to export.");
			}
			var variables = step.getSequence().numberOfVariables();
			step.exportVariableDefinition();
			if (step.getSequence().numberOfVariables() == variables) {
				return result(true, "The sequence already has the variables of this step.").put("changed", false);
			}
		}
		case "object.importParameters" -> {
			if (!(dbo instanceof UrlMappingOperation operation)) {
				return result(false, "This object is not an operation.");
			}
			var added = importParameters(operation);
			if (added == 0) {
				return result(true, "The operation already has a parameter for each variable.").put("changed", false);
			}
		}
		case "object.output", "object.outputRecursively" -> {
			if (!(dbo instanceof Step step)) {
				return result(false, "This object is not a step.");
			}
			output(step, !step.isOutput(), "object.outputRecursively".equals(actionId));
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

	private static void output(Step step, boolean output, boolean recursively) {
		step.setOutput(output);
		step.hasChanged = true;
		if (recursively && step instanceof StepWithExpressions container) {
			for (var child : container.getSteps()) {
				output(child, output, true);
			}
		}
	}

	/**
	 * Adds to an operation a parameter for each variable of its target requestable it has not, as the Eclipse
	 * Studio: a form parameter for a POST or PUT operation, a query parameter otherwise.
	 *
	 * @return the number of parameters added
	 */
	private static int importParameters(UrlMappingOperation operation) throws Exception {
		var target = operation.getTargetRequestable().split("\\.");
		var project = target.length > 1 ? Engine.theApp.databaseObjectsManager.getOriginalProjectByName(target[0]) : null;
		if (project == null) {
			throw new EngineException("The operation has no target requestable: select one first.");
		}
		RequestableObject requestable = target.length == 2 ? project.getSequenceByName(target[1])
				: project.getConnectorByName(target[1]).getTransactionByName(target[2]);
		var added = 0;
		if (requestable instanceof IVariableContainer container) {
			for (var variable : container.getVariables()) {
				var name = variable.getName();
				if (hasParameter(operation, name)) {
					continue;
				}
				UrlMappingParameter parameter = operation instanceof PostOperation || operation instanceof PutOperation
						? new FormParameter()
						: new QueryParameter();
				parameter.setName(name);
				parameter.setComment(variable.getComment());
				parameter.setArray(false);
				parameter.setExposed(variable instanceof RequestableVariable requestableVariable && requestableVariable.isWsdl());
				parameter.setMultiValued(variable.isMultiValued());
				parameter.setRequired(variable.isRequired());
				parameter.setValueOrNull(variable.isMultiValued() ? null : variable.getValueOrNull());
				parameter.setMappedVariableName(name);
				parameter.bNew = true;
				parameter.hasChanged = true;
				operation.add(parameter);
				operation.hasChanged = true;
				added++;
			}
		}
		return added;
	}

	private static boolean hasParameter(UrlMappingOperation operation, String name) {
		try {
			return operation.getParameterByName(name) != null;
		} catch (Exception e) {
			return false;
		}
	}

	private static void changed(DatabaseObject dbo) {
		dbo.hasChanged = true;
	}

	private static JSONObject result(boolean ok, String message) throws Exception {
		return new JSONObject().put("ok", ok).put("message", message);
	}

	private static JSONObject add(JSONArray items, String id, String label, String description, boolean enabled,
			String icon) throws Exception {
		return add(items, id, label, description, enabled, icon, GROUP);
	}

	private static JSONObject add(JSONArray items, String id, String label, String description, boolean enabled,
			String icon, String group) throws Exception {
		var item = new JSONObject()
				.put("id", id)
				.put("label", label)
				.put("description", description)
				.put("group", group)
				.put("enabled", enabled)
				.put("payload", new JSONObject())
				.put("confirm", "")
				.put("icon", icon);
		items.put(item);
		return item;
	}
}
