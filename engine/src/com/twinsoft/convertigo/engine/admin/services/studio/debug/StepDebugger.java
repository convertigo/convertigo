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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Context;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.enums.JsonOutput.JsonRoot;
import com.twinsoft.convertigo.engine.enums.RequestAttribute;
import com.twinsoft.convertigo.engine.util.EngineListenerHelper;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * The debug mode of the sequences run from the web Studio, as the Debug mode of the sequence editor of the
 * Eclipse Studio: the execution stops before each step until the Studio asks for the next one, or runs
 * on until it pauses again.
 * <p>
 * A sequence is debugged when its request has the __debug parameter, the token of the debug session, and
 * comes from a session of an administrator of the Studio.
 */
public class StepDebugger extends EngineListenerHelper {
	/** a debug session nobody follows any longer stops pausing after this delay */
	private static final long FORSAKEN = 60_000;

	private static StepDebugger instance;

	private final Map<String, Session> sessions = new ConcurrentHashMap<>();

	private static class Session {
		final Object lock = new Object();
		final Thread thread;
		final Context context;
		final Sequence sequence;
		volatile boolean stepByStep = true;
		volatile boolean paused;
		volatile boolean running = true;
		volatile boolean released;
		volatile long seen = System.currentTimeMillis();
		volatile Step step;
		volatile String output = "";

		Session(Sequence sequence) {
			this.sequence = sequence;
			this.context = sequence.context;
			this.thread = Thread.currentThread();
		}

		boolean followed() {
			return !released && sequence.isRunning() && System.currentTimeMillis() - seen < FORSAKEN;
		}
	}

	private StepDebugger() {
	}

	public static synchronized StepDebugger get() {
		if (instance == null) {
			instance = new StepDebugger();
			Engine.theApp.addEngineListener(instance);
		}
		return instance;
	}

	@Override
	public void sequenceStarted(EngineEvent engineEvent) {
		if (!(engineEvent.getSource() instanceof Sequence sequence) || sequence.context == null) {
			return;
		}
		var request = sequence.context.httpServletRequest;
		var token = request == null ? null : request.getParameter("__debug");
		if (token == null || token.isBlank()) {
			return;
		}
		var httpSession = request.getSession(false);
		if (httpSession == null || !Engine.authenticatedSessionManager.hasRole(httpSession, Role.WEB_ADMIN)) {
			return;
		}
		// the steps of the sequence fire their events
		RequestAttribute.debug.set(request, true);
		var current = sessions.get(token);
		if (current != null && current.running) {
			// a sequence called by the debugged one: its steps stop too when they run in its thread
			return;
		}
		// the finished sessions the Studio did not read again
		var now = System.currentTimeMillis();
		sessions.values().removeIf(session -> !session.running && now - session.seen > FORSAKEN);
		sessions.put(token, new Session(sequence));
	}

	@Override
	public void sequenceFinished(EngineEvent engineEvent) {
		for (var session : sessions.values()) {
			if (session.sequence == engineEvent.getSource()) {
				session.running = false;
				release(session);
			}
		}
	}

	@Override
	public void stepReached(EngineEvent engineEvent) {
		if (!(engineEvent.getSource() instanceof Step step)) {
			return;
		}
		var session = sessionOf(step);
		if (session == null) {
			return;
		}
		session.step = step;
		if (!session.stepByStep || !session.followed()) {
			return;
		}
		session.output = XMLUtils.prettyPrintDOMWithEncoding(session.context.outputDocument);
		synchronized (session.lock) {
			session.paused = true;
			try {
				while (session.paused && session.followed()) {
					session.lock.wait(1000);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				session.paused = false;
			}
		}
	}

	/**
	 * @return the debug session running the step: the one of its thread, else the only one of its sequence
	 */
	private Session sessionOf(Step step) {
		Session found = null;
		var qname = step.getSequence().getQName();
		for (var session : sessions.values()) {
			if (!session.running || session.released) {
				continue;
			}
			if (session.thread == Thread.currentThread()) {
				return session;
			}
			if (session.sequence.getQName().equals(qname)) {
				if (found != null) {
					return null;
				}
				found = session;
			}
		}
		return found;
	}

	private static void release(Session session) {
		synchronized (session.lock) {
			session.paused = false;
			session.lock.notifyAll();
		}
	}

	/**
	 * Runs an action of the Studio on a debug session and describes it.
	 *
	 * @param action state, step (to the next step), run (without stopping), pause (at the next step) or stop
	 * (the debugging, the sequence runs on)
	 * @param json true for the output document in JSON
	 */
	JSONObject action(String token, String action, boolean json) throws Exception {
		var state = new JSONObject();
		var session = token == null ? null : sessions.get(token);
		if (session == null) {
			return state.put("active", false);
		}
		session.seen = System.currentTimeMillis();
		switch (action) {
		case "step" -> release(session);
		case "run" -> {
			session.stepByStep = false;
			release(session);
		}
		case "pause" -> session.stepByStep = true;
		case "stop" -> {
			session.released = true;
			release(session);
			sessions.remove(token);
		}
		default -> {
		}
		}
		if (!session.running) {
			sessions.remove(token);
		}
		state.put("active", !session.released);
		state.put("running", session.running);
		state.put("paused", session.paused);
		state.put("stepByStep", session.stepByStep);
		var step = session.step;
		if (step != null) {
			state.put("step", new JSONObject()
					.put("id", step.getFullQName())
					.put("name", step.getName()));
		}
		if (session.paused) {
			state.put("output", json ? json(session) : session.output);
		}
		return state;
	}

	private static String json(Session session) throws Exception {
		var project = session.context.project;
		var useType = project != null && project.getJsonOutput() == JsonOutput.useType;
		var jsonRoot = project != null ? project.getJsonRoot() : JsonRoot.docNode;
		return XMLUtils.XmlToJson(session.context.outputDocument.getDocumentElement(), true, useType, jsonRoot);
	}
}
