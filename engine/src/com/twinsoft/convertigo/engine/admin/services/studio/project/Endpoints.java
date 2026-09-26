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

package com.twinsoft.convertigo.engine.admin.services.studio.project;

import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.LinkedHashMap;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.HttpUtils;

/**
 * The endpoints a mobile application can use to reach the Convertigo server, as the endpoint editor of
 * the Eclipse Studio proposes them: the default endpoint of the engine, the URL of this Studio and its
 * addresses on the local networks. The Studio adds the servers it deploys to.
 * <ul>
 * <li>action: none to list them, setDefault (value) to update the default endpoint of the engine</li>
 * </ul>
 */
@ServiceDefinition(name = "Endpoints", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Endpoints extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if ("setDefault".equals(request.getParameter("action"))) {
			// the default endpoint is a setting of the engine
			if (!Engine.authenticatedSessionManager.hasRole(request.getSession(), Role.WEB_ADMIN)) {
				throw new ServiceException("Only an administrator updates the default endpoint.");
			}
			var value = request.getParameter("value");
			EnginePropertiesManager.setProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_ENDPOINT,
					value == null ? "" : value.trim());
			EnginePropertiesManager.saveProperties();
		}
		response.put("default", String.valueOf(MobileApplication.getDefaultServerEnpoint()));
		var endpoints = new LinkedHashMap<String, String>();
		endpoints.put(HttpUtils.convertigoRequestURL(request), "this Studio");
		var port = request.getLocalPort();
		for (var network : Collections.list(NetworkInterface.getNetworkInterfaces())) {
			if (!network.isUp()) {
				continue;
			}
			for (var address : Collections.list(network.getInetAddresses())) {
				if (address instanceof Inet4Address) {
					endpoints.putIfAbsent("http://" + address.getHostAddress() + ":" + port + "/convertigo",
							"from " + network.getDisplayName());
				}
			}
		}
		var list = new JSONArray();
		for (var endpoint : endpoints.entrySet()) {
			list.put(new JSONObject().put("url", endpoint.getKey()).put("from", endpoint.getValue()));
		}
		response.put("endpoints", list);
	}
}
