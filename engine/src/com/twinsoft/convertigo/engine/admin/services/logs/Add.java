/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.logs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;
import com.twinsoft.convertigo.engine.util.Log4jHelper.mdcKeys;

@ServiceDefinition(
	name = "Add",
	roles = { Role.ANONYMOUS },
	parameters = {},
	returnValue = "",
	allow_cors = true,
	admin = false
)
public class Add extends JSonService {

	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		try {
			JSONArray logs = new JSONArray(request.getParameter("logs"));
			JSONObject env = new JSONObject(request.getParameter("env"));

			String uid = env.getString("uid");

			if (uid == null) {
				throw new IllegalArgumentException();
			}

			HttpSession httpSession = request.getSession();
			Map<String, LogParameters> logParametersMap = new HashMap<String, LogParameters>();
			var stored = httpSession.getAttribute(Add.class.getCanonicalName());
			if (stored instanceof Map<?, ?> storedMap) {
				for (var entry : storedMap.entrySet()) {
					if (entry.getKey() != null) {
						logParametersMap.put(entry.getKey().toString(), toLogParameters(entry.getValue()));
					}
				}
			}
			httpSession.setAttribute(Add.class.getCanonicalName(), logParametersMap);

			LogParameters logParameters = logParametersMap.get(uid);

			if (logParameters == null) {
				logParametersMap.put(uid, logParameters = new LogParameters());
				
				logParameters.put(mdcKeys.ContextID.toString().toLowerCase(), httpSession.getId());
			}

			Log4jHelper.mdcSet(logParameters);
			
			logParameters.put(mdcKeys.ClientIP.toString().toLowerCase(), request.getRemoteAddr());

			if (EnginePropertiesManager.getProperty(PropertyName.NET_REVERSE_DNS).equalsIgnoreCase("true")) {
				Log4jHelper.mdcPut(mdcKeys.ClientHostName, request.getRemoteHost());
			}
			
			for (Iterator<String> iKey = GenericUtils.cast(env.keys()); iKey.hasNext();) {
				String key = iKey.next();
				logParameters.put(key.toLowerCase(), env.get(key));
			}

			if (SessionAttribute.authenticatedUser.get(httpSession) != null) {
				Log4jHelper.mdcPut(mdcKeys.User, SessionAttribute.authenticatedUser.string(httpSession));			
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
			
			response.put("remoteLogLevel", Engine.logDevices.getEffectiveLevel().toString().toLowerCase());
		} finally {
			Log4jHelper.mdcClear();
		}
	}

	private LogParameters toLogParameters(Object value) {
		if (value instanceof LogParameters logParameters) {
			return logParameters;
		}
		var logParameters = new LogParameters();
		if (value instanceof Map<?, ?> map) {
			for (var entry : map.entrySet()) {
				if (entry.getKey() != null) {
					logParameters.put(entry.getKey().toString(), entry.getValue());
				}
			}
		}
		return logParameters;
	}
}
