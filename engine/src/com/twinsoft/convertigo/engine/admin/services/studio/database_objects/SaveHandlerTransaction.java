package com.twinsoft.convertigo.engine.admin.services.studio.database_objects;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
        name = "SaveHandlerTransaction",
        roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
        parameters = {},
        returnValue = ""
    )
public class SaveHandlerTransaction extends XmlService {

    @Override
    protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        String qname = request.getParameter("qname");
        String handlers = request.getParameter("handlers");

        Element response = document.createElement("response");
        DatabaseObject dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(qname);
        if (!(dbo instanceof Transaction)) {
            throw new Exception("The database object is not a transaction.");
        }

        // In case of transaction, save its handlers content
        ((Transaction) dbo).handlers = handlers;
        response.setAttribute("status", "success");
        response.setAttribute("message", "Handler transaction updated.");
        document.getDocumentElement().appendChild(response);
    }
}
