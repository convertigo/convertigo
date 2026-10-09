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

import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.engine.CLI;

public class ProjectCar extends ConvertigoTask {
	private File destinationDir;
	private File destinationFile;
	private boolean includeTestCases = true;
	private boolean includeStubs = true;
	private boolean includeMobileApp = true;
	private boolean includeMobileAppAssets = true;
	private boolean includeMobileDataset = true;
	private boolean includeMobilePlatformsAssets = true;
	
	@OutputDirectory @Optional
	public File getDestinationDir() {
		return destinationDir;
	}

	public void setDestinationDir(File destinationDir) {
		this.destinationDir = destinationDir;
	}
	
	public void setDestinationDir(String destinationDir) {
		this.destinationDir = getProject().file(destinationDir);
	}
	
	@OutputFile @Optional
	public File getDestinationFile() {
		return destinationFile;
	}

	public void setDestinationFile(File destinationFile) {
		this.destinationFile = destinationFile;
	}
	
	@Deprecated @Internal
	public Boolean isIncludeTestCases() {
		return getIncludeTestCases();
	}

	@Input @Optional
	public Boolean getIncludeTestCases() {
		return includeTestCases;
	}

	public void setIncludeTestCases(Boolean includeTestCases) {
		this.includeTestCases = includeTestCases;
	}
	
	@Deprecated @Internal
	public Boolean isIncludeStubs() {
		return getIncludeStubs();
	}

	@Input @Optional
	public Boolean getIncludeStubs() {
		return includeStubs;
	}

	public void setIncludeStubs(Boolean includeStubs) {
		this.includeStubs = includeStubs;
	}

	@Deprecated @Internal
	public Boolean isIncludeMobileApp() {
		return getIncludeMobileApp();
	}

	@Input @Optional
	public Boolean getIncludeMobileApp() {
		return includeMobileApp;
	}

	public void setIncludeMobileApp(Boolean includeMobileApp) {
		this.includeMobileApp = includeMobileApp;
	}

	@Deprecated @Internal
	public Boolean isIncludeMobileAppAssets() {
		return getIncludeMobileAppAssets();
	}

	@Input @Optional
	public Boolean getIncludeMobileAppAssets() {
		return includeMobileAppAssets;
	}

	public void setIncludeMobileAppAssets(Boolean includeMobileAppAssets) {
		this.includeMobileAppAssets = includeMobileAppAssets;
	}

	@Deprecated @Internal
	public Boolean isIncludeMobileDataset() {
		return getIncludeMobileDataset();
	}

	@Input @Optional
	public Boolean getIncludeMobileDataset() {
		return includeMobileDataset;
	}

	public void setIncludeMobileDataset(Boolean includeMobileDataset) {
		this.includeMobileDataset = includeMobileDataset;
	}

	@Deprecated @Internal
	public Boolean isIncludeMobilePlatformsAssets() {
		return getIncludeMobilePlatformsAssets();
	}

	@Input @Optional
	public Boolean getIncludeMobilePlatformsAssets() {
		return includeMobilePlatformsAssets;
	}

	public void setIncludeMobilePlatformsAssets(Boolean includeMobilePlatformsAssets) {
		this.includeMobilePlatformsAssets = includeMobilePlatformsAssets;
	}

	public ProjectCar() {
		Project project = getProject();
		try {
			destinationDir = project.file(project.getProperties().get("convertigo.car.destinationDir"));
		} catch (Exception e) {
			destinationDir = project.getLayout().getBuildDirectory().getAsFile().get();
		}
		try {
			includeTestCases = Boolean.parseBoolean(project.getProperties().get("convertigo.car.includeTestCases").toString());
		} catch (Exception e) {}
		try {
			includeStubs = Boolean.parseBoolean(project.getProperties().get("convertigo.car.includeStubs").toString());
		} catch (Exception e) {}
		try {
			includeMobileApp = Boolean.parseBoolean(project.getProperties().get("convertigo.car.includeMobileApp").toString());
		} catch (Exception e) {}
		try {
			includeMobileAppAssets = Boolean.parseBoolean(project.getProperties().get("convertigo.car.includeMobileAppAssets").toString());
		} catch (Exception e) {}
		try {
			includeMobileDataset = Boolean.parseBoolean(project.getProperties().get("convertigo.car.includeMobileDataset").toString());
		} catch (Exception e) {}
		try {
			includeMobilePlatformsAssets = Boolean.parseBoolean(project.getProperties().get("convertigo.car.includeMobilePlatformsAssets").toString());
		} catch (Exception e) {}
	}
	
	@TaskAction
	void taskAction() throws Exception {
		CLI cli = plugin.getCLI();
		destinationFile = cli.exportToCar(
				plugin.load.getConvertigoProject(), destinationDir, includeTestCases, includeStubs,
				includeMobileApp, includeMobileAppAssets, includeMobileDataset, includeMobilePlatformsAssets);
	}
}
