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
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/engine/admin/services/mobiles/GetResources.java $
 * $Author: nicolasa $
 * $Revision: 37964 $
 * $Date: 2014-09-09 10:59:59 +0200 (mar., 09 sept. 2014) $
 */

package com.twinsoft.convertigo.engine.admin.services.logs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Level;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.LogParameters;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;

@ServiceDefinition(
		name = "Add",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = ""
	)
public class Add extends JSonService {
	
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		JSONArray logs = new JSONArray(request.getParameter("logs"));
		JSONObject env = new JSONObject(request.getParameter("env"));
		
		HttpSession httpSession = request.getSession();
				
		Log4jHelper.mdcSet(new LogParameters());
		Log4jHelper.mdcPut(mdcKeys.ClientIP, request.getRemoteAddr());
		Log4jHelper.mdcPut(mdcKeys.Project, env.getString("project"));
		Log4jHelper.mdcPut(mdcKeys.UID, env.getString("uid"));
		Log4jHelper.mdcPut(mdcKeys.ContextID, httpSession.getId());
		
		if (EnginePropertiesManager.getProperty(PropertyName.NET_REVERSE_DNS).equalsIgnoreCase("true")) {
			Log4jHelper.mdcPut(mdcKeys.ClientHostName, request.getRemoteHost());
		}
		
		if (httpSession.getAttribute("authenticatedUser") != null) {
			Log4jHelper.mdcPut(mdcKeys.User, httpSession.getAttribute("authenticatedUser").toString());			
		}
		
		for (int i = 0; i < logs.length(); i++) {
			JSONObject log = logs.getJSONObject(i);
			Level level = Level.toLevel(log.getString("level"), Level.OFF);
			if (Engine.logDevices.isEnabledFor(level)) {
				String msg = log.getString("msg");
				String time = log.getString("time");
				msg = "(" + time + ") " + msg;
				Engine.logDevices.log(level, msg);
			}
		}
		
		Log4jHelper.mdcClear();
	}	 
}
