/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.enums.MimeType;

public abstract class JSonService implements Service {

	public void run(String serviceName, HttpServletRequest request, HttpServletResponse response) throws ServiceException {
        try {
        	response.setContentType(MimeType.JavascriptDeprecated.value());
        	response.setCharacterEncoding("UTF-8");
        	
			JSONObject json = new JSONObject();
			
            getServiceResult(request, json);
            String sJSON = json.toString();
            response.getWriter().write(sJSON);
            
			Engine.logAdmin.debug("JSON generated:\n" + sJSON);
        } catch (Throwable t) {
			ServiceUtils.handleError(t, request, response);
		}
	}

	protected abstract void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception;
}
