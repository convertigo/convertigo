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

import java.io.File;
import java.io.FileFilter;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.codehaus.jettison.json.JSONObject;

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
	
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		String project = Keys.project.value(request);
		String platform = Keys.platform.value(request);
		String uuid = Keys.uuid.value(request);
		
		Engine.logAdmin.debug("(mobile.GetResources) Requested for project " + project + " by the platform " + platform + " and the uuid " + uuid);
		
		final MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(request, "mobile/flashupdate");
		
		if (mobileResourceHelper.mobileApplication.getEnableFlashUpdate()) {
			response.put(Keys.flashUpdateEnabled.name(), true);
			response.put(Keys.requireUserConfirmation.name(), mobileResourceHelper.mobileApplication.getRequireUserConfirmation());
			
			boolean changed = false;
			if (Engine.isStudioMode() && mobileResourceHelper.destDir.exists()) {
				try {
					for (File directory: mobileResourceHelper.mobileDir) {
						FileUtils.listFiles(directory, new IOFileFilter() {

							public boolean accept(File file) {
								if (MobileResourceHelper.defaultFilter.accept(file)) {
									if (FileUtils.isFileNewer(file, mobileResourceHelper.destDir)) {
										throw new RuntimeException();
									}
									return true;
								} else {
									return false;
								}
							}

							public boolean accept(File file, String path) {
								return accept(new File(file, path));
							}
							
						}, MobileResourceHelper.defaultFilter);
					}
				} catch (RuntimeException e) {
					changed = true;
				}
			}

			if (!mobileResourceHelper.destDir.exists() || changed) {
				mobileResourceHelper.prepareFiles(new FileFilter() {
					
					public boolean accept(File pathname) {
						boolean ok = MobileResourceHelper.defaultFilter.accept(pathname) &&
							! new File(mobileResourceHelper.getCurrentMobileDir(), "config.xml").equals(pathname) &&
							! new File(mobileResourceHelper.getCurrentMobileDir(), "res").equals(pathname);
						return ok;
					}
					
				});
			}
			
			mobileResourceHelper.listFiles(response);
		} else {
			response.put(Keys.flashUpdateEnabled.name(), false);
		}
		
		response.put("splashRemoveMode", mobileResourceHelper.mobileApplication.getSplashRemoveMode().name());
		
		JSONObject env = new JSONObject();
		env.put("currentRevision", mobileResourceHelper.destDir.lastModified());
		env.put("currentVersion", mobileResourceHelper.mobileApplication.getComputedApplicationVersion());
		
		response.put("env", env);
	}	 
}
