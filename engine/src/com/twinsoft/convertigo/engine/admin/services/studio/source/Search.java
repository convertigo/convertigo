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

package com.twinsoft.convertigo.engine.admin.services.studio.source;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Searches a text in the files of the projects, as the file search of the Eclipse Studio: the lines of the
 * text files matching a text or a regular expression, without the generated and the installed folders.
 * <ul>
 * <li>text: the text or the regular expression</li>
 * <li>matchCase, regExp: true to match the case, to take the text as a regular expression</li>
 * <li>scope: a project, all the projects else</li>
 * </ul>
 */
@ServiceDefinition(name = "Search", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Search extends JSonService {
	/** the lines a search gives at most */
	private static final int MAX_RESULTS = 500;
	/** the folders the Studio does not search: generated, installed or versioned files */
	private static final Set<String> SKIPPED = Set.of("_private", "node_modules", ".git", "DisplayObjects", "_data",
			"oas3", "oas2", "build", "dist", "www");

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var text = request.getParameter("text");
		if (text == null || text.isEmpty()) {
			throw new ServiceException("missing text parameter");
		}
		var matchCase = "true".equals(request.getParameter("matchCase"));
		Predicate<String> matches;
		if ("true".equals(request.getParameter("regExp"))) {
			var pattern = Pattern.compile(text, matchCase ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			matches = line -> pattern.matcher(line).find();
		} else {
			var lower = text.toLowerCase();
			matches = matchCase ? line -> line.contains(text) : line -> line.toLowerCase().contains(lower);
		}
		var scope = request.getParameter("scope");
		var names = scope == null || scope.isBlank() ? Engine.theApp.databaseObjectsManager.getAllProjectNamesList()
				: java.util.List.of(scope);
		var results = new JSONArray();
		var truncated = false;
		for (var name : names) {
			com.twinsoft.convertigo.beans.core.Project project;
			try {
				project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name);
			} catch (Exception e) {
				// a project not loadable now, as one being migrated, is not searched
				continue;
			}
			if (project == null) {
				continue;
			}
			var root = project.getDirFile().getCanonicalFile();
			truncated = search(root, root, name, matches, results);
			if (truncated) {
				break;
			}
		}
		response.put("results", results);
		response.put("truncated", truncated);
	}

	/**
	 * @return whether the search stopped, having found the lines it gives at most
	 */
	private static boolean search(File root, File folder, String project, Predicate<String> matches, JSONArray results)
			throws Exception {
		var files = folder.listFiles();
		if (files == null) {
			return false;
		}
		java.util.Arrays.sort(files);
		for (var file : files) {
			if (file.isDirectory()) {
				if (!SKIPPED.contains(file.getName()) && !file.getName().startsWith(".")
						&& search(root, file, project, matches, results)) {
					return true;
				}
			} else if (ProjectFiles.isText(file)) {
				var path = root.toPath().relativize(file.toPath()).toString().replace('\\', '/');
				// a file in another encoding is read with its unknown bytes replaced
				var lines = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).split("\\R", -1);
				for (var i = 0; i < lines.length; i++) {
					var line = lines[i];
					if (matches.test(line)) {
						var shown = line.strip();
						results.put(new JSONObject()
								.put("id", project + "//" + path)
								.put("project", project)
								.put("path", path)
								.put("line", i + 1)
								.put("text", shown.length() > 200 ? shown.substring(0, 200) + "…" : shown));
						if (results.length() >= MAX_RESULTS) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
