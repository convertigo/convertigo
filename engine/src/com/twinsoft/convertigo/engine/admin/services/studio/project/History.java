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

package com.twinsoft.convertigo.engine.admin.services.studio.project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

/**
 * Keeps the states of a project after each change of the web Studio, to undo and redo them.
 * The history of a project is dropped when the engine loads it again (reload, close, rename).
 */
@ServiceDefinition(name = "History", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class History extends JSonService {
	private static final int MAX_STATES = 50;
	private static final long MAX_BYTES = 64L * 1024 * 1024;
	private static final Map<String, ProjectHistory> histories = new ConcurrentHashMap<>();

	private record State(String hash, byte[] xml) {
	}

	private static class ProjectHistory {
		private final Deque<State> undo = new ArrayDeque<>();
		private final Deque<State> redo = new ArrayDeque<>();
		private WeakReference<Project> project = new WeakReference<>(null);

		private boolean follows(Project project) {
			return this.project.get() == project;
		}

		private void reset(Project project) {
			undo.clear();
			redo.clear();
			this.project = new WeakReference<>(project);
		}

		private boolean push(State state) {
			var top = undo.peek();
			if (top != null && top.hash.equals(state.hash)) {
				return false;
			}
			undo.push(state);
			long bytes = 0;
			int count = 0;
			for (Iterator<State> it = undo.iterator(); it.hasNext();) {
				var s = it.next();
				bytes += s.xml.length;
				if (++count > MAX_STATES || (count > 2 && bytes > MAX_BYTES)) {
					it.remove();
				}
			}
			return true;
		}
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("project");
		var action = request.getParameter("action");
		if (projectName == null || projectName.isBlank()) {
			throw new ServiceException("missing project parameter");
		}
		var manager = Engine.theApp.databaseObjectsManager;
		var project = manager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("The project " + projectName + " is not opened.");
		}
		var history = histories.computeIfAbsent(projectName, k -> new ProjectHistory());
		boolean done = false;
		synchronized (history) {
			if (!history.follows(project)) {
				history.reset(project);
			}
			switch (action == null ? "state" : action) {
			case "baseline":
				if (history.undo.isEmpty()) {
					done = history.push(export(project));
				}
				break;
			case "snapshot":
				if (history.undo.isEmpty()) {
					// the first change of a project not followed yet: its previous state is lost
					done = history.push(export(project));
				} else if (history.push(export(project))) {
					history.redo.clear();
					done = true;
				}
				break;
			case "undo":
				if (history.push(export(project))) {
					history.redo.clear();
				}
				if (history.undo.size() > 1) {
					history.redo.push(history.undo.pop());
					project = restore(projectName, history.undo.peek());
					history.project = new WeakReference<>(project);
					done = true;
				}
				break;
			case "redo":
				if (history.push(export(project))) {
					history.redo.clear();
				} else if (!history.redo.isEmpty()) {
					var state = history.redo.pop();
					history.undo.push(state);
					project = restore(projectName, state);
					history.project = new WeakReference<>(project);
					done = true;
				}
				break;
			case "clear":
				history.reset(project);
				break;
			case "state":
				break;
			default:
				throw new ServiceException("unknown action " + action);
			}
			response.put("done", done);
			response.put("canUndo", history.undo.size() > 1);
			response.put("canRedo", !history.redo.isEmpty());
		}
	}

	private static State export(Project project) throws Exception {
		var document = CarUtils.exportProjectDocument(project);
		var digest = MessageDigest.getInstance("SHA-256");
		var bytes = new ByteArrayOutputStream();
		try (var gzip = new GZIPOutputStream(bytes); var out = new DigestOutputStream(gzip, digest)) {
			var transformer = XMLUtils.getNewTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.INDENT, "no");
			transformer.transform(new DOMSource(document), new StreamResult(out));
		}
		return new State(HexFormat.of().formatHex(digest.digest()), bytes.toByteArray());
	}

	private static Project restore(String projectName, State state) throws Exception {
		Document document;
		try (var in = new GZIPInputStream(new ByteArrayInputStream(state.xml))) {
			document = XMLUtils.getDefaultDocumentBuilder().parse(in);
		}
		return Engine.theApp.databaseObjectsManager.restoreProject(projectName, document);
	}
}
