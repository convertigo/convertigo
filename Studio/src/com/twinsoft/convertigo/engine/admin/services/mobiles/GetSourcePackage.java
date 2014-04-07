package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.File;
import java.io.FileInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper.Keys;

@ServiceDefinition(
		name = "GetSourcePackage",
		roles = { Role.ANONYMOUS },
		parameters = {},
		returnValue = ""
)
public class GetSourcePackage extends DownloadService {	
	
	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws  Exception {
		String project = Keys.project.value(request);
		String platform = Keys.platform.value(request);
		
		MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(request, "mobile/www");
		File mobileArchiveFile = mobileResourceHelper.makeZipPackage();
				
		FileInputStream archiveInputStream = new FileInputStream(mobileArchiveFile);		
		
		response.setHeader("Content-Disposition", "attachment; filename=\"" + project + "_" + platform + "_SourcePackage.zip\"");
		response.setContentType("application/octet-stream");
		
		IOUtils.copy(archiveInputStream, response.getOutputStream());		
		
		archiveInputStream.close();
	}	

}
