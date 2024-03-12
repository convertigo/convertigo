/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "GetJavaSystemProperties",
		roles = { Role.TEST_PLATFORM, Role.HOME_VIEW, Role.HOME_CONFIG },
		parameters = {},
		returnValue = "the Java system properties"
	)
public class GetJavaSystemPropertiesJson extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var array = new JSONArray();
		var properties = System.getProperties();
		for (var propertyName : new TreeSet<Object>(properties.keySet())) {
			if (propertyName instanceof String pName
					&& (!Engine.isCloudMode() || !(pName.contains("password") || pName.contains("billing")))) {
				var o = new JSONObject();
				o.put("name", pName);
				o.put("value", properties.getProperty(pName));
				array.put(o);
			}
		}
		response.put("properties", array);
	}
}
