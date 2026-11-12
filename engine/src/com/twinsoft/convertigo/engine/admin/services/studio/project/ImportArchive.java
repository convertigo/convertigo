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

import java.io.File;
import java.nio.file.Files;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.sync.SharedWorkspaceSyncManager;
import com.twinsoft.convertigo.engine.util.JakartaServletFileUploadSupport;
import com.twinsoft.convertigo.engine.util.StringUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

/**
 * Imports a project archive (.car or .zip), as the Import wizard of the Eclipse Studio: under the name of its project,
 * or under another one, and never over a project of the workspace unless asked.
 * <ul>
 * <li>the archive: the file of a multipart request</li>
 * <li>projectName: the name of the imported project, the one of the archive when empty</li>
 * <li>overwrite: true replaces a project of the same name; else exists tells it, and nothing is imported</li>
 * </ul>
 */
@ServiceDefinition(name = "ImportArchive", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class ImportArchive extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		if (!JakartaServletFileUploadSupport.isMultipartContent(request)) {
			throw new ServiceException("The import has no archive.");
		}
		var dir = Files.createTempDirectory("convertigo-import-").toFile();
		try {
			File archive = null;
			for (var item : JakartaServletFileUploadSupport.parseRequest(new ServletFileUpload(new DiskFileItemFactory()), request)) {
				if (!item.isFormField()) {
					archive = new File(dir, "project.car");
					item.write(archive);
				}
			}
			if (archive == null) {
				throw new ServiceException("The import has no archive.");
			}
			String archiveName;
			try {
				archiveName = ZipUtils.getProjectName(archive.getPath());
			} catch (Exception e) {
				throw new ServiceException("The file is not a Convertigo project archive: " + e.getMessage());
			}
			var requested = request.getParameter("projectName");
			var target = requested == null || requested.isBlank() ? archiveName : requested.trim();
			if (!StringUtils.isNormalized(target)) {
				throw new ServiceException("The project name " + target
						+ " must be normalized: it starts with a letter and has no non ASCII character.");
			}
			response.put("projectName", target);
			var manager = Engine.theApp.databaseObjectsManager;
			if (!Boolean.parseBoolean(request.getParameter("overwrite"))
					&& (manager.existsProject(target) || new File(Engine.PROJECTS_PATH, target).exists())) {
				response.put("exists", true);
				return;
			}
			// another name gives the objects new priorities, as the Import wizard of the Eclipse Studio
			var project = manager.deployProject(archive.getPath(), target, true, false, !target.equals(archiveName));
			Engine.theApp.schemaManager.clearCache(project.getName());
			SharedWorkspaceSyncManager.markProjectReload(project.getName());
			response.put("done", true);
			response.put("project", project.getName());
		} finally {
			FileUtils.deleteQuietly(dir);
		}
	}
}
