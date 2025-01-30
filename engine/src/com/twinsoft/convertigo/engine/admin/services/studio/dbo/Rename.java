/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.studio.dbo;



import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;

@ServiceDefinition(name = "Rename", roles = { Role.WEB_ADMIN,
		Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Rename extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// id the id of the bean in tree
		var id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}

		String newName = request.getParameter("name");
		if (newName == null) {
			throw new ServiceException("missing name parameter");
		}
		
		String update = request.getParameter("update");
		if (update == null) {
			throw new ServiceException("missing update parameter");
		}
		
		boolean done = false;
		JSONArray ids = new JSONArray();
		DatabaseObject dbo = DboUtils.findDbo(id);
		if (dbo != null) {
			if (dbo instanceof Project) {
				// TODO
			} else {
				String oldName = dbo.getName();
				
				// changes bean name and does the refactoring
				done = DboUtils.changeBeanName(ids, dbo, oldName, newName, update);
				
				// notify for app generation
				if (done) {
					BuilderUtils.dboChanged(dbo, "name", oldName, newName);
				}
			}
		}
		
		response.put("done", done);
		response.put("ids", ids);
	}
}
