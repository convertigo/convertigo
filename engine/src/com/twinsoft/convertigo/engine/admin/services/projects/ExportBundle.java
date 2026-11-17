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

package com.twinsoft.convertigo.engine.admin.services.projects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.DownloadService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;
import com.twinsoft.convertigo.engine.util.ProjectBundle;

@ServiceDefinition(name = "ExportBundle", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class ExportBundle extends DownloadService {
	protected void export(Path destination, java.util.List<String> names, boolean dependencies,
			java.util.Set<ArchiveExportOption> options) throws Exception {
		ProjectBundle.export(destination, names, dependencies, options);
	}

	@Override
	protected void writeResponseResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String selection = request.getParameter("projects");
		if (selection == null || selection.length() > 64 * 1024) throw new IOException("Choose projects to export");
		var json = new JSONArray(selection);
		if (json.length() == 0 || json.length() > 200) throw new IOException("Choose between 1 and 200 projects");
		var names = new ArrayList<String>();
		for (int i = 0; i < json.length(); i++) names.add(json.getString(i));
		var options = new HashSet<>(ArchiveExportOption.all);
		String configured = request.getParameter("exportOptions");
		if (configured != null) {
			if (configured.length() > 4096) throw new IOException("Project export options are too large");
			var values = new JSONObject(configured);
			for (var option : ArchiveExportOption.values()) if (!values.optBoolean(option.name(), true)) options.remove(option);
		}
		var temporary = Files.createTempDirectory("convertigo-project-export-");
		try {
			var output = temporary.resolve("projects-bundle.zip");
			export(output, names, Boolean.parseBoolean(request.getParameter("includeDependencies")), options);
			response.setContentType(MimeType.Zip.value());
			HeaderName.ContentDisposition.setHeader(response, "attachment; filename=\"projects-bundle.zip\"");
			HeaderName.ContentLength.setHeader(response, Long.toString(Files.size(output)));
			Files.copy(output, response.getOutputStream());
		} finally {
			org.apache.commons.io.FileUtils.deleteDirectory(temporary.toFile());
		}
	}
}
