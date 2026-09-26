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

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IScreenClassContainer;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.JavelinTransaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * The handlers the Eclipse Studio offers to add to the JavaScript handlers of a transaction.
 * <ul>
 * <li>id: the transaction</li>
 * <li>apply: missing to list the handlers, else {handlers: the ids of the handlers to add, screenClasses:
 * the screen classes whose entry or exit handler to add, entry, exit}</li>
 * </ul>
 */
@ServiceDefinition(name = "Handlers", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Handlers extends JSonService {

	private static final String STARTED = "on" + Transaction.EVENT_TRANSACTION_STARTED;
	private static final String XML_GENERATED = "on" + Transaction.EVENT_XML_GENERATED;
	private static final String DEFAULT_ENTRY = "onTransactionDefaultHandlerEntry";
	private static final String DEFAULT_EXIT = "onTransactionDefaultHandlerExit";

	static boolean handles(DatabaseObject dbo) {
		return dbo instanceof Transaction;
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var dbo = id == null ? null : Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
		if (!(dbo instanceof Transaction transaction)) {
			throw new ServiceException("The object " + id + " is not a transaction.");
		}
		response.put("id", id);
		var handlers = transaction.handlers == null ? "" : transaction.handlers;
		var javelin = transaction instanceof JavelinTransaction;
		var apply = request.getParameter("apply");
		if (apply == null) {
			var list = new JSONArray();
			list.put(handler(STARTED, "Start of transaction", true, handlers));
			list.put(handler(XML_GENERATED, "XML generation", true, handlers));
			list.put(handler(DEFAULT_ENTRY, "Default transaction entry handler", javelin, handlers));
			list.put(handler(DEFAULT_EXIT, "Default transaction exit handler", javelin, handlers));
			response.put("handlers", list);
			if (transaction.getParent() instanceof IScreenClassContainer<?> container
					&& container.getDefaultScreenClass() != null) {
				response.put("screenClasses", screenClass(container.getDefaultScreenClass(), handlers));
			}
			return;
		}

		var choice = new JSONObject(apply);
		var code = new StringBuilder();
		var chosen = choice.optJSONArray("handlers");
		for (var i = 0; chosen != null && i < chosen.length(); i++) {
			var name = chosen.getString(i);
			if (handlers.contains("function " + name + "()")) {
				continue;
			}
			if (STARTED.equals(name)) {
				code.append("\n// Handles the transaction start event.\n")
					.append("function ").append(name).append("() {\n")
					.append("    // TODO: add your code here\n\n")
					.append("    // TODO: customize the returned value (if you omit returned value, the \n")
					.append("    // algorithm will continue its process).\n")
					.append("    // Possible values are:\n")
					.append("    //    cancel - means the algorithm cancels the transaction core process.\n\n")
					.append("    // return \"cancel\";\n")
					.append("}\n");
			} else if (XML_GENERATED.equals(name)) {
				code.append("\n// Handles the XML generated event.\n")
					.append("function ").append(name).append("() {\n")
					.append("    // TODO: add your code here\n")
					.append("}\n");
			} else if (javelin && (DEFAULT_ENTRY.equals(name) || DEFAULT_EXIT.equals(name))) {
				code.append("\n// Handles the default screenclass ").append(DEFAULT_ENTRY.equals(name) ? "entry" : "exit")
					.append(" event.\n")
					.append("function ").append(name).append("() {\n")
					.append("    // TODO: add your code here\n")
					.append("}\n");
			}
		}
		var screenClasses = choice.optJSONArray("screenClasses");
		for (var i = 0; screenClasses != null && i < screenClasses.length(); i++) {
			var screenClass = screenClasses.getString(i);
			var name = "on" + StringUtils.normalize(screenClass);
			if (choice.optBoolean("entry") && !handlers.contains("function " + name + "Entry()")) {
				code.append("\n// Entry handler for screen class \"").append(screenClass).append("\"\n")
					.append("function ").append(name).append("Entry() {\n")
					.append("    // TODO: add your code here\n\n")
					.append("    // TODO: customize the returned value (if you omit returned value, the \n")
					.append("    // algorithm will continue its process).\n")
					.append("    // Possible values are:\n")
					.append("    //    redetect - means the algorithm detects again the screen class.\n")
					.append("    //    skip     - means the algorithm skips the xmlization process and \n")
					.append("    //               directly goes to the exit handler for the current\n")
					.append("    //               screen class.\n")
					.append("    //    continue - equivalent to an empty string or no return or empty return,\n")
					.append("    //               means the algorithm will continue its process\n\n")
					.append("    // return \"redetect\";\n")
					.append("}\n");
			}
			if (choice.optBoolean("exit") && !handlers.contains("function " + name + "Exit()")) {
				code.append("\n// Exit handler for screen class \"").append(screenClass).append("\"\n")
					.append("function ").append(name).append("Exit() {\n")
					.append("    // TODO: add your code here\n\n")
					.append("    // TODO: customize the returned value (if you omit returned value, the \n")
					.append("    // algorithm will continue its process).\n")
					.append("    // Possible values are:\n")
					.append("    //    accumulate - means the algorithm accumulates XML data and go to\n")
					.append("    //                 the next detected screen class.\n")
					.append("    //    continue - equivalent to an empty string or no return or empty return,\n")
					.append("    //               means the algorithm will continue its process\n\n")
					.append("    // return \"accumulate\";\n")
					.append("}\n");
			}
		}
		if (code.length() > 0) {
			transaction.setExpression(handlers + code);
			transaction.hasChanged = true;
		}
		response.put("done", code.length() > 0);
		response.put("id", transaction.getFullQName());
		response.put("handlers", transaction.handlers);
	}

	private static JSONObject handler(String name, String label, boolean enabled, String handlers) throws Exception {
		return new JSONObject()
				.put("name", name)
				.put("label", label)
				.put("enabled", enabled)
				.put("exists", handlers.contains("function " + name + "()"));
	}

	private static JSONObject screenClass(ScreenClass screenClass, String handlers) throws Exception {
		var name = "on" + StringUtils.normalize(screenClass.getName());
		var children = new JSONArray();
		for (var inherited : screenClass.getInheritedScreenClasses()) {
			children.put(screenClass(inherited, handlers));
		}
		return new JSONObject()
				.put("name", screenClass.getName())
				.put("entry", handlers.contains("function " + name + "Entry()"))
				.put("exit", handlers.contains("function " + name + "Exit()"))
				.put("children", children);
	}
}
