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

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.transactions.SiteClipperTransaction;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * Writes again the schema of a transaction, as the "Update schema" actions of the Eclipse Studio: from its
 * definition, or extracted from an XML response of the transaction.
 * <ul>
 * <li>id: the tree id of the transaction</li>
 * <li>xml: the XML response to extract the schema from, the definition of the transaction when missing</li>
 * </ul>
 */
@ServiceDefinition(name = "UpdateSchema", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class UpdateSchema extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		if (id == null || id.isBlank()) {
			throw new ServiceException("missing id parameter");
		}
		if (!(Utils.getDbo(id) instanceof Transaction transaction)) {
			throw new ServiceException("Only a transaction has a schema to update.");
		}
		var xml = request.getParameter("xml");
		update(transaction, xml == null || xml.isBlank() ? null : XMLUtils.parseDOM("java", xml));
		response.put("done", true);
		response.put("message", "The schema of " + transaction.getName() + " is updated.");
	}

	/**
	 * Updates the schema of the transaction and marks it changed.
	 *
	 * @param document the response to extract the schema from, null to generate it from the definition
	 */
	public static void update(Transaction transaction, Document document) throws Exception {
		if (transaction instanceof SiteClipperTransaction) {
			throw new ServiceException("A site clipper transaction has no schema.");
		}
		var name = transaction.getName();
		if (!name.equals(StringUtils.normalize(name, true))) {
			throw new ServiceException("The name of the transaction should be normalized.");
		}
		var connector = (Connector) transaction.getParent();
		if (!connector.getName().equals(StringUtils.normalize(connector.getName(), true))) {
			throw new ServiceException("The name of the connector should be normalized.");
		}
		if (connector.getDefaultTransaction() == null) {
			throw new ServiceException("The connector must have a default transaction.");
		}
		var extract = document != null;
		var write = true;
		if (transaction instanceof XmlHttpTransaction xmlHttp) {
			var assigned = !xmlHttp.getXmlElementRefAffectation().isEmpty();
			var responseRoot = !xmlHttp.getResponseElementQName().isEmpty();
			if (extract && assigned) {
				throw new ServiceException("Unset the 'Assigned element QName' property first.");
			}
			if (extract && responseRoot) {
				throw new ServiceException("Unset the 'Schema of XML response root element' property first.");
			}
			// the schema of these responses comes from the properties, as the Eclipse Studio keeps it
			write = extract || !(assigned || responseRoot);
		}
		if (extract) {
			document.getDocumentElement().setAttribute("transaction", transaction.getXsdTypePrefix() + name);
		}
		var xsdTypes = transaction.generateXsdTypes(document, extract);
		if (xsdTypes == null || xsdTypes.isEmpty()) {
			throw new ServiceException("No schema is generated for " + name + ".");
		}
		if (write) {
			transaction.writeSchemaToFile(xsdTypes);
		}
		transaction.hasChanged = true;
		Engine.theApp.schemaManager.clearCache(transaction.getProject().getName());
	}
}
