/*
 * Copyright (c) 2001-2021 Convertigo SA.
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
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;

import com.twinsoft.convertigo.engine.CLI;

public class RemoteBuildDownload extends ConvertigoTask {
	
	private File destinationDir;
	private List<File> destinationFiles;

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
	
	@OutputFiles @Optional
	public List<File> getDestinationFiles() {
		return destinationFiles;
	}

	public RemoteBuildDownload() {
		Project project = getProject();
		try {
			destinationDir = project.file(project.getProperties().get("convertigo.downloadNativeDir.destinationDir"));
		} catch (Exception e) {
			destinationDir = project.getBuildDir();
		}
	}
	
	@TaskAction
	void taskAction() throws Exception {
		CLI cli = plugin.getCLI();
		destinationFiles = cli.downloadRemoteBuild(plugin.load.getConvertigoProject(), plugin.remoteBuild.getPlatforms(), destinationDir);
	}
}
