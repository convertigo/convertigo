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

package com.twinsoft.convertigo.engine.admin.services.studio.source;

import java.io.File;

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
import com.twinsoft.convertigo.engine.util.JakartaServletFileUploadSupport;

/**
 * Manages the files of a project, as the file tree of the Eclipse Studio: creates a file or a folder,
 * uploads files, renames or deletes one, always inside the folder of the project.
 * <ul>
 * <li>action: newFile, newFolder, upload (the files are the parts of a multipart request), rename, delete</li>
 * <li>id: the file or the folder, as the tree names it, "Project/path"</li>
 * <li>name: the name of the new or renamed file</li>
 * </ul>
 */
@ServiceDefinition(name = "Files", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG }, parameters = {}, returnValue = "")
public class Files extends JSonService {
	/** the size an upload can have */
	private static final long MAX_UPLOAD = 64L * 1024 * 1024;

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var id = request.getParameter("id");
		var action = String.valueOf(request.getParameter("action"));
		if (!ProjectFiles.isFileId(id)) {
			throw new ServiceException("The id " + id + " is not a project file.");
		}
		var split = id.split("/", 2);
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(split[0]);
		if (project == null) {
			throw new ServiceException("The project " + split[0] + " does not exist.");
		}
		var projectDir = project.getDirFile().getCanonicalFile();
		// the root of the files is the folder of the project
		var target = split[1].replace("/", "").isEmpty() ? projectDir : ProjectFiles.resolve(id);
		switch (action) {
		case "newFile", "newFolder" -> {
			if (!target.isDirectory()) {
				throw new ServiceException("The folder " + id + " does not exist.");
			}
			var file = child(target, request.getParameter("name"));
			if (file.exists()) {
				throw new ServiceException("The project already has " + file.getName() + ".");
			}
			if ("newFolder".equals(action)) {
				file.mkdirs();
			} else {
				file.createNewFile();
			}
			response.put("id", idOf(id, projectDir, file));
		}
		case "upload" -> {
			if (!target.isDirectory()) {
				throw new ServiceException("The folder " + id + " does not exist.");
			}
			if (!JakartaServletFileUploadSupport.isMultipartContent(request)) {
				throw new ServiceException("The upload has no file.");
			}
			var upload = new ServletFileUpload(new DiskFileItemFactory());
			upload.setFileSizeMax(MAX_UPLOAD);
			String last = null;
			for (var item : JakartaServletFileUploadSupport.parseRequest(upload, request)) {
				if (item.isFormField()) {
					continue;
				}
				// the browsers of Windows can give the full path of the file
				var file = child(target, new File(item.getName().replace('\\', '/')).getName());
				item.write(file);
				last = idOf(id, projectDir, file);
			}
			if (last == null) {
				throw new ServiceException("The upload has no file.");
			}
			response.put("id", last);
		}
		case "rename" -> {
			if (target.equals(projectDir) || !target.exists()) {
				throw new ServiceException("The file " + id + " cannot be renamed.");
			}
			var renamed = child(target.getParentFile(), request.getParameter("name"));
			if (renamed.exists()) {
				throw new ServiceException("The project already has " + renamed.getName() + ".");
			}
			if (!target.renameTo(renamed)) {
				throw new ServiceException("The file " + id + " cannot be renamed.");
			}
			response.put("id", idOf(id, projectDir, renamed));
		}
		case "delete" -> {
			if (target.equals(projectDir) || !target.exists()) {
				throw new ServiceException("The file " + id + " cannot be deleted.");
			}
			FileUtils.forceDelete(target);
		}
		default -> throw new ServiceException("Unknown action " + action);
		}
		response.put("done", true);
	}

	/**
	 * @return the file of the name in the folder, a name without path
	 */
	private static File child(File folder, String name) throws ServiceException {
		if (name == null || name.isBlank() || name.contains("/") || name.contains("\\") || name.equals(".")
				|| name.equals("..")) {
			throw new ServiceException("The name " + name + " is not a file name.");
		}
		return new File(folder, name.trim());
	}

	/**
	 * @return the tree id of a file of the project
	 */
	private static String idOf(String id, File projectDir, File file) throws Exception {
		var project = id.split("/", 2)[0];
		var path = projectDir.getCanonicalFile().toPath().relativize(file.getCanonicalFile().toPath()).toString()
				.replace('\\', '/');
		return project + "//" + path;
	}
}
