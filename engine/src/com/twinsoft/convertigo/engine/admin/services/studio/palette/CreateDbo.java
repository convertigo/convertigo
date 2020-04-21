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

package com.twinsoft.convertigo.engine.admin.services.studio.palette;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.database_objects.GetChildren;
import com.twinsoft.convertigo.engine.util.GenericUtils;

@ServiceDefinition(
		name = "CreateDbo",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class CreateDbo extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        String qname = request.getParameter("qname");
        String folderType = request.getParameter("folderType");
        String beanClassNameToCreate = request.getParameter("beanClass");

        DatabaseObject parentDbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
        Class<? extends DatabaseObject> beanClassToCreate = GenericUtils.cast(Class.forName(beanClassNameToCreate));
        Class<? extends DatabaseObject> databaseObjectClass = Get.folderNameToBeanClass.get(folderType);
        Element root = document.getDocumentElement();

        // Before add, we musht check if we can add it
        boolean canCreate = DatabaseObjectsManager.acceptDatabaseObjects(
                parentDbo,
                beanClassToCreate,
                databaseObjectClass
        );
        if (canCreate) {
            DatabaseObject newBean = (DatabaseObject) Class.forName(beanClassNameToCreate).getConstructor().newInstance();

            try {
                String beanName = newBean.getName();

                // The name of the FullSyncConnector needs to in be lower case
                if (newBean instanceof FullSyncConnector) {
                    beanName = beanName.toLowerCase();
                }

                // Compute new name
                newBean.setName(newBean.getChildBeanName(parentDbo.getDatabaseObjectChildren(), beanName, true));

                // If the new bean has to be inserted at a specific position
                if (parentDbo instanceof IContainerOrdered) {
                    String afterPriority = request.getParameter("afterPriority");
                    Long after = null;
                    try {
                        after = Long.valueOf(afterPriority);
                    }
                    catch (NumberFormatException e) {
                        after = null;
                    }

                    IContainerOrdered containerOrdered = (IContainerOrdered) parentDbo;
                    containerOrdered.add(newBean, after);
                }
                else {
                    // Need to create more things in case of Connectors
                    if (newBean instanceof Connector) {
                        Connector.setupConnector(newBean);
                    }

                    parentDbo.add(newBean);
                }

                // Get new bean info
                Element response = document.createElement("response");
                GetChildren.getChildren(newBean.getQName(), response, 0);
                response.setAttribute("state", "success");
                root.appendChild(response);
            }
            catch (EngineException e) {
                throw e;
            }
        }
        else {
            throw new EngineException("You cannot create a " + beanClassNameToCreate + " object here.");
        }
	}
}
