/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.CLI;

public class ProjectLoad extends ConvertigoTask {
	@Internal
	private Project convertigoProject;
	
	private String projectVersion;

	synchronized Project getConvertigoProject() throws Exception {
		if (convertigoProject == null) {
			CLI cli = plugin.getCLI();
			
			convertigoProject = cli.loadProject(getProject().getProjectDir(), projectVersion);
		}
		return convertigoProject;
	}
	
	public String getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(String projectVersion) {
		this.projectVersion = projectVersion;
	}
	
	public ProjectLoad() {
		try {
			projectVersion = getProject().getProperties().get("convertigo.load.projectVersion").toString();
		} catch (Exception e) {
		}
		
		getProject().afterEvaluate(p -> {
			getInputs().getProperties().put("convertigo.load.projectVersion", projectVersion);
			File f = p.file("c8oProject.yaml");
			if (f.exists()) {
				getInputs().file(f);
			}
			File d = p.file("_c8oProject");
			if (d.exists()) {
				getInputs().dir(d);
			}
			getOutputs().file("c8oProject.yaml");
			getOutputs().dir("_c8oProject");
		});
	}
	
	@TaskAction
	void taskAction() throws Exception {
		getConvertigoProject();
	}
}
