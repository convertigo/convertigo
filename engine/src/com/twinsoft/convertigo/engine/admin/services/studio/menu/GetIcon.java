package com.twinsoft.convertigo.engine.admin.services.studio.menu;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "GetIcon",
		roles = { Role.WEB_ADMIN, Role.PROJECT_DBO_CONFIG, Role.PROJECT_DBO_VIEW },
		parameters = {},
		returnValue = ""
	)
public class GetIcon extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String iconPath = request.getParameter("iconPath");
	    InputStream imageIs = new BufferedInputStream(new FileInputStream(Get.rootPath + iconPath));
		IOUtils.copy(imageIs, response.getOutputStream());
	}
}
