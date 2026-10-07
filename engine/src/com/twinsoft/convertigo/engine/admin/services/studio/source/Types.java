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
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * The type definitions of the packages of the NGX application of a project, which the code editor of the
 * Studio gives to its TypeScript checker for the completion, as the TypeScript editor of the Eclipse Studio
 * knows the packages of the project: the .d.ts files and the package.json of each package the application
 * depends on, and of the packages they depend on.
 * <ul>
 * <li>project: the project</li>
 * </ul>
 */
@ServiceDefinition(name = "Types", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "", available_without_web_studio = true)
public class Types extends JSonService {
	/** the size the definitions have at most */
	private static final long MAX_SIZE = 16L * 1024 * 1024;
	/** the folders of a package without definitions the application uses */
	private static final Set<String> SKIPPED = Set.of("node_modules", "locales", "schematics", "fesm2022", "fesm2020",
			"bundles");

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(request.getParameter("project"));
		if (project == null) {
			throw new ServiceException("The project " + request.getParameter("project") + " does not exist.");
		}
		var ionic = new File(project.getDirFile(), "_private/ionic");
		var modules = new File(ionic, "node_modules");
		var files = new JSONObject();
		if (!modules.isDirectory()) {
			response.put("files", files);
			return;
		}
		// the packages of the application, then the ones they depend on
		var packages = new LinkedHashSet<String>(dependencies(new File(ionic, "package.json")));
		for (var name : new java.util.ArrayList<>(packages)) {
			packages.addAll(dependencies(new File(modules, name + "/package.json")));
		}
		long[] size = { 0 };
		for (var name : packages) {
			var folder = new File(modules, name);
			if (!folder.isDirectory()) {
				continue;
			}
			var types = new File(modules, "@types/" + (name.startsWith("@") ? name.substring(1).replace('/', '_') : name));
			for (var root : new File[] { folder, types }) {
				var packageJson = new File(root, "package.json");
				if (packageJson.isFile()) {
					add(files, modules, packageJson, size);
					collect(files, modules, root, size);
				}
			}
			if (size[0] > MAX_SIZE) {
				response.put("truncated", true);
				break;
			}
		}
		response.put("files", files);
	}

	private static Set<String> dependencies(File packageJson) {
		var names = new LinkedHashSet<String>();
		try {
			var json = new JSONObject(Files.readString(packageJson.toPath(), StandardCharsets.UTF_8));
			for (var key : new String[] { "dependencies", "peerDependencies" }) {
				var dependencies = json.optJSONObject(key);
				for (var it = dependencies == null ? java.util.Collections.emptyIterator() : dependencies.keys(); it.hasNext();) {
					names.add(String.valueOf(it.next()));
				}
			}
		} catch (Exception e) {
			// no dependency
		}
		return names;
	}

	private static void collect(JSONObject files, File modules, File folder, long[] size) throws Exception {
		var children = folder.listFiles();
		for (var child : children == null ? new File[0] : children) {
			if (child.isDirectory()) {
				if (!SKIPPED.contains(child.getName())) {
					collect(files, modules, child, size);
				}
			} else if (child.getName().endsWith(".d.ts") || child.getName().endsWith(".d.mts")) {
				add(files, modules, child, size);
			}
		}
	}

	private static void add(JSONObject files, File modules, File file, long[] size) throws Exception {
		if (size[0] + file.length() > MAX_SIZE) {
			return;
		}
		size[0] += file.length();
		var path = "node_modules/" + modules.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
		files.put(path, Files.readString(file.toPath(), StandardCharsets.UTF_8));
	}
}
