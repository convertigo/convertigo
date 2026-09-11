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

import java.io.StringReader;

import jakarta.servlet.http.HttpServletRequest;

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
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.flow.FlowStudioSupport;

@ServiceDefinition(name = "Paste", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
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

		pasteInto(resolveTarget(target), xml, response);
	}

	protected DatabaseObject resolveTarget(String id) throws Exception {
		return DboUtils.findDbo(id);
	}

	protected JSONObject pasteVirtual(DatabaseObject target, String clipboard) throws Exception {
		return FlowStudioSupport.pasteVirtualClipboard(target, clipboard);
	}

	void pasteInto(DatabaseObject targetDbo, String xml, JSONObject response) throws Exception {
		JSONArray ids = new JSONArray();
		JSONArray results = new JSONArray();
		JSONArray errors = new JSONArray();
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
						if (node instanceof Element element &&
								"com.twinsoft.convertigo.beans.flow.FlowVirtualObject".equals(element.getAttribute("classname"))) {
							throw new ServiceException("Outdated Flow clipboard. Copy the object again.");
						}
						if ("flow-virtual-clipboard".equals(node.getNodeName())) {
							var clipboard = node.getTextContent();
							if (!FlowStudioSupport.isVirtualClipboard(clipboard)) {
								throw new ServiceException("Invalid Flow clipboard. Copy the object again.");
							}
							var result = pasteVirtual(targetDbo, clipboard);
							results.put(result);
							if (result.optBoolean("done", false)) {
								DboUtils.copyResult(result, response);
								ids.put(result.getString("id"));
							} else {
								errors.put(result.opt("error"));
							}
							continue;
						}
						object = DboUtils.xmlPaste(node, targetDbo);
						if (object != null && object instanceof DatabaseObject) {
							DatabaseObject dbo = (DatabaseObject)object;
							if (dbo instanceof Project) {
								//TODO
							} else {
								ids.put(dbo.getQName(true));
								
								// notify for app generation
								BuilderUtils.dboAdded(dbo);
							}
						}
					}
					// case cut tree items
					else if ("cut".equals(kind)) {
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							Element el = (Element)node;
							String id = el.getAttribute("id");
							DatabaseObject dbo = DboUtils.findDbo(id);
							if (dbo != null && !dbo.equals(targetDbo)) {
								if (dbo instanceof Project) {
									
								} else {
									DatabaseObject previousParent = dbo.getParent();
									try {
										dbo.delete();
										targetDbo.add(dbo);
										
										ids.put(id);
										
										// notify for app generation
										BuilderUtils.dboMoved(previousParent, targetDbo, dbo);
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
		}
		boolean done = ids.length() > 0 && errors.length() == 0;
		response.put("done", done);
		response.put("ids", ids);
		if (results.length() > 0) response.put("results", results);
		if (errors.length() > 0) {
			response.put("error", errors.toString());
			response.put("partial", ids.length() > 0);
		}
	}
}
