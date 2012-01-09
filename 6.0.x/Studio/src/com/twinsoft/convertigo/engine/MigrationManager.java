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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;

import com.twinsoft.convertigo.engine.migration.Migration3_0_0;

public class MigrationManager {
	private static Map<String, MigrationJob> jobs = new Hashtable<String, MigrationJob>(256);
	private static boolean jobsAdded = false;
	
	protected static void performProjectsMigration() {
		final File projectsDirFile = new File(Engine.PROJECTS_PATH);
		if (!projectsDirFile.exists())
			return; // no need to perform migration
		
		Thread t = new Thread(new Runnable(){
			public void run() {
				
				// Waits for Engine to Start
				while (!Engine.isStarted) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
				
				long t0 = Calendar.getInstance().getTime().getTime();
				Engine.logEngine.info("Migration starting...");
				try {
					String projectName;
					MigrationJob job;
					
					// Starts Convertigo projects (already deployed) migration
					File[] deployedProjects = projectsDirFile.listFiles(new FileFilter() {
				        public boolean accept(File file) {
				            return file.isDirectory() && (file.getName().indexOf('.')==-1);
				        }
					});
					File deployedProjectDir;
					for (int i = 0 ; i < deployedProjects.length ; i++) {
						deployedProjectDir = deployedProjects[i];
						projectName = deployedProjectDir.getName();
						job = new MigrationJob(projectName);
						if (!jobs.containsKey(projectName)) {
							jobs.put(projectName, job);
							job.start();
						}
					}
					
					// Starts Convertigo archives deployment and migration
					String[] projectArchives = projectsDirFile.list(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith(".car");
						}
					});
					String projectArchive;
					for (int i = 0 ; i < projectArchives.length ; i++) {
						projectArchive = projectArchives[i];
						projectName = projectArchive.substring(0, projectArchive.indexOf(".car"));
						job = new MigrationJob(projectName);
						if (!jobs.containsKey(projectName)) {
							jobs.put(projectName, job);
							job.start();
						}
					}
					
					jobsAdded = true;
					
					// Waits until all migrations are done
					for (MigrationJob j: jobs.values()) {
						try {
							j.join();
						} catch (InterruptedException e) {
							Engine.logEngine.error("An error occured while performing migration.", e);
						}
					}
					
					// Migration 3.0.0 specifics
					try {
						Migration3_0_0.done();
					}
					catch(Exception e) {
						Engine.logEngine.error("Unable to end successfully the 3.0.0 migration.", e);
					}
				}
				catch(Throwable t) {
					Engine.logEngine.error("An error occured while performing migration.", t);
				}
				finally {
					long t1 = Calendar.getInstance().getTime().getTime();
					Engine.logEngine.trace("Migration took (" + (t1 - t0) + "ms).");
					Engine.logEngine.info("Migration finished");
					if (Engine.isStarted && Engine.isStudioMode())
						Engine.theApp.fireMigrationFinished(new EngineEvent(""));
				}
			}
		});
		t.setName("Migration");
		t.start();
	}
	
	public static boolean isProjectMigrated(String projectName) {
		MigrationJob job = jobs.get(projectName);
		if (job != null) return job.isFinished;
		return jobsAdded;
	}
	
	public static boolean isMigrationFinished() {
		for (MigrationJob job: jobs.values()) {
			if (!job.isFinished) return false;
		}
		return true;
	}
}
