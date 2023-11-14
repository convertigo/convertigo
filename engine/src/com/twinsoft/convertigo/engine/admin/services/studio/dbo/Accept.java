/*
 * Copyright (c) 2001-2023 Convertigo SA.
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
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.enums.FolderType;

@ServiceDefinition(name = "Accept", roles = { Role.WEB_ADMIN,
		Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Accept extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// action: the drag action (copy|move)
		var action = request.getParameter("action");
		if (action == null) {
			throw new ServiceException("missing action parameter");
		}
		
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
		DatabaseObject dbo = null;

		JSONObject jsonData = new JSONObject(data);
		var type = jsonData.has("type") ? jsonData.getString("type") : "";
		if (type.equals("paletteData")) {
			dbo = DboUtils.createDbo(jsonData, targetDbo);
		} else if (type.equals("treeData")) {
			if (action.equals("move")) {
				JSONObject jsonItem = jsonData.getJSONObject("data");
				dbo = DboUtils.findDbo(jsonItem.getString("id"));
			} else {
				dbo = DboUtils.createDbo(jsonData, targetDbo);
			}
		}

		boolean accept = false;
		if (targetDbo != null && dbo != null) {
			FolderType folderType = position.equals("inside") ? Utils.getFolderType(target) : targetDbo.getFolderType();
			DatabaseObject parentDbo = position.equals("inside") ? targetDbo : targetDbo.getParent();
			if (parentDbo != null) {
				accept = dbo.getParent() != null && action.equals("move") ? DboUtils.canCut(dbo) : true;
				if (accept) {
					accept = DboUtils.acceptDbo(parentDbo, dbo, action.equals("copy"));
					if (accept && action.equals("move")) {
						if (folderType != null) {
							accept = DatabaseObject.getFolderType(dbo.getClass()) == folderType;
						}
						if (accept && position.equals("inside")) {
							accept = dbo.getParent() == null || !parentDbo.equals(dbo.getParent());
						}
					}
				}
			}
		}
		response.put("accept", accept);
	}
}
