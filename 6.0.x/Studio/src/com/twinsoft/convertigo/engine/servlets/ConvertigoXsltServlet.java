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

import com.twinsoft.convertigo.engine.requesters.ConvertigoXsltServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;

public class ConvertigoXsltServlet extends GenericServlet {

	private static final long serialVersionUID = 3045357062633415521L;

	public ConvertigoXsltServlet() {
    }

    public String getName() {
        return "ConvertigoXsltServlet";
    }

    public String getDefaultContentType() {
    	return "text/html";
    }

    public String getServletInfo() {
        return "Twinsoft Convertigo ConvertigoXsltServlet";
    }
    
    public String getDocumentExtension() {
        return ".cxml";
    }
    
    public Requester getRequester() {
    	return new ConvertigoXsltServletRequester();
    }
}
