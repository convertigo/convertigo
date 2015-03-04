package com.twinsoft.convertigo.engine.admin.services.store;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.util.ZipUtils;

@ServiceDefinition(
		name = "DownloadStoreFolder",
		roles = { Role.WEB_ADMIN },
		parameters = {
		@ServiceParameterDefinition(
				name = "css",
				description = "include css: on"
			),
		@ServiceParameterDefinition(
				name = "fonts",
				description = "include fonts: on"
			),
		@ServiceParameterDefinition(
				name = "i18n",
				description = "include i18n: on"
			),
		@ServiceParameterDefinition(
				name = "images",
				description = "include images: on"
			),
		@ServiceParameterDefinition(
				name = "scripts",
				description = "include scripts: on"
			),
		@ServiceParameterDefinition(
				name = "index",
				description = "include index.html: on"
			)
		},
		returnValue = ""
	)
public class DownloadStoreFolder extends DownloadService {
	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		List<String> parameters = getParameters();
		String storePath = Engine.WEBAPP_PATH + "/WEB-INF/store/";
		List<File> excludedFiles = getExcludedFiles(request, storePath, parameters);
		
		// Check if at least one file is included
		if (excludedFiles.size() < parameters.size()) {
			File storeArchiveFile = File.createTempFile("store", ".zip");
			ZipUtils.makeZip(storeArchiveFile.getPath(), storePath, null, excludedFiles);
	
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
	
	private List<String> getParameters() {
		return Arrays.asList("css", "fonts", "i18n", "images", "scripts", "index");
	}
	
	private List<File> getExcludedFiles(HttpServletRequest request, String storePath, List<String> parameters) {
		List<File> excludedFiles = new ArrayList<File>(parameters.size());
		for (String parameter : parameters) {
			String value = request.getParameter(parameter);
			if (value == null || !value.equals("on")) {
				addExcludedFile(excludedFiles, storePath, parameter);
			}
		}
		
		return excludedFiles;
	}
	
	private void addExcludedFile(List<File> excludedFiles, String storePath, String parameter) {
		excludedFiles.add(new File(storePath, parameter.equals("index") ? parameter + ".html" : parameter));
	}
}
