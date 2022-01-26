/*
 * Copyright (c) 2001-2022 Convertigo SA.
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
	
	@Input @Optional
	public boolean isIncludeTestCases() {
		return includeTestCases;
	}

	public void setIncludeTestCases(boolean includeTestCases) {
		this.includeTestCases = includeTestCases;
	}
	
	@Input @Optional
	public boolean isIncludeStubs() {
		return includeStubs;
	}

	public void setIncludeStubs(boolean includeStubs) {
		this.includeStubs = includeStubs;
	}

	@Input @Optional
	public boolean isIncludeMobileApp() {
		return includeMobileApp;
	}

	public void setIncludeMobileApp(boolean includeMobileApp) {
		this.includeMobileApp = includeMobileApp;
	}

	@Input @Optional
	public boolean isIncludeMobileAppAssets() {
		return includeMobileAppAssets;
	}

	public void setIncludeMobileAppAssets(boolean includeMobileAppAssets) {
		this.includeMobileAppAssets = includeMobileAppAssets;
	}

	@Input @Optional
	public boolean isIncludeMobileDataset() {
		return includeMobileDataset;
	}

	public void setIncludeMobileDataset(boolean includeMobileDataset) {
		this.includeMobileDataset = includeMobileDataset;
	}

	@Input @Optional
	public boolean isIncludeMobilePlatformsAssets() {
		return includeMobilePlatformsAssets;
	}

	public void setIncludeMobilePlatformsAssets(boolean includeMobilePlatformsAssets) {
		this.includeMobilePlatformsAssets = includeMobilePlatformsAssets;
	}

	public void setDestinationFile(File destinationFile) {
		this.destinationFile = destinationFile;
	}

	public ProjectCar() {
		Project project = getProject();
		try {
			destinationDir = project.file(project.getProperties().get("convertigo.car.destinationDir"));
		} catch (Exception e) {
			destinationDir = project.getBuildDir();
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
		
//		project.afterEvaluate(p -> {
//			Matcher filter = Pattern.compile("\\.gradle|\\.svn|\\.git|build|_private").matcher("");
//			getInputs().files((Object[]) project.getProjectDir().listFiles((f, s) -> !filter.reset(s).matches()));
//			
//			File yaml = project.file("c8oProject.yaml");
//			try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(yaml), "UTF-8"))) {
//				br.readLine();
//				Matcher m = Pattern.compile("↓(.*) \\[core\\.Project\\]:").matcher(br.readLine());
//				if (m.find()) {
//					String projectName = m.group(1);
//					getOutputs().file(new File(destinationDir, projectName + ".car"));
//				}
//			} catch (Exception e) {
//			}
//		});
	}
	
	@TaskAction
	void taskAction() throws Exception {
		CLI cli = plugin.getCLI();
		destinationFile = cli.exportToCar(
				plugin.load.getConvertigoProject(), destinationDir, includeTestCases, includeStubs,
				includeMobileApp, includeMobileAppAssets, includeMobileDataset, includeMobilePlatformsAssets);
	}
}
