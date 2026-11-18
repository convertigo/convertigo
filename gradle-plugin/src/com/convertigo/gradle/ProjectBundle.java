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

package com.convertigo.gradle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;

/** Produces the same bundle format as the administration console. */
public class ProjectBundle extends ProjectCar {
	private List<String> projects = new ArrayList<>();
	private boolean includeDependencies;
	private List<String> includeProjects = new ArrayList<>();
	private List<String> excludeProjects = new ArrayList<>();

	@Input
	public List<String> getProjects() {
		return projects;
	}

	public void setProjects(List<String> projects) {
		this.projects = new ArrayList<>(projects);
	}

	@Input
	public boolean isIncludeDependencies() {
		return includeDependencies;
	}

	public void setIncludeDependencies(boolean includeDependencies) {
		this.includeDependencies = includeDependencies;
	}

	@Input
	public List<String> getIncludeProjects() { return includeProjects; }

	public void setIncludeProjects(List<String> includeProjects) { this.includeProjects = new ArrayList<>(includeProjects); }

	@Input
	public List<String> getExcludeProjects() { return excludeProjects; }

	public void setExcludeProjects(List<String> excludeProjects) { this.excludeProjects = new ArrayList<>(excludeProjects); }

	@TaskAction
	@Override
	void taskAction() throws Exception {
		var root = plugin.load.getConvertigoProject();
		var names = projects.isEmpty() ? List.of(root.getName()) : List.copyOf(projects);
		var options = new HashSet<>(ArchiveExportOption.all);
		if (!isIncludeTestCases()) options.remove(ArchiveExportOption.includeTestCase);
		if (!isIncludeStubs()) options.remove(ArchiveExportOption.includeStubs);
		if (!isIncludeMobileApp()) options.remove(ArchiveExportOption.includeMobileApp);
		if (!isIncludeMobileAppAssets()) options.remove(ArchiveExportOption.includeMobileAppAssets);
		if (!isIncludeMobileDataset()) options.remove(ArchiveExportOption.includeMobileDataset);
		if (!isIncludeMobilePlatformsAssets()) options.remove(ArchiveExportOption.includeMobilePlatformsAssets);
		File output = getDestinationFile();
		if (output == null) output = new File(getDestinationDir(), root.getName() + "-bundle.zip");
		com.twinsoft.convertigo.engine.util.ProjectBundle.export(output.toPath(), names, includeDependencies, includeProjects, excludeProjects,
				options, name -> name.equals(root.getName()) ? root
						: com.twinsoft.convertigo.engine.Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, false));
		setDestinationFile(output);
	}
}
