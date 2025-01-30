/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.store;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.enums.StoreFiles;
import com.twinsoft.convertigo.engine.util.ZipUtils;

@ServiceDefinition(
		name = "DownloadStoreFolder",
		roles = { Role.WEB_ADMIN, Role.STORE_CONFIG, Role.STORE_VIEW },
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
		String storePath = Engine.WEBAPP_PATH + "/WEB-INF/" + StoreFiles.STORE_DIRECTORY_NAME + "/";
		Set<File> excludedFiles = getExcludedFiles(request, storePath);
		
		// Check if at least one file is included
		if (excludedFiles.size() < StoreFiles.size()) {
			File storeArchiveFile = File.createTempFile(StoreFiles.STORE_DIRECTORY_NAME, ".zip");
			ZipUtils.makeZip(storeArchiveFile.getPath(), storePath, null, excludedFiles);
	
			FileInputStream archiveInputStream = null;
			try {
				archiveInputStream = new FileInputStream(storeArchiveFile);
				
				HeaderName.ContentDisposition.setHeader(response, "attachment; filename=" + "\"" + StoreFiles.STORE_DIRECTORY_NAME + ".zip\"");
				response.setContentType(MimeType.OctetStream.value());
				IOUtils.copy(archiveInputStream, response.getOutputStream());	
			}
			finally {
				archiveInputStream.close();
				storeArchiveFile.delete();
			}
		}
	}
	
	private Set<File> getExcludedFiles(HttpServletRequest request, String storePath) {
		Set<File> excludedFiles = new HashSet<File>(StoreFiles.size());
		
		for (StoreFiles parameter : StoreFiles.values()) {
			String value = request.getParameter(parameter.name());
			if (value == null || !value.equals("on")) {
				addExcludedFile(excludedFiles, storePath, parameter);
			}
		}
		
		return excludedFiles;
	}
	
	private void addExcludedFile(Set<File> excludedFiles, String storePath, StoreFiles parameter) {
		excludedFiles.add(new File(storePath, parameter.filename()));
	}
}
