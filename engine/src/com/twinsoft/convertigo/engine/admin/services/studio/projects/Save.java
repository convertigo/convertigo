package com.twinsoft.convertigo.engine.admin.services.studio.projects;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
        name = "Save",
        roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
        parameters = {},
        returnValue = ""
    )
public class Save extends XmlService {

    @Override
    protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        String qname = request.getParameter("qname");
        DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
        Element response = document.createElement("response");
        if (dbo != null) {
            try {
                Engine.theApp.databaseObjectsManager.exportProject(dbo.getProject());
                response.setAttribute("status", "success");
                response.setAttribute("message", "Successfully saved project.");
            }
            catch (EngineException e) {
                throw e;
            }
        }
        else {
            throw new EngineException("The project does not exist.");
        }

        document.getDocumentElement().appendChild(response);
    }
}
