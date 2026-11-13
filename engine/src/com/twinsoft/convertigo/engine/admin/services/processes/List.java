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

package com.twinsoft.convertigo.engine.admin.services.processes;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.process.ManagedProcesses;

/**
 * The processes the engine launched and manages: function, PID, start, uptime, state, status given by its
 * launcher, executable (never its arguments, which can carry a secret) and free fields (project, port), with
 * the PID of the process reaper.
 */
@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN, Role.HOME_VIEW, Role.HOME_CONFIG },
		parameters = {},
		returnValue = "the managed processes"
	)
public class List extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var service = ManagedProcesses.get();
		var processes = new JSONArray();
		for (var process : service.list()) {
			var json = new JSONObject();
			json.put("function", process.getFunction());
			json.put("pid", process.pid());
			json.put("start", process.getStartInstant().toString());
			json.put("uptime", process.getUptime().toSeconds());
			json.put("state", process.getState().name());
			json.put("status", process.getStatus());
			json.put("command", process.toHandle().info().command().orElse(""));
			var properties = new JSONObject();
			for (var property : process.getProperties().entrySet()) {
				properties.put(property.getKey(), property.getValue());
			}
			json.put("properties", properties);
			processes.put(json);
		}
		response.put("processes", processes);
		response.put("reaper", service.getReaperPid());
		response.put("accepting", service.isAccepting());
	}
}
