/**
 * 
 */
package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.studio.Utils;
import com.twinsoft.convertigo.engine.enums.FolderType;

@ServiceDefinition(name = "Accept", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Accept extends JSonService {

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
		DatabaseObject targetDbo = Utils.getDbo(target);
		FolderType folderType = position.equals("inside") ? Utils.getFolderType(target) : targetDbo.getFolderType();
		DatabaseObject parentDbo = position.equals("inside") ? targetDbo : targetDbo.getParent();
		if (parentDbo != null && dbo != null) {
			boolean accept = accept(parentDbo, dbo);
			if (folderType != null && accept) {
				accept = DatabaseObject.getFolderType(dbo.getClass()) == folderType;
			}
			response.put("accept", accept);
		} else {
			response.put("accept", false);
		}
	}
	
	protected boolean accept(DatabaseObject targetDatabaseObject, DatabaseObject databaseObject) {
		if (!DatabaseObjectsManager.acceptDatabaseObjects(targetDatabaseObject, databaseObject)) {
			return false;
		}
		if (targetDatabaseObject instanceof com.twinsoft.convertigo.beans.mobile.components.MobileComponent) {
			return false;
		}
		if (targetDatabaseObject instanceof com.twinsoft.convertigo.beans.ngx.components.MobileComponent) {
			if (!com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.acceptDatabaseObjects(targetDatabaseObject, databaseObject)) {
				return false;
			}
			if (!com.twinsoft.convertigo.beans.ngx.components.dynamic.ComponentManager.isTplCompatible(targetDatabaseObject, databaseObject)) {
				return false;
			}
		}
		
		return true;
	}
}
