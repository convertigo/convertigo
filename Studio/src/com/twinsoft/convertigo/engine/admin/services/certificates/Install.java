/*
 * Copyright (c) 2001-2011 Convertigo SA.
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
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.admin.services.certificates;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.UploadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Install",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class Install extends UploadService {

	@Override
	protected String getRepository() {
		return Engine.CERTIFICATES_PATH;		
	}	
	
	@Override
	protected void doUpload(HttpServletRequest request, Document document, FileItem item) throws Exception {		
		File fullFile = new File(item.getName());
		String certifNameExtension=fullFile.getName().replaceFirst(".*\\.", ".");
		if(CertificateManager.isCertificateExtension(certifNameExtension)){
			File savedFile = new File(getRepository(), fullFile.getName());
			item.write(savedFile);			
			ServiceUtils.addMessage(document, document.getDocumentElement(), "The certificate \""+fullFile.getName()+"\" has been successfully uploaded", "message", false);
		}
		else{
			ServiceUtils.addMessage(document, document.getDocumentElement(), "The extension \""+certifNameExtension+"\" isn't valid", "error", false);
		}
	}
	
}
