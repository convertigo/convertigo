package com.twinsoft.convertigo.engine.admin.services.tree;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.FolderType;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
		)
public class PropertyGet extends JSonService {

	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}
		var props = new JSONObject();
		if (id.contains("/")) {
			var split = id.split("/", 2);
			var project = (Project) Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(split[0]);
			var root = project.getDirFile();
			var file = new File(root, split[1]);
			props.put("Name", file.getName());
			props.put("Path", split[1]);
			props.put("Size", file.length());
		} else {
			var reg = Get.parseQName.matcher(id);
			reg.matches();
			var ft = FolderType.parse(reg.group(2));
			if (ft != null) {
				props.put("Name", ft.displayName());
			} else {
				var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
				var document = XMLUtils.getDefaultDocumentBuilder().newDocument();
				var elt = dbo.toXml(document, ExportOption.bIncludeBlackListedElements, ExportOption.bIncludeCompiledValue, ExportOption.bIncludeDisplayName, ExportOption.bIncludeEditorClass, ExportOption.bIncludeShortDescription, ExportOption.bHidePassword);
//				document.appendChild(elt);
//				props.put("xml", XMLUtils.prettyPrintDOM(document));
				
				var node = elt.getFirstChild();
				while (node != null) {
					if (node instanceof Element e) {
						var displayName = e.getAttribute("displayName");
						if (e.getFirstChild() instanceof Element c && c.hasAttribute("value")) {
							props.put(displayName, c.getAttribute("value"));
						} else {
							props.put(displayName, "n/a");
						}
					}
					node = node.getNextSibling();
				}
			}
			
		}
		response.put("properties", props);
		response.put("id", id);
	}
}
