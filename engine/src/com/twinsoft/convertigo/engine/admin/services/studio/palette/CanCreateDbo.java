/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.palette;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.GenericUtils;

@ServiceDefinition(
		name = "CanCreateDbo",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class CanCreateDbo extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String qname = request.getParameter("qname");
		String folderType = request.getParameter("folderType");
		String beanClassNameToCreate = request.getParameter("beanClass");

		DatabaseObject parentDbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
		Class<? extends DatabaseObject> beanClassToCreate = GenericUtils.cast(Class.forName(beanClassNameToCreate));
		Class<? extends DatabaseObject> databaseObjectClass = Get.folderNameToBeanClass.get(folderType);

		// Create response
		boolean canCreate = DatabaseObjectsManager.acceptDatabaseObjects(
		        parentDbo,
		        beanClassToCreate,
		        databaseObjectClass
		);
		Element response = document.createElement("response");
		response.setAttribute("state", Boolean.toString(canCreate));

		document.getDocumentElement().appendChild(response);
	}
}
