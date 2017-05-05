package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.connectors.FullSyncConnector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.studio.responses.XmlResponseFactory;
import com.twinsoft.convertigo.engine.util.GenericUtils;

@ServiceDefinition(
		name = "Create",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class Create extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        String qname = request.getParameter("qname");
        String folderType = request.getParameter("folderType");
        String beanClassNameToCreate = request.getParameter("beanClass");

        DatabaseObject parentDbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
        Class<? extends DatabaseObject> beanClassToCreate = GenericUtils.cast(Class.forName(beanClassNameToCreate));
        Class<? extends DatabaseObject> databaseObjectClass = GetPalette.folderNameToBeanClass.get(folderType);
        Element root = document.getDocumentElement();

        // Before add, we musht check if we can add it
        boolean canCreate = DatabaseObjectsManager.acceptDatabaseObjects(
                parentDbo,
                beanClassToCreate,
                databaseObjectClass
        );
        if (canCreate) {
            DatabaseObject newBean = (DatabaseObject) Class.forName(beanClassNameToCreate).newInstance();

            try {
                String beanName = newBean.getName();
                // The name of the FullSyncConnector needs to in be lower case
                if (newBean instanceof FullSyncConnector) {
                    beanName = beanName.toLowerCase();
                }

                // Compute new name
                newBean.setName(newBean.getChildBeanName(parentDbo.getAllChildren(), beanName, true));

                // If the new bean has to be inserted at a specific position
                if (parentDbo instanceof IContainerOrdered) {
                    String afterPriority = request.getParameter("afterPriority");
                    Long after = null;
                    try {
                        after = new Long(afterPriority);
                    }
                    catch (NumberFormatException e) {
                        after = null;
                    }

                    IContainerOrdered containerOrdered = (IContainerOrdered) parentDbo;
                    containerOrdered.add(newBean, after);
                }
                else {
                    parentDbo.add(newBean);
                }

                // Get new bean info
                Element response = document.createElement("response");
                GetChildren.getChildren(newBean.getQName(), response, 0);
                response.setAttribute("state", "success");
                root.appendChild(response);
            }
            catch (EngineException e) {
                root.appendChild(XmlResponseFactory.createMessageBoxResponse(document, parentDbo.getQName(),
                        e.getErrorMessage()));
            }
        }
        else {
            root.appendChild(XmlResponseFactory.createMessageBoxResponse(document, parentDbo.getQName(),
                   "You cannot create a " + beanClassNameToCreate + " object here."));
        }
	}

}
