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
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Declares the global symbols the objects of a project use and the engine does not know, as the Eclipse
 * Studio does: each one is added with an empty value.
 * <ul>
 * <li>projectName: the project</li>
 * </ul>
 */
@ServiceDefinition(name = "DeclareSymbols", roles = { Role.WEB_ADMIN, Role.SYMBOLS_CONFIG }, parameters = {}, returnValue = "")
public class DeclareSymbols extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var projectName = request.getParameter("projectName");
		if (projectName == null || Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName) == null) {
			throw new ServiceException("The project " + projectName + " does not exist.");
		}
		var symbols = Engine.theApp.databaseObjectsManager.symbolsGetUndefined(projectName);
		if (!symbols.isEmpty()) {
			Engine.theApp.databaseObjectsManager.symbolsCreateUndefined(projectName);
		}
		response.put("symbols", new JSONArray(symbols));
	}
}
