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

package com.twinsoft.convertigo.engine;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.twinsoft.convertigo.engine.util.ZipUtils;

public class MigrationManager {
	// Read by the supervision of probe threads while the migration thread fills it.
	private static Map<String, MigrationJob> jobs = new ConcurrentHashMap<String, MigrationJob>();
	private static volatile boolean jobsAdded = false;
	
	protected static void performProjectsMigration() {
		final File projectsDirFile = new File(Engine.PROJECTS_PATH);
		if (!projectsDirFile.exists() || Engine.isStudioMode() || Engine.isCliMode()) {
			jobsAdded = true;
			return; // no need to perform migration
		}
		
		Thread t = new Thread(new Runnable(){
			public void run() {
				
				// Waits for Engine to Start
				while (!Engine.isStarted) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
				
				long t0 = Calendar.getInstance().getTime().getTime();
				long cpus = Runtime.getRuntime().availableProcessors();
				Engine.logEngine.info("Migration starting...");
				try {
					//Added by julienda
					String targetProjectArchive = "";
					
					for (String projectName: Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false)) {
						if (!jobs.containsKey(projectName)) {
							while(countRunning() >= cpus) {
								Thread.sleep(100);
							}
							MigrationJob job = new MigrationJob(projectName);
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
						Engine.logEngine.trace("MigrationManager.performProjectsMigration() - projectArchive: "+projectArchive);
						Engine.logEngine.trace("MigrationManager.performProjectsMigration() - projectsDirFile: "+projectsDirFile);
						//projectName = projectArchive.substring(0, projectArchive.indexOf(".car"));
						
						//Added by julienda
						targetProjectArchive = (new File(projectsDirFile, projectArchive)).getPath();
						String projectName = ZipUtils.getProjectName(targetProjectArchive);
						if (projectName == null) {
							continue; // not a project archive (the job map takes no null key)
						}
						
						MigrationJob job = new MigrationJob(projectName);
						if (!jobs.containsKey(projectName)) {
							while(countRunning() >= cpus) {
								Thread.sleep(100);
							}
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
				}
				catch(Throwable t) {
					Engine.logEngine.error("An error occured while performing migration.", t);
				}
				finally {
					// A failure must not leave the projects reported as loading forever.
					jobsAdded = true;
					long t1 = Calendar.getInstance().getTime().getTime();
					Engine.logEngine.trace("Migration took (" + (t1 - t0) + "ms).");
					Engine.logEngine.info("Migration finished");
					if (Engine.isStarted)
						Engine.theApp.fireMigrationFinished(new EngineEvent(""));
				}
			}
		});
		t.setName("Migration");
		t.setDaemon(true);
		t.start();
	}
	
	public static boolean isProjectMigrated(String projectName) {
		MigrationJob job = jobs.get(projectName);
		if (job != null) return job.isFinished;
		return jobsAdded;
	}
	
	/**
	 * Whether every project of the workspace is loaded: false from the start of the engine until the jobs that load
	 * them are all added and finished (before they are added, the empty job map alone would tell finished).
	 */
	public static boolean areProjectsLoaded() {
		return jobsAdded && isMigrationFinished();
	}

	static boolean isMigrationFinished() {
		for (MigrationJob job: jobs.values()) {
			if (!job.isFinished) return false;
		}
		return true;
	}
	
	static long countRunning() {
		return jobs.values().stream().filter(j -> !j.isFinished).count();
	}
}
