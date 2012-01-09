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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

public abstract class JSonService implements Service {

	public void run(String serviceName, HttpServletRequest request, HttpServletResponse response) throws ServiceException {
        try {
        	response.setContentType("text/javascript");
        	response.setCharacterEncoding("UTF-8");
        	
			JSONObject json = new JSONObject();
			
            getServiceResult(request, json);
            String sJSON = json.toString();
            response.getWriter().write(sJSON);
            
			Engine.logAdmin.debug("JSON generated:\n" + sJSON);
        } catch (Throwable t) {
			ServiceUtils.handleError(t, response);
		}
	}

	protected abstract void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception;
}
