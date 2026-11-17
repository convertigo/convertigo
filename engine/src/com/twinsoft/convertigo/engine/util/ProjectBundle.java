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

package com.twinsoft.convertigo.engine.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.util.ProjectBundlePlan.ProjectInfo;

/** Shared flat bundle producer for the administration console and project build plugin. */
public final class ProjectBundle {
	// CarUtils may prepare a no-test-case descriptor in the source project.
	private static final Object EXPORT_LOCK = new Object();

	private ProjectBundle() {
	}

	@FunctionalInterface
	public interface ProjectResolver {
		Project resolve(String name) throws Exception;
	}

	public static ProjectInfo describe(Project project) throws Exception {
		var dependencies = new TreeSet<String>();
		for (var reference : project.getReferenceList()) {
			if (reference instanceof ProjectSchemaReference projectReference) {
				dependencies.add(projectReference.getParser().getProjectName());
			}
		}
		return new ProjectInfo(project.getName(), project.getVersion(), dependencies);
	}

	public static ProjectBundlePlan export(Path destination, Collection<String> selected, boolean includeDependencies,
			Set<ArchiveExportOption> options) throws Exception {
		return export(destination, selected, includeDependencies, options,
				name -> Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, false));
	}

	public static ProjectBundlePlan export(Path destination, Collection<String> selected, boolean includeDependencies,
			Set<ArchiveExportOption> options, ProjectResolver resolver) throws Exception {
		synchronized (EXPORT_LOCK) {
			var projects = new HashMap<String, Project>();
			var descriptions = new ArrayList<ProjectInfo>();
			var pending = new TreeSet<>(selected);
			if (pending.size() != selected.size()) throw new IOException("The project selection contains a duplicate name");
			while (!pending.isEmpty()) {
				var name = pending.pollFirst();
				ProjectBundlePlan.validateName(name);
				if (projects.containsKey(name)) continue;
				if (projects.size() >= 200) throw new IOException("A bundle may contain at most 200 projects");
				var project = resolver.resolve(name);
				if (project == null || !name.equals(project.getName())) {
					throw new IOException("The selected project is unavailable: " + name);
				}
				var description = describe(project);
				projects.put(name, project);
				descriptions.add(description);
				if (includeDependencies) pending.addAll(description.dependencies());
			}
			var plan = ProjectBundlePlan.create(descriptions);
			write(destination, plan, projects, Set.copyOf(options));
			return plan;
		}
	}

	private static void write(Path destination, ProjectBundlePlan plan, Map<String, Project> projects,
			Set<ArchiveExportOption> options) throws Exception {
		var target = destination.toAbsolutePath().normalize();
		Files.createDirectories(target.getParent());
		var realTarget = target.getParent().toRealPath().resolve(target.getFileName());
		for (var project : projects.values()) {
			var source = project.getDirFile().getCanonicalFile().toPath();
			if (realTarget.startsWith(source)) {
				String top = source.relativize(realTarget).getName(0).toString();
				if (!Set.of("build", "_private", "_data").contains(top)) {
					throw new IOException("Choose a bundle destination outside project sources or in their build directory");
				}
			}
		}
		var staging = Files.createTempDirectory("convertigo-project-bundle-");
		Path ready = null;
		try {
			for (var project : projects.values()) {
				if (staging.toRealPath().startsWith(project.getDirFile().getCanonicalFile().toPath())) {
					throw new IOException("The bundle temporary directory must be outside project sources");
				}
			}
			var manifest = new JSONObject();
			manifest.put("formatVersion", 1);
			var entries = new JSONArray();
			var order = new JSONArray();
			var output = staging.resolve("bundle.zip");
			try (var zip = new ZipOutputStream(Files.newOutputStream(output))) {
				for (var info : plan.projects()) {
					String archiveName = info.name() + ".car";
					var archive = staging.resolve(archiveName);
					CarUtils.makeArchive(archive.toFile(), projects.get(info.name()), options);
					zip.putNextEntry(new ZipEntry(archiveName));
					Files.copy(archive, zip);
					zip.closeEntry();
					entries.put(new JSONObject().put("name", info.name()).put("version", info.version())
							.put("archive", archiveName).put("dependencies", new JSONArray(info.dependencies())));
					order.put(info.name());
				}
				manifest.put("projects", entries);
				manifest.put("order", order);
				zip.putNextEntry(new ZipEntry("bundle.json"));
				zip.write(manifest.toString(2).getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
			try (var validation = ProjectBundleImport.prepare(java.util.List.of(output), staging, ProjectBundleZip.Limits.defaults())) {
				if (!validation.plan().projects().equals(plan.projects())) {
					throw new IOException("Save the selected project descriptors before creating a bundle");
				}
			}
			ready = Files.createTempFile(target.getParent(), ".bundle-output-", ".zip");
			Files.copy(output, ready, StandardCopyOption.REPLACE_EXISTING);
			try {
				Files.move(ready, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
				Files.move(ready, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			try {
				if (ready != null) Files.deleteIfExists(ready);
			} finally {
				org.apache.commons.io.FileUtils.deleteDirectory(staging.toFile());
			}
		}
	}
}
