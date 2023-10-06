/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

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
	static final Pattern pIsImage = Pattern.compile("(?:(.*)_32x32\\.png|\\.(ico|gif|jpe?g|svg))$");

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
		String iconPath = request.getParameter("iconPath");

		if (iconPath == null) {
			throw new ServiceException("Missing iconPath");
		}

		HeaderName.CacheControl.setHeader(response, "public, max-age=300");
		HeaderName.ETag.setHeader(response, Version.fullProductVersionID);

		Matcher isImage = pIsImage.matcher(iconPath);
		if (!isImage.find()) {
			throw new ServiceException("No image requested");
		}
		if (isImage.group(1) != null) {
			try {
				HeaderName.ContentType.setHeader(response, "image/svg+xml");
				String svgPath = isImage.group(1) + ".svg";
				IOUtils.copy(GetIcon.class.getResourceAsStream(svgPath), response.getOutputStream());
				Engine.logAdmin.info("The image has been exported. From class " + svgPath);
				return;
			} catch (Exception e) {
			}
		}
		try {
			String type = isImage.group(1) != null ? "png" : isImage.group(2);
			if ("svg".equals(type)) {
				type = "svg+xml";
			}
			HeaderName.ContentType.setHeader(response, "image/" + type);
			IOUtils.copy(GetIcon.class.getResourceAsStream(iconPath), response.getOutputStream());
			Engine.logAdmin.info("The image has been exported. From iconPath " + iconPath);
			return;
		} catch (Exception e) {
		}
		throw new ServiceException("Icon unreachable: " + iconPath);
	}
}