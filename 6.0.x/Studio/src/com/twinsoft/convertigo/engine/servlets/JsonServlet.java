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

import com.twinsoft.convertigo.engine.requesters.JsonServletRequester;
import com.twinsoft.convertigo.engine.requesters.Requester;

public class JsonServlet extends GenericServlet {
	private static final long serialVersionUID = 8273215809782400280L;

	public JsonServlet() {
    }

    public String getName() {
        return "JsonServlet";
    }

    @Override
    public String getContentType(HttpServletRequest request) {
    	return getDefaultContentType();
    }
    
    @Override
    public String getDefaultContentType() {
    	return "text/javascript";//"application/json";
    }

    @Override
    public String getServletInfo() {
        return "Twinsoft Convertigo JsonServlet";
    }
    
    @Override
    public String getDocumentExtension() {
        return ".json";
    }

    @Override
	public Requester getRequester() {
		return new JsonServletRequester();
	}
}