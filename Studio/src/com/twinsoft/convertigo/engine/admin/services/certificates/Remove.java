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

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;

@ServiceDefinition(
		name = "Remove",
		roles = { Role.WEB_ADMIN, Role.CERTIFICATE_CONFIG },
		parameters = {
				@ServiceParameterDefinition(
						name = "certificateName",
						description = "the name of the certificate to remove from the certificate directory"
					)	
		},
		returnValue = ""
	)
public class Remove extends XmlService {
	
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String certificateName = request.getParameter("certificateName");		
		File toRemove=new File(Engine.CERTIFICATES_PATH+"/"+certificateName);
		toRemove.delete();
	}
		
}	