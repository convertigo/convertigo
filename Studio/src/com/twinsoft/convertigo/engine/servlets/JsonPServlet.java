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

import com.twinsoft.convertigo.engine.requesters.JsonPServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;

public class JsonPServlet extends GenericServlet {
	private static final long serialVersionUID = 8273215809782400280L;

	public JsonPServlet() {
    }

    public String getName() {
        return "JsonPServlet";
    }

    @Override
    public String getContentType(HttpServletRequest request) {
    	return getDefaultContentType();
    }
    
    @Override
    public String getDefaultContentType() {
    	return "text/javascript";
    }

    @Override
    public String getServletInfo() {
        return "Twinsoft Convertigo JsonPServlet";
    }
    
    @Override
    public String getDocumentExtension() {
        return ".jsonp";
    }

    @Override
	public Requester getRequester() {
		return new JsonPServletRequester();
	}
}