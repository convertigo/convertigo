/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.database_objects;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.DatabaseObject.ExportOption;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Get",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class Get extends XmlService {
	public static Element getProperties(DatabaseObject dbo, Document document, String qname) throws EngineException {
		Element elt = dbo.toXml(document, ExportOption.bIncludeBlackListedElements, ExportOption.bIncludeCompiledValue, ExportOption.bIncludeDisplayName, ExportOption.bIncludeEditorClass, ExportOption.bIncludeShortDescription, ExportOption.bHidePassword);
		elt.setAttribute("qname", qname);
		return elt;
	}
	
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();
		String qname = ServiceUtils.getRequiredParameter(request, "qname");
		Map<String, DatabaseObject> map = com.twinsoft.convertigo.engine.admin.services.projects.Get.getDatabaseObjectByQName(request);
		DatabaseObject res = map.get(qname);
		root.appendChild(getProperties(res, document, qname));
	}
}