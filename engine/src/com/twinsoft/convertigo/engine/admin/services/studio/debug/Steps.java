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

package com.twinsoft.convertigo.engine.admin.services.studio.debug;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Drives the debug mode of the sequences run from the web Studio, as the Debug, Run, Pause and Step
 * buttons of the sequence editor of the Eclipse Studio.
 * <ul>
 * <li>token: the debug session, the __debug parameter of the request running the sequence</li>
 * <li>action: state (default), start (before running the sequence), step, run, pause, stop</li>
 * <li>json: true for the output document in JSON</li>
 * </ul>
 */
@ServiceDefinition(name = "Steps", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class Steps extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var action = request.getParameter("action") == null ? "state" : request.getParameter("action");
		var state = StepDebugger.get().action(request.getParameter("token"), action,
				"true".equals(request.getParameter("json")));
		response.put("state", state);
	}
}
