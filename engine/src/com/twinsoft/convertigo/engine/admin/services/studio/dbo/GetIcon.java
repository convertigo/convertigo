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

package com.twinsoft.convertigo.engine.admin.services.studio.dbo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.Version;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.HeaderName;

@ServiceDefinition(name = "GetIcon", roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG,
		Role.PROJECT_DBO_VIEW }, parameters = {}, returnValue = "")
public class GetIcon extends DownloadService {
	static final Pattern pIsImage = Pattern.compile("(?:(.*)_32x32\\.png|\\.(ico|gif|png|jpe?g|svg))$");

	@Override
	public boolean isNoCache() {
		return false;
	}

	@Override
	public boolean isXsrfCheck() {
		return false;
	}

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServiceException {
		if (Version.fullProductVersionID.equals(HeaderName.IfNoneMatch.getHeader(request))) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}
		var iconPath = request.getParameter("iconPath");

		if (iconPath == null) {
			throw new ServiceException("Missing iconPath");
		}

		HeaderName.CacheControl.setHeader(response, "public, max-age=300");
		HeaderName.ETag.setHeader(response, Version.fullProductVersionID);

		var isImage = pIsImage.matcher(iconPath);
		if (!isImage.find()) {
			throw new ServiceException("No image requested");
		}
		if (isImage.group(1) != null) {
			HeaderName.ContentType.setHeader(response, "image/svg+xml");
			try {
				var svgPath = isImage.group(1).replace("_color", "") + "_web.svg";
				IOUtils.copy(GetIcon.class.getResourceAsStream(svgPath), response.getOutputStream());
				Engine.logAdmin.info("The image has been exported. From class " + svgPath);
				return;
			} catch (Exception e) {
			}
			try {
				var svgPath = isImage.group(1) + ".svg";
				IOUtils.copy(GetIcon.class.getResourceAsStream(svgPath), response.getOutputStream());
				Engine.logAdmin.info("The image has been exported. From class " + svgPath);
				return;
			} catch (Exception e) {
			}
		}
		try {
			var type = isImage.group(1) != null ? "png" : isImage.group(2);
			if ("svg".equals(type)) {
				type = "svg+xml";
			}
			HeaderName.ContentType.setHeader(response, "image/" + type);
			var iconStream = openIconStream(iconPath);
			if (iconStream == null) {
				throw new IOException("Icon stream is null");
			}
			try (var is = iconStream) {
				IOUtils.copy(iconStream, response.getOutputStream());
			}
			Engine.logAdmin.info("The image has been exported. From iconPath " + iconPath);
			return;
		} catch (Exception e) {
		}
		throw new ServiceException("Icon unreachable: " + iconPath);
	}

	private InputStream openIconStream(String iconPath) throws Exception {
		if (iconPath.startsWith("projects:")) {
			return new FileInputStream(Engine.PROJECTS_PATH + iconPath.substring("projects:".length()));
		}
		if (iconPath.startsWith("workspace:")) {
			return new FileInputStream(Engine.USER_WORKSPACE_PATH + iconPath.substring("workspace:".length()));
		}
		var iconFile = new File(iconPath);
		if (iconFile.isAbsolute() && isInLoadedProject(iconFile)) {
			return new FileInputStream(iconFile);
		}
		return GetIcon.class.getResourceAsStream(iconPath);
	}

	private boolean isInLoadedProject(File iconFile) throws Exception {
		var iconPath = iconFile.getCanonicalPath();
		for (var projectName: Engine.theApp.databaseObjectsManager.getAllProjectNamesList(true)) {
			var dbo = Engine.theApp.databaseObjectsManager.getDatabaseObjectByQName(projectName);
			if (dbo instanceof Project project) {
				var projectPath = new File(project.getDirPath()).getCanonicalPath();
				if (iconPath.equals(projectPath) || iconPath.startsWith(projectPath + File.separator)) {
					return true;
				}
			}
		}
		return false;
	}
}
