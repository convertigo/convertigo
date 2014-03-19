/*
 * Copyright (c) 2001-2014 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Export", 
		roles = { Role.WEB_ADMIN }, 
		parameters = {}, 
		returnValue = "return the global_symbols.properties file"
	)

public class Export extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		response.setHeader("Content-Disposition", "attachment; filename=\"global_symbols.properties\"");
		response.setContentType("text/plain");	   
		
		String globalSymbolsFilePath = Engine.theApp.databaseObjectsManager.getGlobalSymbolsFilePath();
		File f = new File(globalSymbolsFilePath);

		response.setHeader("Content-Length", "" + f.length());
		if (f.exists()) {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			OutputStream outStream = response.getOutputStream();
			
			byte[] buffer = new byte[1024];
			int nbReadBytes;	
			
			while ((nbReadBytes = bis.read(buffer, 0, 1024)) > 0) {
				outStream.write(buffer, 0, nbReadBytes);
			}			
			bis.close();
		} else {
			throw new IllegalArgumentException("The global symbols file doesn't exist!");			
		}

		String message = "The global symbols file has been exported.";
		Engine.logAdmin.info(message);
	}
}
