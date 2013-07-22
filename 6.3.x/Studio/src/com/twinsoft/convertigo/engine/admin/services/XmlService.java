/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.admin.services;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.util.DOMUtils;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public abstract class XmlService implements Service {

	public void run(String serviceName, HttpServletRequest request, HttpServletResponse response) throws ServiceException {
        try {
			// Create the document XML response
            Document document = DOMUtils.createDocument();
            Element adminElement = document.createElement("admin");
            adminElement.setAttribute("service", serviceName);
            document.appendChild(adminElement);
			
            // Run the core service
            getServiceResult(request, document);

            // Update servlet response and write result
    		response.setContentType("text/xml");
			response.setCharacterEncoding("UTF-8");
			Writer writer = response.getWriter();
			XMLUtils.prettyPrintDOMWithEncoding(document, "UTF-8", writer);

			if (Engine.logAdmin.isDebugEnabled()) {
				String xml = XMLUtils.prettyPrintDOM(document);
				Engine.logAdmin.debug("XML generated:\n" + xml);
			}
        }
	    catch (Throwable t) {
			ServiceUtils.handleError(t, response);
		}
	}

	protected abstract void getServiceResult(HttpServletRequest request, Document document) throws Exception;
}
