/*
 * Copyright (c) 2001-2023 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ConnectorTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TraceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectImportedEvent;
import com.twinsoft.convertigo.engine.DatabaseObjectListener;
import com.twinsoft.convertigo.engine.DatabaseObjectLoadedEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ProjectInMigrationProcessException;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

class ProjectLoadingJob extends Job implements DatabaseObjectListener {

	private String projectName;
	private DatabaseObjectTreeObject projectTreeObject;
	private ConnectorTreeObject defaultConnectorTreeObject;
	private TraceTreeObject demoTraceTreeObject;
	private UnloadedProjectTreeObject unloadedProjectTreeObject;
	private IProgressMonitor monitor;
	private Viewer viewer;
	private boolean isCopy;
	private String originalName;
	
	ProjectLoadingJob(Viewer viewer, UnloadedProjectTreeObject unloadedProjectTreeObject) {
		this(viewer, unloadedProjectTreeObject, false, null);
	}

	ProjectLoadingJob(Viewer viewer, UnloadedProjectTreeObject unloadedProjectTreeObject, boolean isCopy, String originalName) {
		super("Opening project " + unloadedProjectTreeObject.toString());
		this.unloadedProjectTreeObject = unloadedProjectTreeObject;
		this.originalName = originalName;
		this.viewer = viewer;
		this.isCopy = isCopy;
		projectName = unloadedProjectTreeObject.toString();
	}

	protected IStatus run(IProgressMonitor monitor) {
		this.monitor = monitor;
		synchronized (unloadedProjectTreeObject) {
			if (unloadedProjectTreeObject.getParent() == null) {
				Display.getDefault().asyncExec(() -> viewer.refresh());
				return Status.OK_STATUS;
			}
			try {
				int worksNumber = 2 * ConvertigoPlugin.projectManager.getNumberOfObjects(projectName);
				monitor.beginTask("Opening project " + projectName + "...", worksNumber);
	
				if (monitor.isCanceled()) {
					Status status = new Status(Status.CANCEL, ConvertigoPlugin.PLUGIN_UNIQUE_ID, 0, "Project " + projectName + " not loaded because of user abort", null);
					return status;
				}
	
				Project project;
				
				try {
					monitor.subTask("Importing the project...");
					synchronized (Engine.theApp.databaseObjectsManager) {
						Engine.theApp.databaseObjectsManager.clearCacheIfSymbolError(projectName);
						project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
					}
					if (project == null) {
						unloadedProjectTreeObject.getParent().removeChild(unloadedProjectTreeObject);
						Status status = new Status(Status.CANCEL, ConvertigoPlugin.PLUGIN_UNIQUE_ID, 0, "Project " + projectName + " doesn't exists", null);
						return status;
					}

					monitor.subTask("Refreshing project ressources...");
					String projectDir = Engine.projectDir(projectName);
					ConvertigoPlugin.projectManager.getProjectExplorerView().createDir(projectName);
					ConvertigoPlugin.getDefault().createProjectPluginResource(projectName, projectDir, monitor);
		
					Engine.theApp.databaseObjectsManager.addDatabaseObjectListener(this);
					
					if (project.undefinedGlobalSymbols) {
						synchronized (Engine.theApp.databaseObjectsManager) { // parallel projects opening with undefined symbols, check after the first wizard
							project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
							if (project.undefinedGlobalSymbols) {
								final boolean[] created = {false};
								new WalkHelper() {
									boolean create = false;
									boolean forAll = false;
									
									@Override
									protected void walk(DatabaseObject databaseObject) throws Exception {
										if (databaseObject.isSymbolError()) {
											for (Entry<String, Set<String>> entry : databaseObject.getSymbolsErrors().entrySet()) {
												Set<String> undefinedSymbols = Engine.theApp.databaseObjectsManager.symbolsSetCheckUndefined(entry.getValue());
												if (!undefinedSymbols.isEmpty()) {
													if (!forAll) {
														boolean [] response = ConvertigoPlugin.warningGlobalSymbols(projectName,
																databaseObject.getName(), databaseObject.getDatabaseType(),
																entry.getKey(), "" + databaseObject.getCompilablePropertySourceValue(entry.getKey()),
																undefinedSymbols, true);
														create = response[0];
														forAll = response[1];
														
														created[0] |= create;
													}
													if (create) {
														Engine.theApp.databaseObjectsManager.symbolsCreateUndefined(undefinedSymbols);
													}
												}
											}
										}
										super.walk(databaseObject);
									}
								}.init(project);
								if (created[0]) {
									project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
								}
							}
						}
					}
				}
				finally {
					Engine.theApp.databaseObjectsManager.removeDatabaseObjectListener(this);
				}
				
				if (monitor.isCanceled()) {
					Status status = new Status(Status.CANCEL, ConvertigoPlugin.PLUGIN_UNIQUE_ID, 0, "Project " + projectName + " not loaded because of user abort", null);
					return status;
				}
	
				monitor.subTask("Creating connectors...");
	
				monitor.subTask("Updating tree view...");
				TreeParent invisibleRoot = unloadedProjectTreeObject.getParent();
				projectTreeObject = new ProjectTreeObject(viewer, project);
				defaultConnectorTreeObject = null;
				demoTraceTreeObject = null;
				
				invisibleRoot.removeChild(unloadedProjectTreeObject);
				invisibleRoot.addChild(projectTreeObject);
				ConvertigoPlugin.projectManager.setCurrentProject((ProjectTreeObject)projectTreeObject);
				ConvertigoPlugin.getDefault().getProjectPluginResource(projectName, monitor).refreshLocal(IResource.DEPTH_INFINITE, monitor);
				
				loadDatabaseObject(projectTreeObject, project);
				
				// Comment out the following line to disable the resources tree part
				//loadResource(projectTreeObject, "Resources", ((ProjectTreeObject) projectTreeObject).getIProject().members());
				
				Status status = new Status(Status.OK, ConvertigoPlugin.PLUGIN_UNIQUE_ID, 0, "Project " + projectName + " loaded", null);
				return status;
			}
			catch(Exception e) {
				Status status = null;
				unloadedProjectTreeObject.isLoadable = false;
				if (e.getCause() instanceof ProjectInMigrationProcessException)
					status = new Status(Status.WARNING, ConvertigoPlugin.PLUGIN_UNIQUE_ID, 0, "Could not open project \""+projectName+"\" while it was still in migration check process", e);
				else
					status = new Status(Status.ERROR, ConvertigoPlugin.PLUGIN_UNIQUE_ID, 0, "Error while loading project " + projectName, e);
				return status;
			}
			finally {
				// Updating the tree viewer
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						if (projectTreeObject != null) {
							viewer.refresh();
							ISelection selection = new StructuredSelection(projectTreeObject);
							viewer.setSelection(selection, true);
							
							if (defaultConnectorTreeObject != null && ConvertigoPlugin.getAutoOpenDefaultConnector())
								defaultConnectorTreeObject.launchEditor();
	
							TreeObjectEvent treeObjectEvent = null;
							if (isCopy)
								treeObjectEvent = new TreeObjectEvent(projectTreeObject,"name",originalName,projectName,0);
							else
								treeObjectEvent = new TreeObjectEvent(projectTreeObject);
							ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectAdded(treeObjectEvent);
	
							viewer.getControl().getDisplay().asyncExec(new Runnable() {
								public void run() {
									if (demoTraceTreeObject != null)
										demoTraceTreeObject.play(false);
									
									Engine.execute(() -> {
										Project.executeAutoStartSequences(projectName);
									});
								}
							});
							
						}
					}
				});
			}
		}
	}
	
	void loadTrace(TreeParent parentTreeObject, File dir) {
		FolderTreeObject folderTreeObject = new FolderTreeObject(viewer, "Traces");
		parentTreeObject.addChild(folderTreeObject);
		
		/** Ticket #689 workaround, until better IRessource integration */
		try {
			String dirp = dir.getPath();
			dirp = dirp.replaceFirst(".*/(.*/.*)$", "$1");
			IFolder ifolder = parentTreeObject.getProjectTreeObject().getFolder(dirp);
			if(!ifolder.exists())
				ifolder.create(true, true, null);
		} catch (Exception e) {
			if (!dir.exists()) {
				if (!dir.mkdir())
					return;
			}
		}

		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if (new File(dir, name).isFile() && (name.endsWith(".etr"))) return true;
				return false;
			}
		});
		if (files == null) return;

		File file;
		TraceTreeObject traceTreeObject;
		for (int i = 0; i < files.length; i++) {
			file = files[i];
			traceTreeObject = new TraceTreeObject(viewer, file);
			folderTreeObject.addChild(traceTreeObject);
			
			if (file.getName().equals("demo.etr")) {
				if (demoTraceTreeObject == null)
					demoTraceTreeObject = traceTreeObject;
			}
		}
	}

	private void loadDatabaseObject(TreeParent parentTreeObject, DatabaseObject parentDatabaseObject) throws EngineException, IOException {
		ConvertigoPlugin.projectManager.getProjectExplorerView().loadDatabaseObject(parentTreeObject, parentDatabaseObject, this);
	}

	public void databaseObjectLoaded(DatabaseObjectLoadedEvent event) {
		monitor.subTask("Object \"" + ((DatabaseObject) event.getSource()).getName() + "\" loaded");
		monitor.worked(1);
	}
	
	public void databaseObjectImported(DatabaseObjectImportedEvent event) {
		
	}

	public void setDefaultConnectorTreeObject(ConnectorTreeObject defaultConnectorTreeObject) {
		this.defaultConnectorTreeObject = defaultConnectorTreeObject;
	}

	public IProgressMonitor getMonitor() {
		return monitor;
	}
}
