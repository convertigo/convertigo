/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.admin.services.studio.tags;

import jakarta.servlet.http.HttpServletRequest;
import org.codehaus.jettison.json.JSONObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.tags.TagDocument;
import com.twinsoft.convertigo.engine.tags.TagManager;

@ServiceDefinition(name = "Apply", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Apply extends JSonService {
	@Override protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var input = request.getParameter("input");
		if (input == null) throw new IllegalArgumentException("Missing tag command input");
		var result = TagManager.get().mutate(TagManager.Scope.valueOf(request.getParameter("scope")),
				request.getParameter("project"), request.getParameter("revision"), request.getParameter("action"), TagDocument.parseObject(input));
		var json = new JSONObject(result.toString()); var keys = json.keys();
		while (keys.hasNext()) { String key = (String) keys.next(); response.put(key, json.get(key)); }
	}
}
