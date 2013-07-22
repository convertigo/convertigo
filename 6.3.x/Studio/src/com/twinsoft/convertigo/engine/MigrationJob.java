/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.util.Calendar;

import com.twinsoft.convertigo.engine.migration.Migration3_0_0;

public class MigrationJob extends Thread {
	protected boolean isFinished;

	private String projectName;
	
	public MigrationJob(String projectName) {
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
			File projectDir = new File(Engine.PROJECTS_PATH + "/" + projectName);
			if (projectDir.exists())
				Engine.theApp.databaseObjectsManager.updateProject(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xml");
			else
				Engine.theApp.databaseObjectsManager.updateProject(Engine.PROJECTS_PATH + "/" + projectName + ".car");
			
			// Migration 3.0.0 specifics
			Migration3_0_0.projectRenameFilesWithDollarSign(projectDir);
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
