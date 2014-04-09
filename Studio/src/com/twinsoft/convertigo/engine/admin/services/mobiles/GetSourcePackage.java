package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.File;
import java.io.FileInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticationException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper.Keys;
import com.twinsoft.convertigo.engine.enums.Accessibility;

@ServiceDefinition(
		name = "GetSourcePackage",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = ""
)
public class GetSourcePackage extends DownloadService {	
	
	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws  Exception {
		MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(request, "mobile/www");
		
		if (mobileResourceHelper.mobileApplication == null) {
			throw new ServiceException("no such mobile application");
		} else {
			boolean bAdminRole = Engine.authenticatedSessionManager.hasRole(request.getSession(), Role.WEB_ADMIN);
			if (!bAdminRole && mobileResourceHelper.mobileApplication.getAccessibilityEnum() == Accessibility.Private) {
				throw new AuthenticationException("Authentication failure: user has not sufficient rights!");
			}
		}
		
		String project = Keys.project.value(request);
		String platform = Keys.platform.value(request);
		
		File mobileArchiveFile = mobileResourceHelper.makeZipPackage();
				
		FileInputStream archiveInputStream = new FileInputStream(mobileArchiveFile);		
		
		response.setHeader("Content-Disposition", "attachment; filename=\"" + project + "_" + platform + "_SourcePackage.zip\"");
		response.setContentType("application/octet-stream");
		
		IOUtils.copy(archiveInputStream, response.getOutputStream());		
		
		archiveInputStream.close();
	}	

}
