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

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IContainerOrdered;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.BuilderUtils;

@ServiceDefinition(name = "Add", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Add extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// target: the id of the target bean in tree
		var target = request.getParameter("target");
		if (target == null) {
			throw new ServiceException("missing target parameter");
		}

		// position: the position where to add, relative to target (inside|first|after)
		var position = request.getParameter("position");
		if (position == null) {
			position = "inside";
		}

		// data : the json item object of Tree or Palette
		var data = request.getParameter("data");
		if (data == null) {
			throw new ServiceException("missing data parameter");
		}

		DatabaseObject targetDbo = DboUtils.findDbo(target);
		if (targetDbo != null) {
			Long after = null;
			DatabaseObject parentDbo;
			if (position.equals("inside")) {
				parentDbo = targetDbo;
			} else {
				parentDbo = targetDbo.getParent();
				after = targetDbo.priority;
				if (position.equals("first")) {
					after = 0L;
				}
			}

			DatabaseObject dbo = DboUtils.createDbo(new JSONObject(data), parentDbo);
			if (dbo != null && !dbo.equals(parentDbo)) {
				if (parentDbo instanceof IContainerOrdered) {
					((IContainerOrdered) parentDbo).add(dbo, after);
				} else {
					parentDbo.add(dbo);
				}
				
				// notify for app generation
				BuilderUtils.dboAdded(dbo);
			}
			
			response.put("done", true);
			response.put("id", dbo.getFullQName());
		} else {
			response.put("done", false);
		}
	}
}
