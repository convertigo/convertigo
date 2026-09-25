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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.util.HashMap;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.ngx.components.UIComponent;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

/**
 * Creates a shared component from an NGX component, as the "Create a shared component from the selected
 * object" action of the Eclipse Studio.
 * <ul>
 * <li>id: the component</li>
 * <li>name: missing to get the proposed name and the variables the component uses; else the name of the
 * shared component to create</li>
 * <li>keep: true to keep the component disabled, false to remove it</li>
 * <li>variables: the names of the variables of the shared component, as {found variable: name}</li>
 * </ul>
 */
@ServiceDefinition(name = "SharedComponent", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class SharedComponent extends JSonService {

	/**
	 * @return whether the object can become a shared component
	 */
	public static boolean allows(com.twinsoft.convertigo.beans.core.DatabaseObject dbo) {
		return SharedComponentExtraction.allows(dbo);
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var dbo = id == null ? null : Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(id);
		if (!SharedComponentExtraction.allows(dbo)) {
			throw new ServiceException("The object " + id + " cannot become a shared component.");
		}
		var extraction = new SharedComponentExtraction(List.of((UIComponent) dbo));
		var name = request.getParameter("name");
		if (name == null) {
			response.put("name", extraction.getDefaultName());
			var variables = new JSONArray();
			for (var variable : extraction.getItemMap().entrySet()) {
				variables.put(new JSONObject().put("name", variable.getKey()).put("info", variable.getValue()));
			}
			response.put("variables", variables);
			return;
		}
		var names = new HashMap<String, String>();
		var chosen = new JSONObject(request.getParameter("variables") == null ? "{}" : request.getParameter("variables"));
		for (var keys = chosen.keys(); keys.hasNext();) {
			var key = String.valueOf(keys.next());
			names.put(key, chosen.optString(key, key));
		}
		var shared = extraction.extract(com.twinsoft.convertigo.engine.util.StringUtils.normalize(name),
				!"false".equals(request.getParameter("keep")), names);
		response.put("done", true);
		response.put("id", shared.getFullQName());
	}
}
