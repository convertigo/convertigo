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

package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.ws.commons.schema.constants.Constants;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.common.XMLVector;
import com.twinsoft.convertigo.beans.common.XmlQName;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.sequences.GenericSequence;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLCopyStep;
import com.twinsoft.convertigo.beans.transactions.SqlTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.enums.Accessibility;

/**
 * The design of an SQL connector, as its Design tab and its Test SQL connection button in the Eclipse
 * Studio: the tables, procedures and functions of the database become transactions, the tables with the
 * list, insert, select, update and delete ones, possibly each wrapped in a sequence.
 * <ul>
 * <li>id: the SQL connector</li>
 * <li>action: test, search (pattern, types: TABLE,PROCEDURE,FUNCTION), import (items: a JSON array of
 * name, type and specificName, cruds, override, accessibility, authenticated)</li>
 * </ul>
 */
@ServiceDefinition(name = "SqlDesign", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class SqlDesign extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if (!(Utils.getDbo(request.getParameter("id")) instanceof SqlConnector connector)) {
			throw new ServiceException("Only an SQL connector has a design.");
		}
		var thread = Thread.currentThread();
		var classLoader = thread.getContextClassLoader();
		try {
			// the JDBC drivers of the libraries of the project
			thread.setContextClassLoader(connector.getProject().getProjectClassLoader());
			switch (String.valueOf(request.getParameter("action"))) {
			case "test" -> {
				var tested = test(connector);
				for (var key : new String[] { "connected", "message", "error" }) {
					if (tested.has(key)) {
						response.put(key, tested.get(key));
					}
				}
			}
			case "search" -> search(connector, request, response);
			case "import" -> importItems(connector, request, response);
			default -> throw new ServiceException("Unknown action " + request.getParameter("action"));
			}
		} finally {
			thread.setContextClassLoader(classLoader);
		}
	}

	/**
	 * @return whether the connector connects, with a message or an error
	 */
	public static JSONObject test(SqlConnector connector) throws Exception {
		var response = new JSONObject();
		var thread = Thread.currentThread();
		var classLoader = thread.getContextClassLoader();
		try {
			thread.setContextClassLoader(connector.getProject().getProjectClassLoader());
			connector.open();
			response.put("connected", true);
			response.put("message", "Connection parameters are correct.");
		} catch (Exception e) {
			Engine.logBeans.error("Test connection failed! " + e.getMessage());
			response.put("connected", false);
			response.put("error", "Failed to connect to the database! " + e.getMessage());
		} finally {
			connector.close();
			thread.setContextClassLoader(classLoader);
		}
		return response;
	}

	private static void search(SqlConnector connector, HttpServletRequest request, JSONObject response) throws Exception {
		var pattern = request.getParameter("pattern") == null ? "%" : request.getParameter("pattern");
		var types = Arrays.asList(String.valueOf(request.getParameter("types")).split(","));
		var items = new JSONArray();
		try {
			var document = SqlConnector.executeSearch(connector, pattern, types);
			var nodes = document.getDocumentElement().getElementsByTagName("item");
			for (var i = 0; i < nodes.getLength(); i++) {
				var node = (Element) nodes.item(i);
				var name = (Element) node.getElementsByTagName("NAME").item(0);
				items.put(new JSONObject()
						.put("name", name.getTextContent())
						.put("specificName", name.getAttribute("specific_name"))
						.put("type", text(node, "TYPE"))
						.put("remarks", text(node, "REMARKS")));
			}
		} finally {
			connector.close();
		}
		response.put("items", items);
	}

	private static String text(Element element, String tag) {
		var nodes = element.getElementsByTagName(tag);
		return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
	}

	private static void importItems(SqlConnector connector, HttpServletRequest request, JSONObject response)
			throws Exception {
		var items = new JSONArray(String.valueOf(request.getParameter("items")));
		var cruds = Arrays.asList(String.valueOf(request.getParameter("cruds")).split(","));
		var override = "true".equals(request.getParameter("override"));
		var wrap = request.getParameter("accessibility");
		var accessibility = wrap == null || wrap.isBlank() ? null : Accessibility.valueOf(wrap);
		var authenticated = "true".equals(request.getParameter("authenticated"));
		var transactions = new JSONArray();
		var sequences = new JSONArray();
		try {
			for (var i = 0; i < items.length(); i++) {
				var item = items.getJSONObject(i);
				var name = item.getString("name");
				List<SqlTransaction> created;
				if ("TABLE".equals(item.optString("type"))) {
					created = SqlConnector.createSqlTransaction(connector, name, cruds);
				} else {
					var specificName = item.optString("specificName", "");
					created = Arrays.asList(SqlConnector.createSqlTransaction(connector, name,
							specificName.isEmpty() ? name : specificName));
				}
				for (var sqlTransaction : created) {
					if (sqlTransaction == null) {
						continue;
					}
					var existing = connector.getTransactionByName(sqlTransaction.getName());
					if (override || existing == null) {
						if (existing != null) {
							var xsdFile = new File(existing.getSchemaFilePath());
							if (xsdFile.exists()) {
								xsdFile.delete();
							}
							connector.remove(existing);
						}
						connector.add(sqlTransaction);
						listDefaults(sqlTransaction);
						transactions.put(sqlTransaction.getFullQName());
					} else if (existing instanceof SqlTransaction sqlExisting) {
						sqlTransaction = sqlExisting;
					}
					if (accessibility != null) {
						sequences.put(wrap(sqlTransaction, accessibility, authenticated).getFullQName());
					}
				}
			}
		} finally {
			connector.close();
		}
		connector.hasChanged = true;
		response.put("transactions", transactions);
		response.put("sequences", sequences);
		response.put("message", transactions.length() + " transaction" + (transactions.length() == 1 ? "" : "s")
				+ (sequences.length() > 0 ? " and " + sequences.length() + " sequence" + (sequences.length() == 1 ? "" : "s") : "")
				+ " imported in " + connector.getName() + ".");
	}

	/**
	 * The default values of the variables of a list transaction, as the Eclipse Studio sets them.
	 */
	private static void listDefaults(SqlTransaction transaction) {
		if (!transaction.getName().endsWith("_LIST")) {
			return;
		}
		var order = "id";
		var matcher = Pattern.compile("SELECT (.*?),").matcher(transaction.getSqlQuery());
		if (matcher.find()) {
			order = matcher.group(1);
		}
		if (transaction.getVariable("order_by") instanceof RequestableVariable variable) {
			variable.setValueOrNull(order);
		}
		if (transaction.getVariable("limit") instanceof RequestableVariable variable) {
			variable.setXmlTypeAffectation(new XmlQName(Constants.XSD_LONG));
			variable.setValueOrNull("100");
		}
		if (transaction.getVariable("offset") instanceof RequestableVariable variable) {
			variable.setXmlTypeAffectation(new XmlQName(Constants.XSD_LONG));
			variable.setValueOrNull("0");
		}
	}

	/**
	 * @return the sequence calling the transaction and copying its rows, created or emptied
	 */
	private static GenericSequence wrap(SqlTransaction transaction, Accessibility accessibility, boolean authenticated)
			throws Exception {
		var project = transaction.getProject();
		var name = transaction.getName();
		GenericSequence sequence;
		try {
			sequence = (GenericSequence) project.getSequenceByName(name);
			var children = new ArrayList<DatabaseObject>();
			children.addAll(sequence.getAllSteps());
			children.addAll(sequence.getAllVariables());
			for (var child : children) {
				sequence.remove(child);
			}
		} catch (Exception e) {
			sequence = new GenericSequence();
			sequence.setName(name);
			sequence.setAccessibility(accessibility);
			sequence.setAuthenticatedContextRequired(authenticated);
			project.add(sequence);
		}
		var transactionStep = new TransactionStep();
		transactionStep.setSourceTransaction(transaction.getQName());
		sequence.add(transactionStep);
		transactionStep.importVariableDefinition();
		transactionStep.exportVariableDefinition();

		var xmlCopyStep = new XMLCopyStep();
		var source = new XMLVector<String>();
		source.add(Long.toString(transactionStep.priority));
		source.add("./document/sql_output[row]/*|./document/sql_output[not(row)]|./document/error");
		xmlCopyStep.setSourceDefinition(source);
		sequence.add(xmlCopyStep);
		sequence.hasChanged = true;
		return sequence;
	}
}
