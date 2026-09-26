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
import org.mozilla.javascript.tools.debugger.Dim;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Drives the JavaScript debugger of the web Studio.
 * <ul>
 * <li>action: state (default), start, stop, go, stepInto, stepOver, stepOut, pause, breakOnExceptions
 * (value), source (url), breakpoint (url, line, set), variables (frame), eval (expression)</li>
 * </ul>
 */
@ServiceDefinition(name = "Debugger", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class Debugger extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var debugger = WebDebugger.get();
		var action = request.getParameter("action") == null ? "state" : request.getParameter("action");
		switch (action) {
		case "state" -> {
		}
		case "start" -> debugger.start();
		case "stop" -> debugger.stop();
		case "go" -> debugger.resume(Dim.GO);
		case "stepInto" -> debugger.resume(Dim.STEP_INTO);
		case "stepOver" -> debugger.resume(Dim.STEP_OVER);
		case "stepOut" -> debugger.resume(Dim.STEP_OUT);
		case "pause" -> debugger.pause();
		case "breakOnExceptions" -> debugger.setBreakOnExceptions("true".equals(request.getParameter("value")));
		case "source" -> {
			response.put("source", debugger.sourceOf(request.getParameter("url")));
		}
		case "breakpoint" -> {
			var line = Integer.parseInt(request.getParameter("line"));
			response.put("accepted", debugger.breakpoint(request.getParameter("url"), line,
					!"false".equals(request.getParameter("set"))));
		}
		case "variables" -> {
			response.put("variables", debugger.variables(Integer.parseInt(request.getParameter("frame"))));
		}
		case "eval" -> {
			response.put("result", debugger.eval(request.getParameter("expression")));
		}
		default -> throw new ServiceException("Unknown action " + action);
		}
		response.put("state", debugger.state());
	}
}
