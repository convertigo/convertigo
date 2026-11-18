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
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.ProjectBundleDeployment;
import com.twinsoft.convertigo.engine.util.ProjectBundleImport;
import com.twinsoft.convertigo.engine.util.ProjectBundleZip.Limits;

@ServiceDefinition(name = "DeployBundle", roles = { Role.WEB_ADMIN, Role.TRIAL, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class DeployBundle extends XmlService {
	protected Limits limits() { return Limits.defaults(); }

	protected Path createTemporaryDirectory() throws IOException {
		return Files.createTempDirectory("convertigo-project-upload-");
	}

	protected ProjectBundleDeployment.Completion install(HttpServletRequest request, String name, Path archive, boolean assembleXsl) throws Exception {
		var deployment = Engine.theApp.databaseObjectsManager.installPreparedProjectArchive(archive.toString(), name);
		if (deployment == null) throw new IOException("The project could not be installed");
		return () -> {
			try {
				var project = deployment.load();
				if (project == null || !name.equals(project.getName())) throw new IOException("The loaded project differs from its descriptor");
				deployment.finish(assembleXsl);
				Deploy.completeDeployment(request, project);
				return project.migrationSaveError;
			} catch (Exception failure) {
				Engine.logAdmin.warn("Project bundle deployment stopped at " + name, failure);
				throw failure;
			}
		};
	}

	protected AutoCloseable openGroup(com.twinsoft.convertigo.engine.util.ProjectBundlePlan.Group group) {
		if (!group.cyclic()) return () -> {};
		return Engine.theApp.databaseObjectsManager.openProjectImportGroup(
				group.projects().stream().map(com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo::name)
						.collect(java.util.stream.Collectors.toSet()));
	}

	protected boolean projectAvailable(String name) {
		return Engine.theApp.databaseObjectsManager.existsProject(name);
	}

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		if (!ServletFileUpload.isMultipartContent(request)) throw new IOException("A multipart project upload is required");
		var limits = limits();
		var temporary = createTemporaryDirectory();
		List<FileItem> items = List.of();
		try {
			var factory = new DiskFileItemFactory(16 * 1024, temporary.toFile());
			var upload = new ServletFileUpload(factory);
			upload.setSizeMax(limits.uploadBytes());
			upload.setFileSizeMax(limits.uploadBytes());
			upload.setFileCountMax(limits.uploads() + 8L);
			items = upload.parseRequest(request);
			var files = new ArrayList<Path>();
			boolean assemble = Boolean.parseBoolean(request.getParameter("bAssembleXsl"));
			for (var item : items) {
				if (item.isFormField()) {
					if (item.getSize() > 4096) throw new IOException("Project upload options are too large");
					if (item.getFieldName().equals("bAssembleXsl")) assemble = Boolean.parseBoolean(item.getString("UTF-8"));
				} else {
					String name = item.getName();
					if (name == null || !name.toLowerCase(Locale.ROOT).matches(".+\\.(car|zip)")) throw new IOException("Choose .car or .zip project archives");
					if (files.size() >= limits.uploads()) throw new IOException("Too many project archives");
					var target = temporary.resolve("upload-" + files.size());
					try (var input = item.getInputStream()) { Files.copy(input, target); }
					files.add(target);
				}
			}
			boolean assembleXsl = assemble;
			try (var prepared = ProjectBundleImport.prepare(files, temporary, limits)) {
				prepared.checkDependencies(this::projectAvailable);
				for (var warning : prepared.warnings()) ServiceUtils.addMessage(document, document.getDocumentElement(), warning, "warning", false);
				var results = ProjectBundleDeployment.apply(prepared, new ProjectBundleDeployment.Installer() {
					@Override public ProjectBundleDeployment.Completion install(String name, Path archive) throws Exception {
						return DeployBundle.this.install(request, name, archive, assembleXsl);
					}
					@Override public AutoCloseable openGroup(com.twinsoft.convertigo.engine.util.ProjectBundlePlan.Group group) {
						return DeployBundle.this.openGroup(group);
					}
				});
				var summary = document.createElement("deployment");
				boolean success = results.stream().allMatch(result -> result.status() == ProjectBundleDeployment.Status.DEPLOYED);
				summary.setAttribute("success", Boolean.toString(success));
				document.getDocumentElement().appendChild(summary);
				for (var result : results) {
					var project = document.createElement("project");
					project.setAttribute("name", result.name());
					project.setAttribute("version", result.version());
					project.setAttribute("status", result.status().name().toLowerCase(Locale.ROOT));
					if (!result.warning().isEmpty()) project.setAttribute("warning", result.warning());
					if (!result.error().isEmpty()) project.setAttribute("detail", result.error());
					summary.appendChild(project);
				}
			}
		} finally {
			try {
				for (var item : items) item.delete();
			} finally {
				org.apache.commons.io.FileUtils.deleteDirectory(temporary.toFile());
			}
		}
	}
}
