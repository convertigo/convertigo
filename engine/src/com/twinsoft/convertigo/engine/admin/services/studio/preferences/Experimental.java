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

package com.twinsoft.convertigo.engine.admin.services.studio.preferences;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.ExperimentalFeatures;

/**
 * The preference "Show new experimental features" of a web Studio user (native JSON sequences, native JSON steps,
 * JSON path sources), as the one of the Eclipse Studio but for the session of this user only: the palette, the
 * properties and the menus of the Studio services follow it during the calls of the session.
 * <ul>
 * <li>shown: true or false to change it, nothing to read it</li>
 * </ul>
 * Answers shown, the preference of the session, and engine, whether the engine shows them to everyone.
 */
@ServiceDefinition(name = "Experimental", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class Experimental extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var session = request.getSession();
		var shown = request.getParameter("shown");
		if (shown != null) {
			if (Boolean.parseBoolean(shown)) {
				session.setAttribute(ExperimentalFeatures.SESSION_ATTRIBUTE, Boolean.TRUE);
			} else {
				session.removeAttribute(ExperimentalFeatures.SESSION_ATTRIBUTE);
			}
		}
		response.put("shown", ExperimentalFeatures.isShownInSession(session.getAttribute(ExperimentalFeatures.SESSION_ATTRIBUTE)));
		response.put("engine", ExperimentalFeatures.isShownByEngine());
	}
}
