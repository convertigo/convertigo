/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.File;
import java.io.FileInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticationException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;

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
			boolean bTpPrivateRole = Engine.authenticatedSessionManager.hasRole(request.getSession(), Role.TEST_PLATFORM_PRIVATE);
			if (!bTpPrivateRole && mobileResourceHelper.mobileApplication.getAccessibility() == Accessibility.Private) {
				throw new AuthenticationException("Authentication failure: user has not sufficient rights!");
			}
		}
		
		File mobileArchiveFile = mobileResourceHelper.makeZipPackage();
		
		try (FileInputStream archiveInputStream = new FileInputStream(mobileArchiveFile)) {
			HeaderName.ContentDisposition.setHeader(response, "attachment; filename=\"" + mobileArchiveFile.getName() + "\"");
			HeaderName.ContentLength.setHeader(response, Long.toString(mobileArchiveFile.length()));
			response.setContentType(MimeType.OctetStream.value());
			
			IOUtils.copy(archiveInputStream, response.getOutputStream());
		}
	}	

}
