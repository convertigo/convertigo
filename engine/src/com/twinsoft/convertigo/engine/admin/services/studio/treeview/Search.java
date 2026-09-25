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

package com.twinsoft.convertigo.engine.admin.services.studio.treeview;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.enums.DatabaseObjectTypes;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.YamlConverter;

/**
 * Finds the objects whose definition contains a text, as the Convertigo search of the Eclipse Studio: each
 * object of the projects, or of the object given as scope, is written as YAML and searched.
 * <ul>
 * <li>text: the searched text, or a regular expression with regExp=true</li>
 * <li>matchCase: true for a case sensitive search</li>
 * <li>type: a database object type (Sequence, Step, Transaction...), or * for all</li>
 * <li>scope: the tree id of the object to search in, the whole workspace when missing</li>
 * <li>limit: the maximum number of results (500 by default)</li>
 * </ul>
 */
@ServiceDefinition(name = "Search", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Search extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var text = request.getParameter("text");
		if (text == null || text.isEmpty()) {
			throw new ServiceException("missing text parameter");
		}
		var matchCase = "true".equals(request.getParameter("matchCase"));
		var regExp = "true".equals(request.getParameter("regExp"));
		var type = request.getParameter("type");
		if (type == null || type.isBlank()) {
			type = "*";
		} else if (!"*".equals(type)) {
			DatabaseObjectTypes.valueOf(type);
		}
		var limit = 500;
		try {
			limit = Math.max(1, Integer.parseInt(request.getParameter("limit")));
		} catch (Exception e) {
			// the default limit
		}

		Pattern pattern = null;
		if (regExp) {
			try {
				pattern = matchCase ? Pattern.compile(text) : Pattern.compile(text, Pattern.CASE_INSENSITIVE);
			} catch (PatternSyntaxException e) {
				throw new ServiceException("The regular expression is not valid: " + e.getDescription());
			}
		}
		var searched = matchCase ? text : text.toLowerCase();

		List<DatabaseObject> roots = new ArrayList<>();
		var scope = request.getParameter("scope");
		if (scope != null && !scope.isBlank()) {
			var root = Utils.getDbo(scope);
			if (root == null) {
				throw new ServiceException("The object " + scope + " does not exist.");
			}
			roots.add(root);
		} else {
			var dbom = Engine.theApp.databaseObjectsManager;
			for (var projectName : dbom.getAllProjectNamesList(true)) {
				var project = dbom.getOriginalProjectByName(projectName, true);
				if (project != null) {
					roots.add(project);
				}
			}
		}

		var results = new JSONArray();
		var truncated = new boolean[] { false };
		var objectType = type;
		var matcher = pattern == null ? null : pattern.matcher("");
		var max = limit;
		for (var root : roots) {
			if (truncated[0]) {
				break;
			}
			new WalkHelper() {

				@Override
				protected void walk(DatabaseObject databaseObject) throws Exception {
					if (truncated[0]) {
						return;
					}
					if ("*".equals(objectType) || objectType.equals(databaseObject.getDatabaseType())) {
						String definition;
						try {
							definition = YamlConverter.toYaml(databaseObject.toXml(XMLUtils.createDom()));
						} catch (Exception e) {
							definition = databaseObject.toString();
						}
						var found = matcher != null
								? matcher.reset(definition).find()
								: (matchCase ? definition : definition.toLowerCase()).contains(searched);
						if (found) {
							if (results.length() >= max) {
								truncated[0] = true;
								return;
							}
							results.put(result(databaseObject));
						}
					}
					super.walk(databaseObject);
				}
			}.init(root);
		}
		response.put("results", results);
		response.put("truncated", truncated[0]);
	}

	private static JSONObject result(DatabaseObject dbo) throws Exception {
		var names = new ArrayList<String>();
		for (var parent = dbo.getParent(); parent != null; parent = parent.getParent()) {
			names.add(0, parent.getName());
		}
		var path = new JSONArray(names);
		return new JSONObject()
				.put("id", dbo.getQName(true))
				.put("name", dbo.getName())
				.put("type", dbo.getDatabaseType())
				.put("project", dbo.getProject() == null ? "" : dbo.getProject().getName())
				.put("path", path)
				.put("icon", "studio.dbo.GetIcon?iconPath=" + Get.iconPath(dbo));
	}
}
