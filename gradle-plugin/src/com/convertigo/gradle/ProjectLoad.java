/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.CLI;

public class ProjectLoad extends ConvertigoTask {
	private Project convertigoProject;
	private String projectVersion;
	private String mobileApplicationEndpoint;
	private File gitContainer;

	@Internal
	synchronized Project getConvertigoProject() throws Exception {
		if (convertigoProject == null) {
			CLI cli = plugin.getCLI();
			
			convertigoProject = cli.loadProject(getProject().getProjectDir(), projectVersion, mobileApplicationEndpoint, gitContainer == null ? null : gitContainer.getAbsolutePath());
		}
		return convertigoProject;
	}
	
	@Input @Optional
	public String getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}
	
	@Input @Optional
	public String getMobileApplicationEndpoint() {
		return mobileApplicationEndpoint;
	}

	public void setMobileApplicationEndpoint(String mobileApplicationEndpoint) {
		this.mobileApplicationEndpoint = mobileApplicationEndpoint;
	}

	@Internal
	public File getGitContainer() {
		return gitContainer;
	}

	public void setGitContainer(File gitContainer) {
		this.gitContainer = gitContainer;
	}

	public void setGitContainer(String gitContainer) {
		this.gitContainer = new File(gitContainer);
	}

	public ProjectLoad() {
		try {
			projectVersion = getProject().getProperties().get("convertigo.load.projectVersion").toString();
		} catch (Exception e) {}
		try {
			mobileApplicationEndpoint = getProject().getProperties().get("convertigo.load.mobileApplicationEndpoint").toString();
		} catch (Exception e) {}
		try {
			gitContainer = new File(getProject().getProperties().get("convertigo.load.gitContainer").toString());
		} catch (Exception e) {}
	}
	
	@TaskAction
	void taskAction() throws Exception {
		getConvertigoProject();
	}
}
