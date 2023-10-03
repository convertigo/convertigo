/**
 * 
 */
package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;

@ServiceDefinition(name = "Remove", roles = { Role.WEB_ADMIN,
		Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Remove extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {

		// id the id of the bean in tree
		var id = request.getParameter("id");
		if (id == null) {
			throw new ServiceException("missing id parameter");
		}

		DatabaseObject dbo = Utils.getDbo(id);
		if (dbo instanceof Project) {
			// TODO
			response.put("done", false);
		} else {
			dbo.getParent().remove(dbo);
			response.put("done", true);
		}
	}
}
