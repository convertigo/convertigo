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
import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "GetResources",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = ""
	)
public class GetResources extends JSonService {
	enum Keys {
		application,
		platform,
		uuid,
		flashUpdateEnabled,
		requireUserConfirmation;
	};
	
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		String application = request.getParameter(Keys.application.toString());
		String platform = request.getParameter(Keys.platform.toString());
		String uuid = request.getParameter(Keys.uuid.toString());
		
		Engine.logAdmin.debug("(mobile.GetResources) Requested for application " + application + " by the platform " + platform + " and the uuid " + uuid);
		
		final MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(application, "_private/flashupdate");
		
		if (mobileResourceHelper.mobileApplication.getEnableFlashUpdate()) {
			response.put(Keys.flashUpdateEnabled.toString(), true);
			response.put(Keys.requireUserConfirmation.toString(), mobileResourceHelper.mobileApplication.getRequireUserConfirmation());
			
			boolean changed = false;
			if (Engine.isStudioMode() && mobileResourceHelper.destDir.exists()) {
				try {
					FileUtils.listFiles(mobileResourceHelper.mobileDir, new IOFileFilter() {

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
				} catch (RuntimeException e) {
					changed = true;
				}
			}

			if (!mobileResourceHelper.destDir.exists() || changed) {
				mobileResourceHelper.prepareFiles(request, new FileFilter() {
					
					public boolean accept(File pathname) {
						boolean ok = MobileResourceHelper.defaultFilter.accept(pathname) &&
							! new File(mobileResourceHelper.mobileDir, "config.xml").equals(pathname) &&
							! new File(mobileResourceHelper.mobileDir, "res").equals(pathname);
						return ok;
					}
					
				});
			}
			
			listFiles(mobileResourceHelper.destDir, response);
		} else {
			response.put(Keys.flashUpdateEnabled.toString(), false);
		}
	}
	
	private void listFiles(File directory, JSONObject response) throws JSONException, IOException {
		File canonicalDir = directory.getCanonicalFile();
		int uriDirectoryLength = canonicalDir.toURI().toString().length();
		JSONArray jArray = new JSONArray();
		
		 for (File f : FileUtils.listFiles(canonicalDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
			 File canonnicalF = f.getCanonicalFile();
			 JSONObject jObj = new JSONObject();
			 URI uriFile = canonnicalF.toURI();
			 jObj.put("uri", uriFile.toString().substring(uriDirectoryLength));
			 jObj.put("date", canonnicalF.lastModified());
			 jObj.put("size", canonnicalF.length());
			 jArray.put(jObj);
		 }
		 response.put("files", jArray);
	}
	 
}
