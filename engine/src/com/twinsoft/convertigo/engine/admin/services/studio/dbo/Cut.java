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

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@ServiceDefinition(name = "Cut", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Cut extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// ids the ids of cut beans in tree
		var ids = request.getParameter("ids");
		if (ids == null) {
			throw new ServiceException("missing ids parameter");
		}

		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element root = document.createElement("convertigo");
		root.setAttribute("clipboard", "cut");
		document.appendChild(root);
		JSONArray jsonArray = new JSONArray(ids);
		for (int i = 0; i < jsonArray.length(); i++) {
			String id = (String) jsonArray.get(i);
			DatabaseObject dbo = DboUtils.findDbo(id);
			if (dbo != null) {
				DboUtils.xmlCut(document, id);
			}
		}
		if (root.getChildNodes().getLength() > 0) {
			String sXml = XMLUtils.prettyPrintDOM(document);
			response.put("done", true);
			response.put("xml", sXml);
		} else {
			response.put("done", false);
		}
	}
}
