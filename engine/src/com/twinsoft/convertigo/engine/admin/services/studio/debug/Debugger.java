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

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.mozilla.javascript.tools.debugger.Dim;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.steps.ExceptionStep;
import com.twinsoft.convertigo.beans.steps.LogStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.util.CopilotHelper;

/**
 * Drives the JavaScript debugger of the web Studio.
 * <ul>
 * <li>action: state (default), start, stop, go, stepInto, stepOver, stepOut, pause, breakOnExceptions
 * (value), source (url), breakpoint (url, line, set), breakpoints (url), variables (frame), eval
 * (expression)</li>
 * <li>id and property: the object and its property holding a script, in place of its url; the lines
 * are then the lines of the property</li>
 * </ul>
 */
@ServiceDefinition(name = "Debugger", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class Debugger extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var debugger = WebDebugger.get();
		var action = request.getParameter("action") == null ? "state" : request.getParameter("action");
		var url = request.getParameter("url");
		var id = request.getParameter("id");
		// the lines the engine adds before the script of a property when it runs it
		var offset = 0;
		if (url == null && id != null) {
			// the script of a property of an object, as the engine names it when it runs it
			var dbo = Utils.getDbo(id);
			url = dbo.getShortQName() + "-" + scriptName(dbo, request.getParameter("property"));
			offset = CopilotHelper.addInstruction(dbo, "").split("\n", -1).length - 1;
		}
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
			response.put("source", debugger.sourceOf(url));
		}
		case "breakpoint" -> {
			var line = Integer.parseInt(request.getParameter("line")) + offset;
			response.put("accepted", debugger.breakpoint(url, line, !"false".equals(request.getParameter("set"))));
			response.put("breakpoints", lines(debugger.breakpointsOf(url), offset));
		}
		case "breakpoints" -> {
			response.put("breakpoints", lines(debugger.breakpointsOf(url), offset));
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

	/**
	 * @return the name the engine gives the script of a property when it runs it, the name of the
	 * property but for a few objects
	 */
	private static String scriptName(DatabaseObject dbo, String property) {
		if (dbo instanceof Transaction && "handlers".equals(property)) {
			return dbo.getName();
		}
		if (dbo instanceof LogStep && "expression".equals(property)) {
			return "LogStep";
		}
		if (dbo instanceof ExceptionStep && "expression".equals(property)) {
			return "message";
		}
		if ((dbo instanceof TransactionStep || dbo instanceof SequenceStep) && "contextName".equals(property)) {
			return "ctxName";
		}
		return property;
	}

	private static JSONArray lines(JSONArray breakpoints, int offset) throws Exception {
		var lines = new JSONArray();
		for (var i = 0; i < breakpoints.length(); i++) {
			var line = breakpoints.getInt(i) - offset;
			if (line > 0) {
				lines.put(line);
			}
		}
		return lines;
	}
}
