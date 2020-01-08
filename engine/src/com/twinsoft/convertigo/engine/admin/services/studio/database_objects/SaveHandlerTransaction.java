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
