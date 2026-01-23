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

package com.twinsoft.convertigo.engine.admin.services.engine;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.requesters.HttpSessionListener;
import com.twinsoft.tas.KeyManager;

@ServiceDefinition(
		name = "Monitor",
		roles = { Role.WEB_ADMIN, Role.MONITOR_AGENT, Role.HOME_VIEW, Role.HOME_CONFIG },
		parameters = {},
		returnValue = "the monitoring data"
	)
public class JsonMonitor extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		final long mb = 1024 * 1024;
		Runtime runtime = Runtime.getRuntime();
		long memoryTotal = runtime.totalMemory();
		int sessionCount = HttpSessionListener.countSessions();
		int sessionMaxCV = KeyManager.getMaxCV(Session.EmulIDSE);
		response.put("memoryMaximal", runtime.maxMemory() / mb);
		response.put("memoryTotal", memoryTotal / mb);
		response.put("memoryUsed", (memoryTotal - runtime.freeMemory()) / mb);
		response.put("threads", RequestableObject.nbCurrentWorkerThreads);
		response.put("sessions", sessionCount);
		response.put("sessionMaxCV", sessionMaxCV);
		response.put("availableSessions", sessionMaxCV - sessionCount);
		try {
			response.put("contexts", Engine.isStarted ? Engine.theApp.contextManager.getNumberOfContexts() : 0);
		} catch (Exception e) {
			response.put("contexts", 0);
		}
		response.put("requests", Math.max(EngineStatistics.getAverage(EngineStatistics.REQUEST), 0));
		response.put("engineState", Engine.isStarted);
		response.put("startTime", Engine.startStopDate);
		response.put("time", System.currentTimeMillis());
	}

}
