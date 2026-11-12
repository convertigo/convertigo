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

package com.twinsoft.convertigo.engine.admin.services.studio.registration;

import java.util.Properties;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.studio.StudioPsc;
import com.twinsoft.convertigo.engine.studio.StudioPsc.DeploymentKey;

/**
 * The Personal Studio Configuration (PSC) of a desktop Studio, which the Eclipse Studio asks for at its first
 * start: the Convertigo agent of the Assistant needs its owner.
 * <ul>
 * <li>action=state (default): desktop (whether the engine serves a desktop Studio), registered, anonymous, owner and
 * servers of the PSC of the workspace, or error; never its secret nor the passwords of its servers</li>
 * <li>action=save, psc: the PSC given at the registration, or pasted, written as the PSC of the workspace once
 * decoded, in a desktop Studio only; invalid tells why it is not</li>
 * </ul>
 */
@ServiceDefinition(name = "Psc", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class Psc extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if ("save".equals(request.getParameter("action"))) {
			if (!Engine.isDesktopStudio()) {
				throw new ServiceException("Only a desktop Studio has a Personal Studio Configuration");
			}
			try {
				StudioPsc.save(request.getParameter("psc"));
				response.put("saved", true);
			} catch (StudioPsc.PscException e) {
				response.put("invalid", e.getMessage());
			}
		}
		putState(response);
	}

	private static void putState(JSONObject response) throws Exception {
		response.put("desktop", Engine.isDesktopStudio());
		Properties properties;
		try {
			properties = StudioPsc.decode();
		} catch (StudioPsc.PscException e) {
			response.put("registered", false);
			if (StudioPsc.file().exists()) {
				response.put("error", e.getMessage());
			}
			return;
		}
		var anonymous = StudioPsc.isAnonymous(properties);
		response.put("registered", !anonymous);
		response.put("anonymous", anonymous);
		if (!anonymous) {
			response.put("owner", StudioPsc.owner(properties));
		}
		var servers = new JSONArray();
		for (int i = 1; properties.containsKey(DeploymentKey.adminUser.key(i)); i++) {
			var server = DeploymentKey.server.value(properties, i);
			if (server != null && !server.isEmpty()) {
				servers.put(server);
			}
		}
		response.put("servers", servers);
	}
}
