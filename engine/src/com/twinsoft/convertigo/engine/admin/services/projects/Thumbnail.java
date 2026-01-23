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

package com.twinsoft.convertigo.engine.admin.services.projects;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.MimeType;

@ServiceDefinition(
		name = "Thumbnail",
		roles = { Role.TEST_PLATFORM, Role.PROJECTS_CONFIG, Role.PROJECTS_VIEW },
		parameters = {},
		returnValue = ""
		)
public class Thumbnail extends DownloadService {
	static byte[] imageBytes;
	static {
		var img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		var baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, "png", baos);
		} catch (IOException e) {
		}
		imageBytes = baos.toByteArray();
	}
	

	@Override
	protected void writeResponseResult(HttpServletRequest request,HttpServletResponse response) throws IOException, EngineException {
		var projectName = request.getParameter("projectName");
		var dir = Engine.projectDir(projectName);
		var file = new File(dir, "thumbnail.png");
		if (!file.exists()) {
			file = new File(dir, "thumbnail.jpg");
		}
		if (!file.exists()) {
			file = new File(dir, "thumbnail.auto.jpg");
		}
		response.setContentType(file.getName().endsWith("png") ? MimeType.Png.value() : MimeType.Jpeg.value());
		if (!file.exists()) {
			response.setContentLength(imageBytes.length);
			response.getOutputStream().write(imageBytes);
		} else {
			response.setContentLength((int) file.length());
			try (var fis = new FileInputStream(file)) {
				IOUtils.copyLarge(fis, response.getOutputStream());
			}
		}
		response.flushBuffer();
	}
	
	@Override
	public boolean isXsrfCheck() {
		return false;
	}
}
