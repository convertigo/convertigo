/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.engine;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.sessions.ConvertigoHttpSessionManager;
import com.twinsoft.convertigo.engine.sessions.RedisInstanceDiscovery;

@ServiceDefinition(
		name = "ListInstances",
		roles = {
				Role.AUTHENTICATED
		},
		parameters = {},
		returnValue = "cluster instances list"
)
public class ListInstances extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		RedisInstanceDiscovery.updateFromRequest(request);
		var localId = RedisInstanceDiscovery.getLocalInstanceId();
		var localBaseUrl = RedisInstanceDiscovery.getLocalBaseUrl();

		response.put("storeMode", ConvertigoHttpSessionManager.getInstance().getStoreMode().name());
		response.put("localInstanceId", localId != null ? localId : "");
		response.put("localBaseUrl", localBaseUrl != null ? localBaseUrl : "");

		var now = System.currentTimeMillis();
		var instances = new JSONArray();
		for (var info : RedisInstanceDiscovery.listInstances()) {
			var obj = new JSONObject();
			obj.put("instanceId", info.instanceId != null ? info.instanceId : "");
			obj.put("baseUrl", info.baseUrl != null ? info.baseUrl : "");
			obj.put("startedAt", info.startedAt);
			obj.put("lastHeartbeat", info.lastHeartbeat);
			obj.put("ageSeconds", info.lastHeartbeat > 0 ? Math.max(0L, (now - info.lastHeartbeat) / 1000L) : -1L);
			instances.put(obj);
		}
		response.put("instances", instances);
	}
}
