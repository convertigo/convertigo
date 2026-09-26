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

package com.twinsoft.convertigo.engine.admin.services.studio.tutorial;

import java.io.File;

import jakarta.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.ngx.components.dynamic.IonBean;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.CachedIntrospector;

/**
 * Checks the controls of a step of a Studio tutorial, as the Tutorial view of the Eclipse Studio does before
 * it lets the tutorial go on: an object exists, a property matches, a file exists, the project is deployed,
 * a link or the application is open.
 * <ul>
 * <li>controls: the controls of the step, as the tutorial declares them</li>
 * <li>lastDeployment, lastLink, previewProject, previewUrl: what the Studio did last</li>
 * </ul>
 */
@ServiceDefinition(name = "Check", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class Check extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var controls = new JSONArray(String.valueOf(request.getParameter("controls")));
		var ok = controls.length() > 0;
		for (var i = 0; ok && i < controls.length(); i++) {
			ok = check(controls.getJSONObject(i), request);
		}
		response.put("ok", ok);
	}

	private static boolean check(JSONObject control, HttpServletRequest request) {
		try {
			var manager = Engine.theApp.databaseObjectsManager;
			switch (control.optString("type")) {
			case "qnameExists" -> {
				return manager.getDatabaseObjectByQName(control.getString("qname")) != null;
			}
			case "property" -> {
				var dbo = manager.getDatabaseObjectByQName(control.getString("qname"));
				if (dbo == null) {
					return false;
				}
				var name = control.getString("name");
				var expression = control.getString("expression");
				for (var descriptor : CachedIntrospector.getBeanInfo(dbo.getClass()).getPropertyDescriptors()) {
					if (descriptor.getName().equals(name)) {
						var value = descriptor.getReadMethod().invoke(dbo);
						return value != null && value.toString().matches(expression);
					}
				}
				// a property of an NGX component, as mode:value
				var ionBean = (IonBean) dbo.getClass().getMethod("getIonBean").invoke(dbo);
				var ionProperty = ionBean.getProperty(name);
				return (ionProperty.getMode() + ":" + ionProperty.getSmartValue()).matches(expression);
			}
			case "fileExists" -> {
				var dir = new File(Engine.projectDir(control.getString("project")), control.getString("subDir"));
				var files = dir.listFiles();
				var expression = control.getString("fileExpression");
				for (var file : files == null ? new File[0] : files) {
					if (file.getName().matches(expression)) {
						return true;
					}
				}
				return false;
			}
			case "deployment" -> {
				return control.getString("project").equals(request.getParameter("lastDeployment"));
			}
			case "linkOpen" -> {
				var link = request.getParameter("lastLink");
				return link != null && !link.isEmpty() && link.matches(control.getString("expression"));
			}
			case "ngxEditorOpen" -> {
				var url = request.getParameter("previewUrl");
				return control.getString("project").equals(request.getParameter("previewProject")) && url != null
						&& url.matches(control.getString("url"));
			}
			default -> {
				// the checks running scripts in the application are left to the user, who can go on
				return false;
			}
			}
		} catch (Exception e) {
			Engine.logStudio.trace("(Tutorial) check " + control + " failed: " + e.getMessage());
			return false;
		}
	}
}
