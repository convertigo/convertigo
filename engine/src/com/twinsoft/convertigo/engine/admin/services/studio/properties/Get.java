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

package com.twinsoft.convertigo.engine.admin.services.studio.properties;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@ServiceDefinition(name = "Get", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Get extends JSonService {

	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");

		if (id == null) {
			throw new ServiceException("missing id parameter");
		}
		var props = new JSONObject();
		if (id.contains("/")) {
			addFileProperties(props, id);
		} else {
			var reg = Utils.parseQName.matcher(id);
			reg.matches();
			var ft = FolderType.parse(reg.group(2));
			if (ft != null) {
				addFolderTypeProperties(props, ft.displayName());
			} else {
				var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
				var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
				var elt = dbo.toXml(document, ExportOption.bIncludeBlackListedElements,
						ExportOption.bIncludeCompiledValue, ExportOption.bIncludeDisplayName,
						ExportOption.bIncludeEditorClass, ExportOption.bIncludeShortDescription,
						ExportOption.bHidePassword);
//				document.appendChild(elt);
//				System.out.println(XMLUtils.prettyPrintDOM(document));
				var node = elt.getFirstChild();
				while (node != null) {
					if (node instanceof Element e) {
						addDboProperties(props, e);
					}
					node = node.getNextSibling();
				}
			}

		}
		response.put("properties", props);
		response.put("id", id);
	}

	protected void addFolderTypeProperties(JSONObject props, String name) throws Exception {
		props.put("Name", new JSONObject().put("name", "Name").put("value", name));
	}

	protected void addFileProperties(JSONObject props, String id) throws Exception {
		var split = id.split("/", 2);
		var qname = split[0];
		var path = split[1];

		var project = (Project) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		var root = project.getDirFile();
		var file = new File(root, path);
		var name = file.getName().equals(project.getName()) && path.isBlank() ? "Files" : file.getName();
		var length = file.length();

		props.put("Name", new JSONObject().put("name", "Name").put("value", name));
		props.put("Path", new JSONObject().put("name", "Path").put("value", path));
		props.put("Size", new JSONObject().put("name", "Size").put("value", length));
	}

	protected void addDboProperties(JSONObject props, Element elt) throws Exception {
		JSONObject property = new JSONObject();

		NamedNodeMap map = elt.getAttributes();
		for (int i = 0; i < map.getLength(); i++) {
			Node node = map.item(i);
			var nodeName = node.getNodeName();
			var nodeValue = node.getNodeValue();
			nodeValue = "null".equals(nodeValue) ? "" : nodeValue;
			Boolean.valueOf(nodeValue);
			if ("true".equals(nodeValue) || "false".equals(nodeValue)) {
				property.put(nodeName, Boolean.valueOf(nodeValue));
			} else {
				property.put(nodeName, nodeValue);
			}
		}

		var displayName = elt.getAttribute("displayName");
		Node fc = elt.getFirstChild();
		var nodeName = fc.getNodeName();
		if (fc instanceof Element c && c.hasAttribute("value")) {
			if ("beanData".equals(displayName)) {
				addIonProperties(props, c.getAttribute("value"));
			} else {
				property.put("value", c.getAttribute("value"));
			}

			Node next = fc.getNextSibling();
			if (next != null && next instanceof Element n && n.getTagName().equals("possibleValues")) {
				JSONArray values = new JSONArray();
				Node v = n.getFirstChild();
				while (v != null) {
					values.put(v.getFirstChild().getNodeValue());
					v = v.getNextSibling();
				}
				property.put("values", values);
			}
		} else if ("xmlizable".equals(nodeName) && fc instanceof Element c && c.hasAttribute("classname")) {
			var classname = c.getAttribute("classname");
			if ("com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType".equals(classname)) {
				String smv = c.getFirstChild().getTextContent();
				property.put("mode", smv.split(":")[0]);
				property.put("value", smv.split(":")[1]);
			}
		} else {
			property.put("value", "n/a");
		}
		property.put("class", nodeName);
		property.put("kind", "dbo");

		boolean shouldAdd = !property.has("isHidden") || "false".equals(property.getString("isHidden"));
		if (shouldAdd) {
			props.put(displayName, property);
		}
	}

	protected void addIonProperties(JSONObject props, String beanData) throws Exception {
		com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean ionBean = com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager
				.loadBean(beanData);
		for (com.twinsoft.convertigo.beans.ngx.components.dynamic.IonProperty ionProperty : ionBean.getProperties()
				.values()) {
			String pLabel = ionProperty.getLabel();
			JSONObject property = ionProperty.getJSONObject();
			boolean shouldAdd = !property.getBoolean("hidden");
			if (shouldAdd) {
				property.remove("hidden");
				property.remove("attr");
				property.remove("composite");
				property.put("kind", "ion");
				props.put(pLabel, property);
			}
		}
	}
}
