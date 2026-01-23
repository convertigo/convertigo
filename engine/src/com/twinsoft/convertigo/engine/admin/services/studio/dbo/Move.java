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

@ServiceDefinition(name = "Move", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Move extends JSonService {

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

		// data : the json item object of Tree
		var data = request.getParameter("data");
		if (data == null) {
			throw new ServiceException("missing data parameter");
		}

		boolean done = false;
		
		JSONObject jsonData = new JSONObject(data);
		var type = jsonData.has("type") ? jsonData.getString("type") : "";
		if (type.equals("treeData")) {
			JSONObject jsonItem = jsonData.getJSONObject("data");
			DatabaseObject dbo = DboUtils.findDbo(jsonItem.getString("id"));
			if (dbo != null) {
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
					if (!dbo.equals(parentDbo)) {
						boolean isOrdering = parentDbo.equals(dbo.getParent());
						boolean isMoving = !isOrdering && DboUtils.canCut(dbo);
						if (isOrdering || isMoving) {
							DatabaseObject previousParent = dbo.getParent();
							DatabaseObject previousSibling = dbo.getPreviousSiblingInFolder();
							try {
								dbo.delete();
								if (parentDbo instanceof IContainerOrdered) {
									((IContainerOrdered) parentDbo).add(dbo, after);
								} else {
									parentDbo.add(dbo);
								}
								done = true;
								
								// notify for app generation
								if (isOrdering) {
									BuilderUtils.dboUpdated(parentDbo);
								} else {
									BuilderUtils.dboMoved(previousParent, parentDbo, dbo);
								}
							} catch (Exception e) {
								if (dbo.getParent() == null && previousParent != null) {
									after = previousSibling == null ? 0L : previousSibling.priority;
									if (previousParent instanceof IContainerOrdered) {
										((IContainerOrdered) previousParent).add(dbo, after);
									} else {
										previousParent.add(dbo);
									}
								}
							}
						}
					}
				}			
			}
		}
		response.put("done", done);
	}
}
