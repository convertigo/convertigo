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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.MySimpleBeanInfo;
import com.twinsoft.convertigo.beans.core.TransactionWithVariables;
import com.twinsoft.convertigo.beans.transactions.AbstractHttpTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.AbstractCouchDbTransaction;
import com.twinsoft.convertigo.beans.transactions.couchdb.CouchVariable;
import com.twinsoft.convertigo.beans.transactions.couchdb.ICouchParametersExtra;
import com.twinsoft.convertigo.beans.variables.RequestableHttpVariable;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.CouchParam;
import com.twinsoft.convertigo.engine.enums.DynamicHttpVariable;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * The variables the Eclipse Studio offers to add to a transaction: the dynamic variables of an HTTP
 * transaction (body, URI, content type, custom header, POST or GET variables...) and the parameters of a
 * CouchDB transaction.
 * <ul>
 * <li>id: the transaction</li>
 * <li>apply: missing to list the variables, else {names: the variables to have, customs: [{variable, name,
 * value}] the custom variables to add}; an HTTP transaction removes its dynamic variables left out</li>
 * </ul>
 */
@ServiceDefinition(name = "Variables", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Variables extends JSonService {

	/**
	 * @return whether the object has variables to offer
	 */
	static boolean handles(DatabaseObject dbo) {
		return dbo instanceof AbstractHttpTransaction || dbo instanceof AbstractCouchDbTransaction;
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var dbo = id == null ? null : Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
		if (!handles(dbo)) {
			throw new ServiceException("The object " + id + " has no variables to add.");
		}
		response.put("id", id);
		var apply = request.getParameter("apply");
		if (apply == null) {
			response.put("options", dbo instanceof AbstractHttpTransaction http ? httpOptions(http) : couchOptions((AbstractCouchDbTransaction) dbo));
			response.put("removable", dbo instanceof AbstractHttpTransaction);
			return;
		}
		var choice = new JSONObject(apply);
		var names = new ArrayList<String>();
		var array = choice.optJSONArray("names");
		for (int i = 0; array != null && i < array.length(); i++) {
			names.add(array.getString(i));
		}
		var transaction = (TransactionWithVariables) dbo;
		var before = variableNames(transaction);
		if (dbo instanceof AbstractHttpTransaction http) {
			applyHttp(http, names, choice.optJSONArray("customs"));
		} else {
			applyCouch((AbstractCouchDbTransaction) dbo, names);
		}
		var after = variableNames(transaction);
		response.put("done", true);
		response.put("changed", !before.equals(after));
	}

	private static List<String> variableNames(TransactionWithVariables transaction) {
		var names = new ArrayList<String>();
		for (var variable : transaction.getVariablesList()) {
			names.add(variable.getName());
		}
		return names;
	}

	private static JSONArray httpOptions(AbstractHttpTransaction transaction) throws Exception {
		var options = new JSONArray();
		for (var variable : DynamicHttpVariable.values()) {
			if (variable.can(transaction)) {
				options.put(new JSONObject()
						.put("name", variable.name())
						.put("label", variable.display())
						.put("description", text(variable.description()))
						.put("group", variable.prefix() == null ? "Dynamic variables" : "Custom variables")
						.put("custom", variable.prefix() != null)
						.put("checked", variable.prefix() == null && transaction.getVariable(variable.name()) != null));
			}
		}
		return options;
	}

	private static void applyHttp(AbstractHttpTransaction transaction, List<String> names, JSONArray customs) throws Exception {
		for (var variable : DynamicHttpVariable.values()) {
			if (!variable.can(transaction) || variable.prefix() != null) {
				continue;
			}
			var existing = transaction.getVariable(variable.name());
			if (names.contains(variable.name()) && existing == null) {
				var added = new RequestableHttpVariable();
				added.setName(variable.name());
				if (variable == DynamicHttpVariable.__body) {
					added.setHttpMethod("POST");
				}
				added.bNew = true;
				transaction.addVariable(added);
				transaction.hasChanged = true;
			} else if (!names.contains(variable.name()) && existing instanceof RequestableVariable removed) {
				transaction.removeVariable(removed);
				transaction.hasChanged = true;
			}
		}
		for (int i = 0; customs != null && i < customs.length(); i++) {
			var custom = customs.getJSONObject(i);
			var text = custom.optString("name", "");
			DynamicHttpVariable variable;
			try {
				variable = DynamicHttpVariable.valueOf(custom.optString("variable"));
			} catch (Exception e) {
				continue;
			}
			if (text.isBlank() || variable.prefix() == null || !variable.can(transaction)) {
				continue;
			}
			var name = variable.prefix() + text;
			var normalized = StringUtils.normalize(name);
			var added = new RequestableHttpVariable();
			added.setName(normalized);
			if (!name.equals(normalized)) {
				added.setHttpName(text);
			}
			if (variable == DynamicHttpVariable.__POST_) {
				added.setHttpMethod("POST");
			}
			var value = custom.optString("value", "");
			if (!value.isEmpty()) {
				added.setValueOrNull(value);
			}
			added.bNew = true;
			transaction.addVariable(added);
			transaction.hasChanged = true;
		}
	}

	private static JSONArray couchOptions(AbstractCouchDbTransaction transaction) throws Exception {
		// by name, an extra variable replacing the parameter it tells multi-valued
		Map<String, JSONObject> options = new LinkedHashMap<>();
		var parentClass = transaction.getParent() == null ? "" : transaction.getParent().getClass().getCanonicalName();
		for (var property : CachedIntrospector.getBeanInfo(transaction).getPropertyDescriptors()) {
			var name = property.getName();
			if ((name.startsWith("q_") || name.startsWith("p_"))
					&& !parentClass.equals(property.getValue(MySimpleBeanInfo.BLACK_LIST_PARENT_CLASS))) {
				options.put(name, couchOption(transaction, name, property.getShortDescription(),
						name.startsWith("q_") ? "Query parameters" : "Parameters", false));
			}
		}
		if (transaction instanceof ICouchParametersExtra extra) {
			for (var variable : extra.getCouchParametersExtra()) {
				var name = variable.getVariableName();
				options.put(name, couchOption(transaction, name, variable.getVariableDescription(),
						name.startsWith("q_") ? "Query parameters" : "Data", variable.isMultiValued()));
			}
		}
		return new JSONArray(options.values());
	}

	private static JSONObject couchOption(AbstractCouchDbTransaction transaction, String name, String description,
			String group, boolean multiValued) throws Exception {
		return new JSONObject()
				.put("name", name)
				.put("label", couchVariableName(name))
				.put("description", text(description))
				.put("group", group)
				.put("multiValued", multiValued)
				.put("checked", transaction.getVariable(couchVariableName(name)) != null);
	}

	private static void applyCouch(AbstractCouchDbTransaction transaction, List<String> names) throws Exception {
		var selected = new ArrayList<CouchVariable>();
		var options = couchOptions(transaction);
		for (int i = 0; i < options.length(); i++) {
			var option = options.getJSONObject(i);
			if (names.contains(option.getString("name")) && !option.getBoolean("checked")) {
				selected.add(new CouchVariable(option.getString("name"), option.getString("description"),
						option.getBoolean("multiValued")));
			}
		}
		// as the Eclipse Studio, the CouchDB transactions only add their variables
		transaction.createVariables(selected);
	}

	private static String couchVariableName(String name) {
		return name.startsWith("p_") || name.startsWith("q_") ? CouchParam.prefix + name.substring(2) : name;
	}

	/**
	 * @return the first part of a description, without its HTML
	 */
	private static String text(String description) {
		if (description == null) {
			return "";
		}
		var index = description.indexOf('|');
		var text = index < 0 ? description : description.substring(0, index);
		return text.replaceAll("<[^>]+>", "").trim();
	}
}
