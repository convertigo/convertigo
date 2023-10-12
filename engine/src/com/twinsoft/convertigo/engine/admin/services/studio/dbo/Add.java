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
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;

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

		JSONObject jsonData = new JSONObject(data);
		DatabaseObject dbo = DboUtils.createDbo(jsonData);
		if (dbo != null) {
			DatabaseObject targetDbo = Utils.getDbo(target);
			
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
			
			if (parentDbo instanceof IContainerOrdered) {
				((IContainerOrdered)parentDbo).add(dbo, after);
			} else {
				parentDbo.add(dbo);
			}

			response.put("done", true);
			response.put("id", dbo.getFullQName());
		} else {
			response.put("done", false);
		}
	}
}
