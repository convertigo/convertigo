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

package com.twinsoft.convertigo.engine.admin.services.studio.assistant;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.MigrationManager;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.GitUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;

/**
 * The local agent stack of the Convertigo Assistant (lib_ConvertigoAssistant, lib_ConvertigoMCP,
 * lib_ConvertigoAgentBridge), as the Assistant view of the Eclipse Studio sees and installs it: the Assistant hosted
 * on assistant.convertigo.com asks the Studio to activate the local agent, which imports these projects.
 * <ul>
 * <li>action=state (default): installed, version of each project, state (ready, loading, missing), updateAllowed,
 * desktop (whether the engine serves a desktop Studio), studioVersion</li>
 * <li>action=install, name, importUrl, forceUpdate: imports one of these projects from its URL (a project URL of
 * the Studio, as name=https://github.com/...), unless it is already installed; only these three names, from the
 * repositories of convertigo on GitHub: a built release (.car) or a clone</li>
 * <li>action=manifest, channel (stable or preview): the built releases of the stack the published manifest of this
 * channel lists, as the Assistant reads it, with their import URLs</li>
 * </ul>
 */
@ServiceDefinition(name = "Stack", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class Stack extends JSonService {
	static final String ASSISTANT = "lib_ConvertigoAssistant";
	static final List<String> PROJECTS = List.of(ASSISTANT, "lib_ConvertigoMCP", "lib_ConvertigoAgentBridge");
	/** the page of the local Assistant, served by the engine */
	static final String LOCAL_ASSISTANT_PATH = "/projects/" + ASSISTANT + "/DisplayObjects/mobile/";

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var action = request.getParameter("action");
		if ("manifest".equals(action)) {
			manifest(request, response);
		} else if ("install".equals(action)) {
			install(request, response);
		}
		putState(response);
	}

	/** the manifests of the built releases of the stack, published with the Assistant */
	static final String MANIFEST_URL = "https://raw.githubusercontent.com/convertigo/c8oprj-lib-convertigo-assistant/refs/heads/master/stack-release-%s.json";

	private static void manifest(HttpServletRequest request, JSONObject response) throws Exception {
		var channel = "preview".equals(request.getParameter("channel")) ? "preview" : "stable";
		var get = new org.apache.http.client.methods.HttpGet(String.format(MANIFEST_URL, channel));
		String text;
		try (var answer = Engine.theApp.httpClient4.execute(get)) {
			var code = answer.getStatusLine().getStatusCode();
			if (code < 200 || code >= 300) {
				throw new ServiceException("The " + channel + " manifest of the Assistant cannot be read (HTTP " + code + ")");
			}
			text = org.apache.http.util.EntityUtils.toString(answer.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
		}
		var manifest = new JSONObject(text);
		var projects = new org.codehaus.jettison.json.JSONArray();
		var listed = manifest.optJSONArray("projects");
		for (int i = 0; listed != null && i < listed.length(); i++) {
			var item = listed.optJSONObject(i);
			if (item == null) {
				continue;
			}
			var name = item.optString("name");
			var importUrl = item.optString("importUrl");
			// only the projects of the stack, from the repositories of convertigo
			if (PROJECTS.contains(name) && fromConvertigo(name, importUrl)) {
				projects.put(new JSONObject().put("name", name).put("version", item.optString("version"))
						.put("tag", item.optString("tag")).put("importUrl", importUrl));
			}
		}
		response.put("manifest", new JSONObject().put("channel", channel).put("projects", projects));
	}

	/** a project URL of the project from a repository of convertigo on GitHub, its built release or its clone */
	static boolean fromConvertigo(String name, String importUrl) {
		try {
			var parser = new ProjectUrlParser(importUrl);
			if (!parser.isValid() || !name.equals(parser.getProjectName()) || parser.getGitUrl() == null) {
				return false;
			}
			var url = parser.getGitUrl();
			if (url.startsWith("git@github.com:convertigo/")) {
				return true;
			}
			var uri = new java.net.URI(url);
			return "https".equals(uri.getScheme()) && "github.com".equalsIgnoreCase(uri.getHost()) && uri.getRawUserInfo() == null
					&& uri.getPort() < 0 && uri.getRawPath() != null && uri.getRawPath().startsWith("/convertigo/")
					&& !uri.getRawPath().contains("..");
		} catch (Exception e) {
			return false;
		}
	}

	private static void install(HttpServletRequest request, JSONObject response) throws Exception {
		var name = request.getParameter("name");
		var importUrl = request.getParameter("importUrl");
		if (name == null || !PROJECTS.contains(name)) {
			throw new ServiceException("Only the projects of the local agent stack are installed: " + PROJECTS);
		}
		if (importUrl == null || importUrl.isBlank()) {
			throw new ServiceException("missing importUrl parameter");
		}
		var parser = new ProjectUrlParser(importUrl);
		if (!fromConvertigo(name, importUrl)) {
			throw new ServiceException("The import URL of " + name + " is not a release or a repository of " + name
					+ " from convertigo on GitHub: " + importUrl);
		}
		if (installed(name) != null && !Boolean.parseBoolean(request.getParameter("forceUpdate"))) {
			response.put("skipped", true);
			return;
		}
		var project = Engine.theApp.referencedProjectManager.importProject(parser, true);
		if (project == null) {
			throw new ServiceException("Unable to import " + name + " from " + importUrl);
		}
		response.put("installed", name);
	}

	private static Project installed(String name) {
		try {
			return Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, false);
		} catch (Exception e) {
			return null;
		}
	}

	private static void putState(JSONObject response) throws Exception {
		var projects = new JSONObject();
		boolean all = true;
		boolean versions = true;
		boolean loading = false;
		boolean updateAllowed = true;
		for (var name : PROJECTS) {
			var project = installed(name);
			var version = project == null ? "" : String.valueOf(project.getVersion());
			projects.put(name, new JSONObject().put("installed", project != null).put("version", version));
			all &= project != null;
			versions &= !version.isBlank() && !"null".equals(version);
			// a project still migrating as the engine starts, or present in the workspace and not loaded yet
			loading |= project == null && Engine.theApp.databaseObjectsManager.getAllProjectNamesList().contains(name)
					&& !MigrationManager.isProjectMigrated(name);
			// a project cloned from Git is updated by the user, not by the Assistant
			updateAllowed &= project == null || GitUtils.getWorkingDir(project.getDirFile()) == null;
		}
		response.put("projects", projects);
		response.put("state", all && versions ? "ready" : loading ? "loading" : "missing");
		response.put("updateAllowed", updateAllowed);
		response.put("localAssistantPath", LOCAL_ASSISTANT_PATH);
		// a desktop Studio, Eclipse or Electron, opens its local Assistant with the local agent of its one user
		response.put("desktop", Engine.isDesktopStudio());
		// the version the Assistant checks before it offers its agent, as the context of the Eclipse Studio gives it
		response.put("studioVersion", com.twinsoft.convertigo.engine.ProductVersion.fullProductVersion);
	}
}
