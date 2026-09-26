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
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;

/**
 * The design of an SAP JCo connector, as its editor in the Eclipse Studio: the BAPIs of the SAP repository
 * matching a pattern, imported as transactions of the connector.
 * <ul>
 * <li>id: the connector</li>
 * <li>action: search (pattern, BAPI_* by default) or import (functions: a JSON array of name and description)</li>
 * </ul>
 */
@ServiceDefinition(name = "SapDesign", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class SapDesign extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if (!(Utils.getDbo(request.getParameter("id")) instanceof SapJcoConnector connector)) {
			throw new ServiceException("The object is not an SAP JCo connector.");
		}
		if ("import".equals(request.getParameter("action"))) {
			var functions = new JSONArray(String.valueOf(request.getParameter("functions")));
			var imported = new JSONArray();
			for (var i = 0; i < functions.length(); i++) {
				var function = functions.getJSONObject(i);
				var name = function.getString("name");
				connector.removeSerializedData(name);
				var transaction = SapJcoConnector.createSapJcoTransaction(connector, name);
				if (transaction == null) {
					continue;
				}
				// a BAPI imported again replaces its transaction
				var existing = connector.getTransactionByName(name);
				if (existing != null) {
					try {
						new File(existing.getSchemaFilePath()).delete();
					} catch (Exception e) {
						// no schema
					}
					connector.remove(existing);
				}
				transaction.setComment(function.optString("description"));
				connector.add(transaction);
				imported.put(transaction.getFullQName());
			}
			connector.hasChanged = true;
			response.put("transactions", imported);
			return;
		}
		var pattern = request.getParameter("pattern");
		if (pattern == null || pattern.isBlank()) {
			pattern = "BAPI_*";
		}
		// the repository is searched in a context of its own, as the editor of the Eclipse Studio does
		var previous = connector.context;
		var context = new Context("studio-sap-design-" + UUID.randomUUID());
		context.projectName = connector.getProject().getName();
		context.setConnector(connector);
		try {
			var document = SapJcoConnector.executeJCoSearch(connector, pattern);
			var functions = new JSONArray();
			var items = document == null ? null : document.getElementsByTagName("item");
			for (var i = 0; items != null && i < items.getLength(); i++) {
				var item = (Element) items.item(i);
				functions.put(new JSONObject()
						.put("name", text(item, "FUNCNAME"))
						.put("description", text(item, "STEXT"))
						.put("group", text(item, "GROUPNAME")));
			}
			response.put("functions", functions);
		} finally {
			connector.context = previous;
		}
	}

	private static String text(Element item, String name) {
		var nodes = item.getElementsByTagName(name);
		return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
	}
}
