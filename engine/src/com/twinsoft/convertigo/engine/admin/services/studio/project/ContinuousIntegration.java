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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ProductVersion;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Puts in a project the continuous integration files Convertigo publishes for its version, as the Update
 * CI actions of the Eclipse Studio: the Gradle build with the GitLab, CircleCI or GitHub Actions
 * configuration, or the .httpignore file. An existing file that differs is kept as a dated .bak file.
 * <ul>
 * <li>projectName: the project</li>
 * <li>type: gitlab, circleci, github-actions, gradle or httpignore</li>
 * </ul>
 */
@ServiceDefinition(name = "ContinuousIntegration", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class ContinuousIntegration extends JSonService {
	private static final String BASE_URL = "https://github.com/convertigo/convertigo-common-resources/raw/"
			+ ProductVersion.productVersion + "/";
	private static final Set<String> TYPES = Set.of("gitlab", "circleci", "github-actions", "gradle", "httpignore");

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		var project = projectName == null ? null : Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
		if (project == null) {
			throw new ServiceException("Unknown project " + projectName);
		}
		var type = request.getParameter("type");
		if (!TYPES.contains(type)) {
			throw new ServiceException("Unknown continuous integration " + type);
		}
		var update = new Update(project.getDirFile());
		update.files(BASE_URL + type + ".json");
		response.put("files", new JSONArray(update.written));
		response.put("backups", new JSONArray(update.backups));
		response.put("message", update.written.size() + " file" + (update.written.size() == 1 ? " is" : "s are")
				+ " updated in " + projectName
				+ (update.backups.isEmpty() ? "." : ", the former ones are kept as " + update.suffix + " files."));
	}

	private static class Update {
		final File dir;
		final String suffix = "." + new SimpleDateFormat("yy-MM-dd_HH-mm-ss").format(new Date()) + ".bak";
		final Set<String> written = new TreeSet<>();
		final Set<String> backups = new TreeSet<>();

		Update(File dir) {
			this.dir = dir;
		}

		void files(String url) throws Exception {
			var json = new JSONObject(new String(get(url), StandardCharsets.UTF_8));
			var imports = json.optJSONArray("imports");
			for (var i = 0; imports != null && i < imports.length(); i++) {
				files(BASE_URL + imports.getString(i));
			}
			var files = json.optJSONArray("files");
			for (var i = 0; files != null && i < files.length(); i++) {
				var file = files.getJSONObject(i);
				write(BASE_URL + file.getString("from"), file.getString("to"), file.optBoolean("backup"));
			}
		}

		void write(String url, String path, boolean backup) throws Exception {
			var dest = new File(dir, path);
			if (!dest.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator)) {
				throw new ServiceException("The file " + path + " is outside the project.");
			}
			var content = get(url);
			if (dest.exists() && backup) {
				var former = FileUtils.readFileToString(dest, StandardCharsets.UTF_8).replaceAll("[\\s\\n\\r]+", "");
				var next = new String(content, StandardCharsets.UTF_8).replaceAll("[\\s\\n\\r]+", "");
				if (!former.equals(next)) {
					var bak = new File(dest.getParentFile(), dest.getName() + suffix);
					FileUtils.deleteQuietly(bak);
					FileUtils.moveFile(dest, bak);
					backups.add(path);
				}
			}
			dest.getParentFile().mkdirs();
			FileUtils.writeByteArrayToFile(dest, content);
			if (dest.getName().equals("gradlew")) {
				// the wrapper runs as a command
				dest.setExecutable(true);
			}
			written.add(path);
		}

		static byte[] get(String url) throws Exception {
			var get = new HttpGet(url);
			try (var response = Engine.theApp.httpClient4.execute(get)) {
				var code = response.getStatusLine().getStatusCode();
				if (code != 200) {
					throw new EngineException("Code " + code + " for " + url);
				}
				return IOUtils.toByteArray(response.getEntity().getContent());
			}
		}
	}
}
