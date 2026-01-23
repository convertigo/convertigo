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

package com.twinsoft.convertigo.engine.admin.services.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.SessionAttribute;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager.FullSyncAuthentication;

@ServiceDefinition(
		name = "EchoHttp",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = "",
		admin = false,
		allow_cors = true
		)
public class Get extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		HttpSession session = request.getSession(false);
		response.put("authenticated", false);
		if (session != null) {
			String user = null;
			try {
				FullSyncAuthentication fsauth = Engine.theApp.couchDbManager.getFullSyncAuthentication(session);
				user = fsauth.getAuthenticatedUser();
				response.put("groups", new JSONArray(fsauth.getGroups()));
			} catch (Exception e) {
				user = SessionAttribute.authenticatedUser.string(session);
				if (StringUtils.isBlank(user)) {
					user = null;
				}
			}
			if (user != null) {
				response.put("authenticated", true);
				response.put("user", user);
			}
			response.put("session", session.getId());
			response.put("maxInactive", session.getMaxInactiveInterval());
		}
	}
}
