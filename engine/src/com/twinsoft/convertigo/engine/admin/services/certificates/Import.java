/*
 * Copyright (c) 2001-2026 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.certificates;

import java.io.File;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.UploadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;

@ServiceDefinition(
	name = "Import",
	roles = { Role.WEB_ADMIN, Role.CERTIFICATE_CONFIG },
	parameters = {},
	returnValue = ""
)
public class Import extends UploadService {

	@Override
	protected void doUpload(HttpServletRequest request, Document document, FileItem item) throws Exception {
		String fileName = item.getName();
		if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
			throw new ServiceException("The certificate import requires a .zip file");
		}

		String actionImport = request.getParameter("action-import");
		if (actionImport == null || "on".equals(actionImport)) {
			actionImport = request.getParameter("priority");
		}
		if (actionImport == null) {
			actionImport = "priority-import";
		}
		if (!"clear-import".equals(actionImport) && !"priority-server".equals(actionImport)
				&& !"priority-import".equals(actionImport)) {
			throw new ServiceException("Invalid certificate import policy");
		}

		try (CertificateArchive.Archive archive = CertificateArchive.read(item.getInputStream(), item.getSize())) {
			File backup = CertificateArchive.install(archive, "clear-import".equals(actionImport),
					"priority-server".equals(actionImport));
			String message = "The certificate configuration has been successfully imported. Previous configuration saved as "
					+ backup.getName() + ".";
			Engine.logAdmin.info(message);
			ServiceUtils.addMessage(document, document.getDocumentElement(), message, "message", false);
		}
	}

	@Override
	protected String getRepository() {
		return Engine.CERTIFICATES_PATH;
	}
}
