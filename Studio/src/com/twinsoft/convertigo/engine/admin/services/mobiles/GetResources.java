/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.admin.services.mobiles;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.JsonObject;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper.Keys;

@ServiceDefinition(
		name = "GetResources",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = ""
	)
public class GetResources extends JSonService {
	
	protected void getServiceResult(HttpServletRequest request, JsonObject response) throws Exception {
		String project = Keys.project.value(request);
		String platform = Keys.platform.value(request);
		String uuid = Keys.uuid.value(request);
		
		// request the session to set the affinity
		request.getSession();
		
		Engine.logAdmin.debug("(mobile.GetResources) Requested for project " + project + " by the platform " + platform + " and the uuid " + uuid);
		
		final MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(request, "mobile/flashupdate");
		
		if (mobileResourceHelper.mobileApplication.getEnableFlashUpdate()) {
			response.addProperty(Keys.flashUpdateEnabled.name(), true);
			response.addProperty(Keys.requireUserConfirmation.name(), mobileResourceHelper.mobileApplication.getRequireUserConfirmation());
			
			mobileResourceHelper.prepareFilesForFlashupdate();
			
			mobileResourceHelper.listFiles(response);
		} else {
			response.addProperty(Keys.flashUpdateEnabled.name(), false);
		}
		
		JsonObject env = new JsonObject();
		env.addProperty("currentRevision", mobileResourceHelper.destDir.lastModified());
		env.addProperty("currentVersion", mobileResourceHelper.mobileApplication.getComputedApplicationVersion());
		response.add("resourcesEnv", env);
		
		env = new JsonObject();
		env.addProperty("remoteRevision", mobileResourceHelper.destDir.lastModified());
		env.addProperty("remoteVersion", mobileResourceHelper.mobileApplication.getComputedApplicationVersion());
		env.addProperty("splashRemoveMode", mobileResourceHelper.mobileApplication.getSplashRemoveMode().name());
		response.add("env", env);
	}	 
}
