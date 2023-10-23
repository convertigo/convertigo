/**
 * 
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

@ServiceDefinition(name = "Copy", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Copy extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// ids the ids of copied beans in tree
		var ids = request.getParameter("ids");
		if (ids == null) {
			throw new ServiceException("missing ids parameter");
		}

		Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
		Element root = document.createElement("convertigo");
		root.setAttribute("clipboard", "copy");
		document.appendChild(root);
		JSONArray jsonArray = new JSONArray(ids);
		for (int i = 0; i < jsonArray.length(); i++) {
			String id = (String) jsonArray.get(i);
			DatabaseObject dbo = DboUtils.findDbo(id);
			if (dbo != null) {
				DboUtils.xmlCopy(document, dbo);
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
