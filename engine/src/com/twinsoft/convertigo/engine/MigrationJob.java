/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.util.Calendar;

import com.twinsoft.convertigo.engine.util.FileUtils;

class MigrationJob extends Thread {
	protected boolean isFinished;

	private String projectName;
	
	MigrationJob(String projectName) {
		this.projectName = projectName;
		setName("MigrationJob #" + projectName);
	}
	
	public String getProjectName() {
		return projectName;
	}
	
	public void run() {
		isFinished = false;
		long t0 = Calendar.getInstance().getTime().getTime();
		Engine.logEngine.trace("MigrationJob for project \"" + projectName + "\" started.");
		try {
			// Migrates project
			File projectDir = new File(Engine.projectDir(projectName));
			if (projectDir.exists()) {
				FileUtils.deleteQuietly(new File(projectDir, "_data/download"));
				File toLoad = new File(projectDir, "c8oProject.yaml");
				if (!toLoad.exists()) {
					toLoad = new File(projectDir, projectName + ".xml");
				}
				Engine.theApp.databaseObjectsManager.updateProject(toLoad);
			} else {
				Engine.theApp.databaseObjectsManager.updateProject(new File(Engine.PROJECTS_PATH, projectName + ".car"));
			}
		}
		catch(Throwable t) {
			Engine.logEngine.error("An error occured while migrating project \"" + projectName + "\".", t);
		}
		finally {
			isFinished = true;
			long t1 = Calendar.getInstance().getTime().getTime();
			Engine.logEngine.trace("MigrationJob for project \"" + projectName + "\" finished (" + (t1 - t0) + "ms).");
			if (Engine.isStarted && Engine.isStudioMode())
				Engine.theApp.fireProjectMigrated(new EngineEvent(projectName));
		}
	}
	
}
