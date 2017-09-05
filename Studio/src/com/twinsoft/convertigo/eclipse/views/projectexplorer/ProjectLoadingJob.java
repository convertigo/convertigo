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

package com.twinsoft.convertigo.eclipse.views.projectexplorer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ResourceFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ResourceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TraceTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.DatabaseObjectImportedEvent;
import com.twinsoft.convertigo.engine.DatabaseObjectListener;
import com.twinsoft.convertigo.engine.DatabaseObjectLoadedEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ProjectInMigrationProcessException;
import com.twinsoft.convertigo.engine.helpers.WalkHelper;

public class ProjectLoadingJob extends Job implements DatabaseObjectListener {

	private String projectName;
	private DatabaseObjectTreeObject projectTreeObject;
	private ConnectorTreeObject defaultConnectorTreeObject;
	private TraceTreeObject demoTraceTreeObject;
	private UnloadedProjectTreeObject unloadedProjectTreeObject;
	private IProgressMonitor monitor;
	private Viewer viewer;
	private boolean isCopy;
	private String originalName;
	
	public ProjectLoadingJob(Viewer viewer, UnloadedProjectTreeObject unloadedProjectTreeObject) {
		this(viewer, unloadedProjectTreeObject, false, null);
	}

	public ProjectLoadingJob(Viewer viewer, UnloadedProjectTreeObject unloadedProjectTreeObject, boolean isCopy, String originalName) {
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
				return Status.OK_STATUS;
			}
			try {
				int worksNumber = 2 * ConvertigoPlugin.projectManager.getNumberOfObjects(projectName);
				monitor.beginTask("Opening project " + projectName + "...", worksNumber);
	
				if (monitor.isCanceled()) {
					Status status = new Status(Status.CANCEL, ConvertigoPlugin.PLUGIN_UNIQUE_ID, 0, "Project " + projectName + " not loaded because of user abort", null);
					return status;
				}
	
				monitor.subTask("Refreshing project ressources...");
				ConvertigoPlugin.projectManager.getProjectExplorerView().createDir(projectName);
				ConvertigoPlugin.getDefault().getProjectPluginResource(projectName, monitor);
	
				Engine.theApp.databaseObjectsManager.addDatabaseObjectListener(this);
				Project project;
				
				try {
					Engine.theApp.databaseObjectsManager.clearCacheIfSymbolError(projectName);
					project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
					if (project.undefinedGlobalSymbols) {
						synchronized (Engine.theApp.databaseObjectsManager) { // parallel projects opening with undefined symbols, check after the first wizard
							project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
							if (project.undefinedGlobalSymbols) {
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
								project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
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
				
				loadDatabaseObject(projectTreeObject, project);
				
				// Comment out the following line to disable the resources tree part
				//loadResource(projectTreeObject, "Resources", resourceProject.members());
				
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
								}
							});
							
						}
					}
				});
			}
		}
	}
	
	protected void loadResource(TreeParent parentTreeObject, Object folderObject, IResource[] members) throws CoreException {
		ResourceFolderTreeObject resourceFolderTreeObject;
		if (folderObject instanceof String) resourceFolderTreeObject = new ResourceFolderTreeObject(viewer, (String) folderObject);
		else resourceFolderTreeObject = new ResourceFolderTreeObject(viewer, (IFolder) folderObject);
		parentTreeObject.addChild(resourceFolderTreeObject);

		ResourceTreeObject resourceTreeObject;
		for (int i = 0; i < members.length; i++) {
			IResource resource = members[i];
			String name = resource.getName();
			if (resource instanceof IFolder) {
				if (name.equals("_data")) continue;
				if (name.equals("_lib")) continue;
				if (name.equals("_private")) continue;
				loadResource(resourceFolderTreeObject, ((IFolder) resource), ((IFolder) resource).members());
			}
			else {
				if (name.equals(".project")) continue;
				if (name.endsWith(".etr")) continue;
				resourceTreeObject = new ResourceTreeObject(viewer, (IFile) resource);
				resourceFolderTreeObject.addChild(resourceTreeObject);
			}
		}
	}

	public void loadTrace(TreeParent parentTreeObject, File dir) {
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
