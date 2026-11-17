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

	@Input
	public List<String> getProjects() {
		return projects;
	}

	public void setProjects(List<String> projects) {
		this.projects = new ArrayList<>(projects);
	}

	@Input
	public boolean getIncludeDependencies() {
		return includeDependencies;
	}

	public void setIncludeDependencies(boolean includeDependencies) {
		this.includeDependencies = includeDependencies;
	}

	@TaskAction
	@Override
	void taskAction() throws Exception {
		var root = plugin.load.getConvertigoProject();
		var names = projects.isEmpty() ? List.of(root.getName()) : List.copyOf(projects);
		var options = new HashSet<>(ArchiveExportOption.all);
		if (!getIncludeTestCases()) options.remove(ArchiveExportOption.includeTestCase);
		if (!getIncludeStubs()) options.remove(ArchiveExportOption.includeStubs);
		if (!getIncludeMobileApp()) options.remove(ArchiveExportOption.includeMobileApp);
		if (!getIncludeMobileAppAssets()) options.remove(ArchiveExportOption.includeMobileAppAssets);
		if (!getIncludeMobileDataset()) options.remove(ArchiveExportOption.includeMobileDataset);
		if (!getIncludeMobilePlatformsAssets()) options.remove(ArchiveExportOption.includeMobilePlatformsAssets);
		File output = getDestinationFile();
		if (output == null) output = new File(getDestinationDir(), root.getName() + "-bundle.zip");
		com.twinsoft.convertigo.engine.util.ProjectBundle.export(output.toPath(), names, includeDependencies,
				options, name -> name.equals(root.getName()) ? root
						: com.twinsoft.convertigo.engine.Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, false));
		setDestinationFile(output);
	}
}
