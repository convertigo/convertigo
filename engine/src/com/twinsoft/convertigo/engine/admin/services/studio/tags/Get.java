/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.admin.services.studio.tags;

import jakarta.servlet.http.HttpServletRequest;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.tags.TagManager;

@ServiceDefinition(name = "Get", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Get extends JSonService {
	@Override protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var scope = TagManager.Scope.valueOf(request.getParameter("scope"));
		var result = TagManager.get().read(scope, request.getParameter("project"));
		if (scope == TagManager.Scope.projectObjects) result.set("suggestions", TagManager.get().suggestions(request.getParameter("project")).path("suggestions"));
		var json = new JSONObject(result.toString()); var keys = json.keys();
		while (keys.hasNext()) { String key = (String) keys.next(); response.put(key, json.get(key)); }
	}
}
