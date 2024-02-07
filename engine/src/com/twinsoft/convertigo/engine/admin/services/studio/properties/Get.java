/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.ScreenClass;
import com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.ngx.components.UIDynamicElement;
import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;
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
				if (dbo != null) {
					var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
					var elt = dbo.toXml(document, ExportOption.bIncludeBlackListedElements,
							ExportOption.bIncludeCompiledValue, ExportOption.bIncludeDisplayName,
							ExportOption.bIncludeEditorClass, ExportOption.bIncludeShortDescription,
							ExportOption.bHidePassword);
//					document.appendChild(elt);
//					System.out.println(XMLUtils.prettyPrintDOM(document));
					var node = elt.getFirstChild();
					while (node != null) {
						if (node instanceof Element e && e.getNodeName().equals("property")) {
							addDboProperties(props, e);
						}
						node = node.getNextSibling();
					}
					addInfosProperties(props, dbo);
				}
			}
		}
		response.put("properties", props);
		response.put("id", id);
	}

	protected void addInfosProperties(JSONObject props, DatabaseObject dbo) {
		try {
			JSONObject info = new JSONObject().put("category", "Information").put("isDisabled", true);

			String depth = Integer.toString(dbo instanceof ScreenClass ? ((ScreenClass) dbo).getDepth()
					: org.apache.commons.lang3.StringUtils.countMatches(dbo.getQName(), '.'));
			props.put("Depth", new JSONObject(info.toString()).put("name", "P_Depth").put("value", depth));

			String exported = dbo.getProject().getInfoForProperty("exported");
			props.put("Exported", new JSONObject(info.toString()).put("name", "P_Exported").put("value", exported));

			String javaClass = dbo.getClass().getName();
			props.put("Java class", new JSONObject(info.toString()).put("name", "P_JavaClass").put("value", javaClass));

			String minVersion = (String) dbo.getProject().getMinVersion();
			props.put("Min version",
					new JSONObject(info.toString()).put("name", "P_MinVersion").put("value", minVersion));

			String name = dbo.getName();
			props.put("Name", new JSONObject(info.toString()).put("name", "P_Name").put("value", name));

			String priority = Long.toString(dbo.priority);
			props.put("Priority", new JSONObject(info.toString()).put("name", "P_Priority").put("value", priority));

			String qname = dbo.getQName();
			props.put("QName", new JSONObject(info.toString()).put("name", "P_QName").put("value", qname));

			if (dbo instanceof ApplicationComponent) {
				String tplVersion = ((ApplicationComponent) dbo).getTplProjectVersion();
				props.put("Template version",
						new JSONObject(info.toString()).put("name", "P_TemplateVersion").put("value", tplVersion));
			}

			String type = null;
			if (dbo instanceof UIDynamicElement) {
				IonBean ionBean = ((UIDynamicElement) dbo).getIonBean();
				if (ionBean != null) {
					type = ionBean.getName();
				}
			}
			try {
				type = type == null
						? CachedIntrospector.getBeanInfo(dbo.getClass()).getBeanDescriptor().getDisplayName()
						: type;
			} catch (Exception e) {
				type = "n/a";
			}
			props.put("Type", new JSONObject(info.toString()).put("name", "P_Type").put("value", type));

		} catch (Exception e) {
		}
	}

	protected void addFolderTypeProperties(JSONObject props, String name) throws Exception {
		props.put("Name", new JSONObject().put("name", "Name").put("value", name).put("category", "Information")
				.put("isDisabled", true));
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

		props.put("Name", new JSONObject().put("name", "Name").put("value", name).put("category", "Information")
				.put("isDisabled", true));
		props.put("Path", new JSONObject().put("name", "Path").put("value", path).put("category", "Information")
				.put("isDisabled", true));
		props.put("Size", new JSONObject().put("name", "Size").put("value", length).put("category", "Information")
				.put("isDisabled", true));
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
				while (v != null && v.getFirstChild() != null) {
					values.put(v.getFirstChild().getNodeValue());
					v = v.getNextSibling();
				}
				property.put("values", values);
			}
		} else if ("xmlizable".equals(nodeName) && fc instanceof Element c && c.hasAttribute("classname")) {
			var classname = c.getAttribute("classname");
			if ("com.twinsoft.convertigo.beans.ngx.components.MobileSmartSourceType".equals(classname)) {
				String mss = c.getFirstChild().getTextContent();
				int index = mss.indexOf(":");
				String mode = "plain";
				Object value = "";
				if (index != -1) {
					mode = mss.substring(0, index);
					try {
						value = mode.equals("source") ? new JSONObject(mss.substring(index + 1))
								: mss.substring(index + 1);
					} catch (Exception e) {
						value = mode.equals("source") ? new JSONObject() : "";
					}
				}
				property.put("mode", mode);
				property.put("value", value);
			} else {
				property.put("value", "n/a");
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
