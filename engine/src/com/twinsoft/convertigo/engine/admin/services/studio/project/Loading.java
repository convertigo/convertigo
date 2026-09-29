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

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.MigrationManager;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.git.SourceControl;

/**
 * Whether the engine still loads the projects of the workspace as it starts: the web Studio, opened as
 * soon as the engine answers, lists its projects again once they are loaded. The projects whose files have
 * conflicts of Git, which do not load, are loaded then as HEAD has them.
 * <ul>
 * <li>loading: true while projects migrate</li>
 * <li>pending: the projects that still migrate</li>
 * <li>loaded: once none migrates, the projects loaded, as projects.List gives them</li>
 * </ul>
 */
@ServiceDefinition(name = "Loading", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class Loading extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var names = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
		var pending = new JSONArray();
		for (var name : names) {
			if (!MigrationManager.isProjectMigrated(name)) {
				pending.put(name);
			}
		}
		var loaded = new JSONArray();
		if (pending.length() == 0) {
			for (var name : names) {
				try {
					// loaded, from the cache, or as HEAD has it when Git filled its files with markers
					if (SourceControl.project(name) != null) {
						loaded.put(name);
					}
				} catch (Exception e) {
					Engine.logStudio.debug("(Loading) the project " + name + " does not load", e);
				}
			}
		}
		response.put("loading", pending.length() > 0);
		response.put("pending", pending);
		response.put("loaded", loaded);
	}
}
