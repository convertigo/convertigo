/**
 * 
 */
package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import java.io.StringReader;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@ServiceDefinition(name = "Paste", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Paste extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// target: the id of the target bean in tree
		var target = request.getParameter("target");
		if (target == null) {
			throw new ServiceException("missing target parameter");
		}

		// xml : the xml data to be pasted
		var xml = request.getParameter("xml");
		if (xml == null) {
			throw new ServiceException("missing xml parameter");
		}

		DatabaseObject targetDbo = DboUtils.findDbo(target);
		if (targetDbo != null) {
			Document document = XMLUtils.getDefaultDocumentBuilder().parse(new InputSource(new StringReader(xml)));
			Element root = document.getDocumentElement();
			String kind = root.getAttribute("clipboard");
			NodeList nodeList = root.getChildNodes();
			int len = nodeList.getLength();
			Object object;
			Node node;
			for (int i = 0; i < len; i++) {
				node = (Node) nodeList.item(i);
				if (node.getNodeType() != Node.TEXT_NODE) {
					// case copied tree items
					if ("copy".equals(kind)) {
						object = DboUtils.xmlPaste(node, targetDbo);
						if (object != null) {
							if (object instanceof Project) {
								//TODO
							} else {
								
							}
						}
					}
					// case cut tree items
					else if ("cut".equals(kind)) {
						
					}
				}
			}
			response.put("done", true);
		} else {
			response.put("done", false);
		}
	}
}
