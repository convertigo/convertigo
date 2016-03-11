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

package com.twinsoft.convertigo.engine.admin.services.certificates.mappings;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.util.FileAndProperties;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
		name = "Delete",
		roles = { Role.WEB_ADMIN, Role.CERTIFICATE_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Delete extends XmlService {
	
	Document document;	

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();

		FileAndProperties rep=ServiceUtils.startCertificate();
		File file=rep.getF();
		Properties storesProperties=rep.getP();

        //String message="";
		String link;

		int i = 1;
		while ( (link = (String)request.getParameter("link_"+i)) != null ) {
			ServiceUtils.deleteMapping(storesProperties, link, document, rootElement);
			i++;
		}
		FileOutputStream fos = new FileOutputStream(file);
		storesProperties.store(fos , "");
		fos.flush();
		fos.close();			
	}
}	