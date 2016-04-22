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

package com.twinsoft.convertigo.engine.servlets;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.requesters.ClientXsltServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;

public class ClientXsltServlet extends GenericServlet {

	private static final long serialVersionUID = -6347865570410554737L;

	public ClientXsltServlet() {
    }

    public String getName() {
        return "ClientXsltServlet";
    }

    @Override
    public String getDefaultContentType() {
    	return MimeType.TextXml.value();
    }

    @Override
    public String getServletInfo() {
        return "Twinsoft Convertigo ClientXsltServlet";
    }
    
    public String getDocumentExtension() {
        return ".xml";
    }
    
    @Override
    public Object processRequest(HttpServletRequest request) throws Exception {
    	Object result = super.processRequest(request);
    	//if(request.getAttribute("convertigo.contentType")==null) request.setAttribute("convertigo.contentType", getDefaultContentType());
    	request.setAttribute("convertigo.contentType", getDefaultContentType());
		return result;
    }

    public Requester getRequester() {
    	return new ClientXsltServletRequester();
    }
}
