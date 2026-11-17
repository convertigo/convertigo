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
import java.util.ArrayList;
import java.util.List;

/** Sequential application of an already validated bundle, stopping at the first failure. */
public final class ProjectBundleDeployment {
	public enum Status { DEPLOYED, FAILED, NOT_ATTEMPTED }
	public record Result(String name, String version, Status status, String warning, String error) {}

	@FunctionalInterface
	public interface Deployer {
		/** Uses the normal project backup/replacement operation and returns any migration-save warning. */
		String deploy(String name, Path archive) throws Exception;
	}

	private ProjectBundleDeployment() {
	}

	public static List<Result> apply(ProjectBundleImport prepared, Deployer deployer) {
		var results = new ArrayList<Result>();
		boolean stopped = false;
		for (var project : prepared.plan().projects()) {
			if (stopped) {
				results.add(new Result(project.name(), project.version(), Status.NOT_ATTEMPTED, "", ""));
				continue;
			}
			try {
				String warning = deployer.deploy(project.name(), prepared.archive(project.name()));
				results.add(new Result(project.name(), project.version(), Status.DEPLOYED, warning == null ? "" : warning, ""));
			} catch (Exception failure) {
				stopped = true;
				String message = failure.getMessage();
				results.add(new Result(project.name(), project.version(), Status.FAILED, "",
						message == null || message.isBlank() ? "Project deployment failed" : message));
			}
		}
		return List.copyOf(results);
	}
}
