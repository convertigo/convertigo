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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;

@ServiceDefinition(
	name = "Export",
	roles = { Role.WEB_ADMIN, Role.CERTIFICATE_CONFIG },
	parameters = {},
	returnValue = "return the selected certificates, configuration and mappings as a ZIP archive"
)
public class Export extends DownloadService {

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String elements = request.getParameter("elements");
		if (elements == null) {
			elements = request.getParameter("certificates");
		}
		CertificateArchive.ExportSelection selected = CertificateArchive.parseSelection(elements);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		String fileName = "certificates_" + dateFormat.format(new Date()) + ".zip";

		synchronized (Engine.CERTIFICATES_PATH) {
			HeaderName.ContentDisposition.setHeader(response, "attachment; filename=\"" + fileName + "\"");
			HeaderName.CacheControl.setHeader(response, "no-store");
			response.setContentType(MimeType.Zip.value());
			CertificateArchive.write(selected, response.getOutputStream());
		}
		Engine.logAdmin.info("The certificate configuration has been exported.");
	}
}
