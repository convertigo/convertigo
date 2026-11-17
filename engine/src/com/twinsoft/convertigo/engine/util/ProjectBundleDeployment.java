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

import java.nio.file.Path;
import java.util.List;

/** Sequential application of an already validated bundle, stopping at the first failure. */
public final class ProjectBundleDeployment {
	public enum Status { DEPLOYED, FAILED, INSTALLED, NOT_ATTEMPTED }
	public record Result(String name, String version, Status status, String warning, String error) {}

	@FunctionalInterface
	public interface Completion {
		/** Loads/finalizes one installed project and returns its migration-save warning. */
		String finish() throws Exception;
	}

	@FunctionalInterface
	public interface Installer {
		Completion install(String name, Path archive) throws Exception;
		default AutoCloseable openGroup(ProjectBundlePlan.Group group) throws Exception { return () -> {}; }
	}

	private ProjectBundleDeployment() {
	}

	public static List<Result> apply(ProjectBundleImport prepared, Installer installer) {
		var results = new java.util.LinkedHashMap<String, Result>();
		boolean stopped = false;
		for (var group : prepared.plan().groups()) {
			var installed = new java.util.LinkedHashMap<String, Completion>();
			if (!stopped) {
				String current = group.projects().get(0).name();
				try (var scope = installer.openGroup(group)) {
					for (var project : group.projects()) {
						current = project.name();
						var completion = installer.install(current, prepared.archive(current));
						if (completion == null) throw new java.io.IOException("The project installation returned no loading action");
						installed.put(current, completion);
					}
					for (var project : group.projects()) {
						current = project.name();
						String warning = installed.get(current).finish();
						results.put(current, new Result(current, project.version(), Status.DEPLOYED, warning == null ? "" : warning, ""));
					}
				} catch (Exception failure) {
					stopped = true;
					String message = failure.getMessage();
					var name = current;
					String version = group.projects().stream().filter(project -> project.name().equals(name)).findFirst().orElseThrow().version();
					results.put(current, new Result(current, version, Status.FAILED, "",
							message == null || message.isBlank() ? "Project deployment failed" : message));
				}
			}
			for (var project : group.projects()) if (!results.containsKey(project.name())) {
				boolean touched = installed.containsKey(project.name());
				results.put(project.name(), new Result(project.name(), project.version(), touched ? Status.INSTALLED : Status.NOT_ATTEMPTED,
						"", touched ? "Sources installed; loading and finalization were not completed" : ""));
			}
		}
		return prepared.plan().projects().stream().map(project -> results.get(project.name())).toList();
	}
}
