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

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.MonitorMetrics;
import com.twinsoft.convertigo.engine.MonitorMetrics.Sample;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Monitor",
		roles = { Role.WEB_ADMIN, Role.MONITOR_AGENT, Role.HOME_VIEW, Role.HOME_CONFIG },
		parameters = {},
		returnValue = "the monitoring data"
	)
public class JsonMonitor extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		putSample(response, MonitorMetrics.current());
	}

	private static void putSample(JSONObject json, Sample sample) throws Exception {
		json.put("memoryMaximal", sample.memoryMaximal);
		json.put("memoryTotal", sample.memoryTotal);
		json.put("memoryUsed", sample.memoryUsed);
		json.put("threads", sample.threads);
		json.put("contexts", sample.contexts);
		json.put("sessions", sample.sessions);
		json.put("sessionMaxCV", sample.sessionMaxCV);
		json.put("availableSessions", sample.availableSessions);
		json.put("requests", sample.requests);
		json.put("engineState", sample.engineState);
		json.put("startTime", sample.startTime);
		json.put("time", sample.time);
	}

}
