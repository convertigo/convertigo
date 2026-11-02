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
import jakarta.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;


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
import com.twinsoft.convertigo.engine.debug.JavascriptDebugger;
import com.twinsoft.convertigo.engine.util.CopilotHelper;

/**
 * Drives the JavaScript debugger of the web Studio.
 * <ul>
 * <li>action: state (default), start, stop, go, stepInto, stepOver, stepOut, pause, breakOnExceptions
 * (value), source (url), breakpoint (url, line, set), breakpoints (url), variables (frame), eval
 * (expression, frame). Resume and inspection require the current pauseId.</li>
 * <li>Requests opt in with __javascriptDebug=state.sessionId in the owning administrator HTTP session.</li>
 * <li>id and property: the object and its property holding a script, in place of its url; the lines
 * are then the lines of the property</li>
 * </ul>
 */
@ServiceDefinition(name = "Debugger", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class Debugger extends JSonService {

	/** Debugger sources and values must never enter the generic JSON response/error logs. */
	@Override
	public void run(String serviceName, HttpServletRequest request, HttpServletResponse response) {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		try {
			var json = new JSONObject(); getServiceResult(request, json); response.getWriter().write(json.toString());
		} catch (Exception e) {
			try {
				response.setStatus(400);
				String message = e instanceof IllegalArgumentException || e instanceof IllegalStateException
						? e.getMessage() : "Debugger operation failed. Refresh the debugger.";
				response.getWriter().write(new JSONObject().put("error", new JSONObject().put("message", message)).toString());
			} catch (Exception ignored) { /* Do not log debug payloads, including on disconnect. */ }
		}
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if (!"POST".equals(request.getMethod())) throw new IllegalArgumentException("Debugger requests require POST.");
		var httpSession = request.getSession(false);
		if (httpSession == null || !com.twinsoft.convertigo.engine.Engine.authenticatedSessionManager.hasRole(httpSession, Role.WEB_ADMIN))
			throw new IllegalArgumentException("Administrator session required.");
		String owner = httpSession.getId();
		var action = request.getParameter("action") == null ? "state" : request.getParameter("action");
		if (!JavascriptDebugger.enabled()) {
			JavascriptDebugger.stop(owner);
			if (!"state".equals(action)) throw new IllegalStateException("JavaScript debugging is disabled on this server.");
		}
		if ("stop".equals(action)) JavascriptDebugger.stop(owner);
		var debugger = JavascriptDebugger.session(owner, "start".equals(action));
		if (debugger == null) {
			if (!java.util.Set.of("state", "stop", "breakpoints").contains(action)) throw new IllegalStateException("Start the debugger first.");
			response.put("state", new JSONObject().put("enabled", JavascriptDebugger.enabled()).put("attached", false)
					.put("stopped", false).put("sources", new JSONArray()).put("serial", 0));
			response.put("breakpoints", new JSONArray());
			return;
		}
		long pauseId = request.getParameter("pauseId") == null ? -1 : Long.parseLong(request.getParameter("pauseId"));
		var url = request.getParameter("url");
		var id = request.getParameter("id");
		// the lines the engine adds before the script of a property when it runs it
		var offset = 0;
		if (url == null && id != null) {
			// the script of a property of an object, as the engine names it when it runs it
			var dbo = Utils.getDbo(id);
			url = dbo.getShortQName() + "-" + scriptName(dbo, request.getParameter("property"));
			offset = CopilotHelper.addInstruction(dbo, "").split("\n", -1).length - 1;
			// the frames of the script show in the lines of the property
			response.put("url", url);
			response.put("offset", offset);
		}
		switch (action) {
		case "state" -> {
		}
		case "start", "stop" -> { }
		case "go", "stepInto", "stepOver", "stepOut" -> debugger.resume(action, pauseId);
		case "pause" -> debugger.requestPause();
		case "breakOnExceptions" -> debugger.breakOnExceptions("true".equals(request.getParameter("value")));
		case "breakOnEnter" -> debugger.breakOnEnter("true".equals(request.getParameter("value")));
		case "breakOnReturn" -> debugger.breakOnReturn("true".equals(request.getParameter("value")));
		case "studioRequests" -> JavascriptDebugger.studioRequests(owner, "true".equals(request.getParameter("value")));
		case "source" -> {
			response.put("source", debugger.source(url));
		}
		case "breakpoint" -> {
			var line = Integer.parseInt(request.getParameter("line")) + offset;
			response.put("accepted", debugger.breakpoint(url, line, !"false".equals(request.getParameter("set"))));
			response.put("breakpoints", lines(debugger.breakpoints(url), offset));
		}
		case "breakpoints" -> {
			response.put("breakpoints", lines(debugger.breakpoints(url), offset));
		}
		case "variables" -> {
			response.put("variables", debugger.variables(Integer.parseInt(request.getParameter("frame")), pauseId));
		}
		case "eval" -> {
			// Audit the capability use only: no source, expression, result or session token.
			com.twinsoft.convertigo.engine.Engine.logAudit.info("JavaScript debugger administrator evaluation requested");
			var result = debugger.evaluate(Integer.parseInt(request.getParameter("frame")), pauseId, request.getParameter("expression"));
			response.put("result", result.get("result"));
			response.put("evaluationError", result.optBoolean("evaluationError"));
		}
		default -> throw new ServiceException("Unknown action " + action);
		}
		response.put("state", debugger.state().put("enabled", true));
	}

	/**
	 * @return the name the engine gives the script of a property when it runs it, the name of the
	 * property but for a few objects
	 */
	public static String scriptName(DatabaseObject dbo, String property) {
		if (property == null || property.isEmpty()) throw new IllegalArgumentException("Script property required.");
		try {
			var getter = dbo.getClass().getMethod("get" + Character.toUpperCase(property.charAt(0)) + property.substring(1));
			if (getter.getReturnType() == com.twinsoft.convertigo.beans.steps.SmartType.class) return property + "-smartType";
		} catch (NoSuchMethodException e) { /* Ordinary expression names need no SmartType suffix. */ }

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
