/**
 * 
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
