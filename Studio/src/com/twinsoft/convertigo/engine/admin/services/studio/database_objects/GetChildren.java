package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class GetChildren extends XmlService {
	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String qname = request.getParameter("qname");
		
		final Element root = document.getDocumentElement();
		
		if (qname != null) {
			getChildren(qname, root, 1);
		} else {
			for (String qn: Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
				getChildren(qn, root, 0);
			}
		}
	}
	
	private void getChildren(String qname, Element parent, int depth) throws Exception {
		DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		Element elt = parent.getOwnerDocument().createElement("dbo");
		elt.setAttribute("qname", qname);
		elt.setAttribute("classname", dbo.getClass().getName());
		elt.setAttribute("name", dbo.toString());
		elt.setAttribute("category", dbo.getDatabaseType());
		elt.setAttribute("comment", dbo.getComment());
		elt.setAttribute("hasChildren", Boolean.toString(!dbo.getDatabaseObjectChildren().isEmpty()));
		parent.appendChild(elt);
		if (depth > 0) {
			for (DatabaseObject child: dbo.getDatabaseObjectChildren()) {
				getChildren(child.getQName(), elt, depth - 1);
			}
		}
	}
}
