/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import java.io.StringReader;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
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

		JSONArray ids = new JSONArray();
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
						if (object != null && object instanceof DatabaseObject) {
							if (object instanceof Project) {
								//TODO
							}
							ids.put(((DatabaseObject)object).getQName(true));
						}
					}
					// case cut tree items
					else if ("cut".equals(kind)) {
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							Element el = (Element)node;
							String id = el.getAttribute("id");
							DatabaseObject dbo = DboUtils.findDbo(id);
							if (dbo != null && !dbo.equals(targetDbo)) {
								DatabaseObject previousParent = dbo.getParent();
								try {
									dbo.delete();
									targetDbo.add(dbo);
									ids.put(id);
								} catch (Exception e) {
									if (dbo.getParent() == null && previousParent != null) {
										previousParent.add(dbo);
									}
								}
							}
						}
					}
				}
			}
		}
		boolean done = ids.length() > 0;
		response.put("done", done);
		response.put("ids", ids);
	}
}
