package com.twinsoft.convertigo.engine.admin.services.store;

import java.io.File;
import java.io.FileInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.ZipUtils;

@ServiceDefinition(
		name = "DownloadStoreFolder",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class DownloadStoreFolder extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request,HttpServletResponse response) throws Exception {
		File storeArchiveFile = File.createTempFile("store", ".zip");
		ZipUtils.makeZip(storeArchiveFile.getPath(), Engine.WEBAPP_PATH + "/WEB-INF/store", null);
		
		FileInputStream archiveInputStream = null;
		try {
			archiveInputStream = new FileInputStream(storeArchiveFile);
			
			response.setHeader("Content-Disposition", "attachment; filename=" + "\"store.zip\"");
			response.setContentType("application/octet-stream");
			IOUtils.copy(archiveInputStream, response.getOutputStream());	
		}
		finally {
			storeArchiveFile.delete();
			archiveInputStream.close();
		}
	}
}
