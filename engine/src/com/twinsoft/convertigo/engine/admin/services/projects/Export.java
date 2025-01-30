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

package com.twinsoft.convertigo.engine.admin.services.projects;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.FileUtils;

@ServiceDefinition(
		name = "Export",
		roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Export extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request,HttpServletResponse response) throws IOException, EngineException {
	
		String projectName = request.getParameter("projectName");
		String exportOptionsTxt = request.getParameter("exportOptions");
		
		HeaderName.ContentDisposition.setHeader(response, "attachment; filename=\"" + projectName + ".car\"");
		response.setContentType(MimeType.Zip.value());
		
		// if any, backup existing CAR file
		File f = new File(Engine.projectDir(projectName) + ".car");
		long lastDate = -1;
		if (f.exists()) {
			lastDate = f.lastModified();
			File oldFile = new File(Engine.projectDir(projectName) + ".car.old");
			FileUtils.deleteQuietly(oldFile);
			f.renameTo(oldFile);
		}

		if (!Engine.theApp.databaseObjectsManager.existsProject(projectName)) {
			throw new IllegalArgumentException("The project '" + projectName + "' does not exist!");
		}
		
		Set<ArchiveExportOption> exportOptions = ArchiveExportOption.all;
		
		if (exportOptionsTxt != null) {
			try {
				JSONObject exportOptionsJson = new JSONObject(exportOptionsTxt);
				exportOptions = new HashSet<ArchiveExportOption>(exportOptions);
				for (Iterator<?> i = exportOptionsJson.keys(); i.hasNext();) {
					String key = (String) i.next();
					try {
						ArchiveExportOption opt = ArchiveExportOption.valueOf(key);
						if (!exportOptionsJson.getBoolean(key)) {
							exportOptions.remove(opt);
						}
					} catch (Exception e) {
						Engine.logAdmin.warn("Export cannot find ArchiveExportOption:" + key);
					}
				}
			} catch (JSONException e) {
				throw new EngineException("Export unable to parse JSON exportOptions: " + exportOptionsTxt);
			}
			
		}
		
		// build a new CAR file from project directory
		f = CarUtils.makeArchive(projectName, exportOptions);
	
		// upload CAR file to admin
		HeaderName.ContentLength.setHeader(response, "" + f.length());
		if (f.exists()) {
			if (lastDate > 0) {
				f.setLastModified(lastDate);
			}
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			OutputStream outStream = response.getOutputStream();
			
			byte[] buffer = new byte[1024];
			int nbReadBytes;	
			
			while ((nbReadBytes = bis.read(buffer, 0, 1024)) > 0) {
				outStream.write(buffer, 0, nbReadBytes);
			}			
			bis.close();
		}
		
		Engine.logAdmin.info("The project '" + projectName + "' has been exported");
	}

}
