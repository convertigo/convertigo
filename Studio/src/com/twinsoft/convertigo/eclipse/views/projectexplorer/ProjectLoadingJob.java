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
import com.twinsoft.convertigo.engine.DatabaseObjectImportedEvent;
import com.twinsoft.convertigo.engine.DatabaseObjectListener;
import com.twinsoft.convertigo.engine.DatabaseObjectLoadedEvent;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ProjectInMigrationProcessException;

public class ProjectLoadingJob extends Job implements DatabaseObjectListener {

	private String projectName;
	private DatabaseObjectTreeObject projectTreeObject;
	private ConnectorTreeObject defaultConnectorTreeObject;
	private TraceTreeObject demoTraceTreeObject;
	private UnloadedProjectTreeObject unloadedProjectTreeObject;
	private IProgressMonitor monitor;
	private Viewer viewer;
	//private IProject project;
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
				project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
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
						
						if (defaultConnectorTreeObject != null)
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
	
//	private void loadResource(TreeParent parentTreeObject, File dir) {
//		IWorkspace workspace = ResourcesPlugin.getWorkspace();
//		IWorkspaceRoot workspaceRoot = workspace.getRoot();
//
//		String folderName = dir.getName();
//		ResourceFolderTreeObject resourceFolderTreeObject;
//		IFolder folder = null;
//		if (folderName.equals(projectName)) {
//			folderName = "Resources";
//			resourceFolderTreeObject = new ResourceFolderTreeObject(viewer, folderName);
//		}
//		else {
//			folder = workspaceRoot.getFolder(new Path(dir.getAbsolutePath()));
//			resourceFolderTreeObject = new ResourceFolderTreeObject(viewer, folder);
//		}
//		parentTreeObject.addChild(resourceFolderTreeObject);
//
//		// Directories
//		File[] files = dir.listFiles(new FilenameFilter() {
//			public boolean accept(File dir, String name) {
//				if (new File(dir, name).isFile()) return false;
//				if (name.equals("_data")) return false;
//				if (name.equals("_lib")) return false;
//				if (name.equals("_private")) return false;
//				return true;
//			}
//		});
//		if (files == null) return;
//
//		File file;
//		ResourceTreeObject resourceTreeObject;
//		for (int i = 0; i < files.length; i++) {
//			file = files[i];
//			loadResource(resourceFolderTreeObject, file);
//		}
//
//		// Files
//		files = dir.listFiles(new FilenameFilter() {
//			public boolean accept(File dir, String name) {
//				if (new File(dir, name).isDirectory()) return false;
//				if (name.equals(projectName + ".xml")) return false;
//				if (name.endsWith(".car")) return false;
////				if (name.endsWith(".mnt")) return false;
////				if (name.endsWith(".etr")) return false;
//				return true;
//			}
//		});
//		if (files == null) return;
//
//		IFile resource;
//		for (int i = 0; i < files.length; i++) {
//			file = files[i];
//			resource = workspaceRoot.getFile(new Path(file.getAbsolutePath() + "/" + file.getName()));
//			resourceTreeObject = new ResourceTreeObject(viewer, resource);
//			resourceFolderTreeObject.addChild(resourceTreeObject);
//		}
//	}
	
//	private void loadResource(TreeParent parentFolderTreeObject, IResource resource) {
//		TreeObject resourceTreeObject;
//		if (resource instanceof IFolder) {
//			resourceTreeObject = new ResourceFolderTreeObject(viewer, resource.getName());
//			parentFolderTreeObject.addChild(resourceTreeObject);
//			
//			IWorkspace workspace = ResourcesPlugin.getWorkspace();
//	        IProject resourceProject = workspace.getRoot().getProject(projectName);
//
//			for (int i = 0; i < ((IFolder) resource).; i++) {
//				file = files[i];
//				resourceTreeObject = new ResourceTreeObject(viewer, file.getName());
//				resourceFolderTreeObject.addChild(resourceTreeObject);
//			}
//		}
//		else if (resource instanceof IFile) {
//			resourceTreeObject = new ResourceTreeObject(viewer, file.getName());
//			parentFolderTreeObject.addChild(resourceTreeObject);
//		}
//	}
	
//	private void loadResource(TreeParent parentFolderTreeObject, IResource resource) {
//		TreeObject resourceTreeObject;
//		if (resource instanceof IFolder) {
//			resourceTreeObject = new ResourceFolderTreeObject(viewer, resource.getName());
//			parentFolderTreeObject.addChild(resourceTreeObject);
//			
//			IWorkspace workspace = ResourcesPlugin.getWorkspace();
//	        IProject resourceProject = workspace.getRoot().getProject(projectName);
//
//			for (int i = 0; i < ((IFolder) resource).; i++) {
//				file = files[i];
//				resourceTreeObject = new ResourceTreeObject(viewer, file.getName());
//				resourceFolderTreeObject.addChild(resourceTreeObject);
//			}
//		}
//		else if (resource instanceof IFile) {
//			resourceTreeObject = new ResourceTreeObject(viewer, file.getName());
//			parentFolderTreeObject.addChild(resourceTreeObject);
//		}
//	}
//	
//	private void createDirsAndFiles() {
//		createDir();
//		createFiles();
//	}
//	
//	private void createDir() {
//		File file = null;
//		
//		file = new File(Engine.PROJECTS_DIRECTORY + "/" + projectName + "/_private");
//		if (!file.exists())
//			file.mkdir();
//
//		file = new File(Engine.PROJECTS_DIRECTORY + "/" + projectName + "/Traces");
//		if (!file.exists())
//			file.mkdir();
//	}
//	
//	private void createFiles() {
//		if ((originalName != null) && (!originalName.equals(projectName))) {
//			if (isCopy) {
//				copyXsd();
//				copyWsdl();
//			}
//			else {
//				renameXsd();
//				renameWsdl();
//			}
//		}
//		else {
//			createXsd();
//			createWsdl();
//		}
//	}
//	
//	private void copyXsd() {
//		try {
//			ProjectUtils.copyXsdFile(Engine.PROJECTS_DIRECTORY, originalName, projectName);
//		} catch (IOException e) {
//			ConvertigoPlugin.logException(e,"Error while copying xsd file for project '" + projectName + "'", Boolean.FALSE);
//		}
//		
//		createTempXsd();
//	}
//		
//	private void copyWsdl() {
//		try {
//			ProjectUtils.copyWsdlFile(Engine.PROJECTS_DIRECTORY, originalName, projectName);
//		} catch (IOException e) {
//			ConvertigoPlugin.logException(e,"Error while copying wsdl file for project '" + projectName + "'", Boolean.FALSE);
//		}
//		
//		createTempWsdl();
//	}
//
//	private void renameXsd() {
//		try {
//			ProjectUtils.renameXsdFile(Engine.PROJECTS_DIRECTORY, originalName, projectName);
//		} catch (Exception e) {
//			ConvertigoPlugin.logException(e,"Error while renaming xsd file for project '" + projectName + "'", Boolean.FALSE);
//		}
//		
//		File xsdTemp = new File(Engine.PROJECTS_DIRECTORY + "/" + projectName + "/" + originalName + ".temp.xsd");
//		xsdTemp.delete();
//		createTempXsd();
//	}
//	
//	private void renameWsdl() {
//		try {
//			ProjectUtils.renameWsdlFile(Engine.PROJECTS_DIRECTORY, originalName, projectName);
//		} catch (Exception e) {
//			ConvertigoPlugin.logException(e,"Error while renaming wsdl file for project '" + projectName + "'", Boolean.FALSE);
//		}
//		
//		File wsdlTemp = new File(Engine.PROJECTS_DIRECTORY + "/" + projectName + "/" + originalName + ".temp.wsdl");
//		wsdlTemp.delete();
//		createTempWsdl();
//		
//	}
//
//	private void createXsd() {
//		try {
//			ProjectUtils.createXsdFile(Engine.PROJECTS_DIRECTORY, projectName);
//		} catch (Exception e) {
//			ConvertigoPlugin.logException(e,"Error while creating xsd file for project '" + projectName + "'", Boolean.FALSE);
//		}
//		
//		createTempXsd();
//	}
//	
//	private void createWsdl() {
//		try {
//			ProjectUtils.createWsdlFile(Engine.PROJECTS_DIRECTORY, projectName);
//		} catch (Exception e) {
//			ConvertigoPlugin.logException(e,"Error while creating wsdl file for project '" + projectName + "'", Boolean.FALSE);
//		}
//		
//		createTempWsdl();
//	}
//
//	private void createTempXsd() {
//		String xsdPath = Engine.PROJECTS_DIRECTORY + "/" + projectName + "/" + projectName + ".xsd";
//		String tempPath = Engine.PROJECTS_DIRECTORY + "/" + projectName + "/" + projectName + ".temp.xsd";
//		try {
//			copyToTemp(xsdPath, tempPath);
//		} catch (Exception e) {
//			ConvertigoPlugin.logException(e,"Error while creating temporary xsd file for project '" + projectName + "'", Boolean.FALSE);
//		}
//	}
//	
//	private void createTempWsdl() {
//		String wsdlPath = Engine.PROJECTS_DIRECTORY + "/" + projectName + "/" + projectName + ".wsdl";
//		String tempPath = Engine.PROJECTS_DIRECTORY + "/" + projectName + "/" + projectName + ".temp.wsdl";
//		try {
//			copyToTemp(wsdlPath, tempPath);
//		} catch (Exception e) {
//			ConvertigoPlugin.logException(e,"Error while creating temporary wsdl file for project '" + projectName + "'");
//		}
//	}
//
//	private void copyToTemp(String sourceFilePath, String targetFilePath) throws EngineException {
//		try {
//			File sourceFile = new File(sourceFilePath);
//			if (sourceFile.exists()) {
//				File targetFile = new File(targetFilePath);
//				if (!targetFile.exists()) {
//					try {
//						if (targetFile.createNewFile()) {
//							String line;
//							BufferedReader br = new BufferedReader(new FileReader(sourceFilePath));
//							BufferedWriter bw = new BufferedWriter(new FileWriter(targetFilePath));
//							while((line = br.readLine()) != null) {
//								line = line.replaceAll(projectName+".xsd", projectName+".temp.xsd");
//							    bw.write(line);
//							    bw.newLine();
//							}
//							bw.close();
//						}
//						else {
//							throw new EngineException("Error while creating '"+targetFilePath+"'");
//						}
//					}
//					catch (IOException e) {
//						throw new EngineException("Error while writing from '"+sourceFilePath+"' to '"+ targetFilePath +"'");
//					}
//				}
//			}
//			else {
//				throw new EngineException("'"+sourceFilePath+"' does not exist");
//			}
//		}
//		catch (Exception e) {
//			throw new EngineException("Unable to copy '"+sourceFilePath+"' to '"+targetFilePath+"'",e);
//		}
//	}

	private void loadDatabaseObject(TreeParent parentTreeObject, DatabaseObject parentDatabaseObject) throws EngineException, IOException {
		ConvertigoPlugin.projectManager.getProjectExplorerView().loadDatabaseObject(parentTreeObject, parentDatabaseObject, this);
//		
//		DatabaseObjectTreeObject childTreeObject;
//        ObjectsFolderTreeObject objectsFolderTreeObject;
//		
//        List<? extends DatabaseObject> vChildDatabaseObjects;
//        DatabaseObject childDBO;
//        int len;
//        
//        // Add load subtask here because of databaseObjectLoaded event no more received since memory improvement
//        // (getSubDatabaseObject called only when necessary)
//        monitor.subTask("Loading databaseObject '"+ parentDatabaseObject.getName() +"'...");
//        
//        if (parentDatabaseObject instanceof Project) {
//            Project project = (Project) parentDatabaseObject;
//            
//            // Creates directories and files
//            createDirsAndFiles();
//            
//			// Connectors
//			vChildDatabaseObjects = project.getConnectorsList();
//			len = vChildDatabaseObjects.size();
//			if (len != 0) {
//				// Set default connector if none
//				if (project.getDefaultConnector() == null) {
//					// Report from 4.5: fix #401
//					ConvertigoPlugin.logWarning(null, "Project \""+ project.getName() +"\" has no default connector. Try to set a default one.");
//					Connector defaultConnector = (Connector)vChildDatabaseObjects.get(0);
//					try {
//						project.setDefaultConnector(defaultConnector);
//						defaultConnector.hasChanged = true;
//					}
//					catch (Exception e) {
//						ConvertigoPlugin.logWarning(e, "Unable to set a default connector for project \""+ project.getName() +"\"");
//					}
//				}
//				
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_CONNECTORS);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//				
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//					childTreeObject = new ConnectorTreeObject(viewer, childDatabaseObject, false);
//					objectsFolderTreeObject.addChild(childTreeObject);
//			        monitor.worked(1);
//					loadDatabaseObject(childTreeObject, childDatabaseObject);
//				}
//			}
//            
//			// Sequences
//			vChildDatabaseObjects = project.getSequencesList();
//			len = vChildDatabaseObjects.size();
//			if (len != 0) {
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_SEQUENCES);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//					childTreeObject = new SequenceTreeObject(viewer, childDatabaseObject, false);
//					objectsFolderTreeObject.addChild(childTreeObject);
//			        monitor.worked(1);
//					loadDatabaseObject(childTreeObject, childDatabaseObject);
//				}
//			}
//		}
//		else if (parentDatabaseObject instanceof Connector) {
//			Connector connector = (Connector) parentDatabaseObject;
//			
//			// Open connector editor
//			if (connector.isDefault) {
//				defaultConnectorTreeObject = (ConnectorTreeObject)parentTreeObject;
//			}
//			
//			// Traces
//			if (connector instanceof JavelinConnector)
//				loadTrace(parentTreeObject, new File(Engine.PROJECTS_DIRECTORY + "/" + projectName + "/Traces/" + connector.getName()));
//			
//			// Pools
//			vChildDatabaseObjects = connector.getPoolsList();
//			len = vChildDatabaseObjects.size();
//			if (len != 0) {
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_POOLS);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//					childTreeObject = new DatabaseObjectTreeObject(viewer, childDatabaseObject, false);
//					objectsFolderTreeObject.addChild(childTreeObject);
//			        monitor.worked(1);
//				}
//			}
//            
//			// Transactions
//			vChildDatabaseObjects = connector.getTransactionsList();
//			len = vChildDatabaseObjects.size();
//			if (len != 0) {
//				// Set default transaction if none
//				if (connector.getDefaultTransaction() == null) {
//					// Report from 4.5: fix #401
//					ConvertigoPlugin.logWarning(null, "For project \""+ connector.getProject().getName() +"\" :\nConnector \""+ connector.getName() +"\" has no default transaction. Try to set a default one.");
//					Transaction defaultTransaction = (Transaction)vChildDatabaseObjects.get(0);
//					try {
//						connector.setDefaultTransaction(defaultTransaction);
//						defaultTransaction.hasChanged = true;
//					}
//					catch (Exception e) {
//						ConvertigoPlugin.logWarning(e, "For project \""+ connector.getProject().getName() +"\" :\nUnable to set a default transaction for connector \""+ connector.getName() +"\"");
//					}
//				}
//
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_TRANSACTIONS);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//					childTreeObject = new TransactionTreeObject(viewer, childDatabaseObject, false);
//					objectsFolderTreeObject.addChild(childTreeObject);
//			        monitor.worked(1);
//					loadDatabaseObject(childTreeObject, childDatabaseObject);
//				}
//			}
//
//			if (parentDatabaseObject instanceof JavelinConnector) {
//				// Root screen class
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_SCREEN_CLASSES);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//        
//				childDBO = ((JavelinConnector) parentDatabaseObject).getDefaultScreenClass();
//				childTreeObject = new ScreenClassTreeObject(viewer, childDBO, false);
//				objectsFolderTreeObject.addChild(childTreeObject);
//		        monitor.worked(1);
//				loadDatabaseObject(childTreeObject, childDBO);
//			}
//			else if (parentDatabaseObject instanceof HtmlConnector) {
//				// Root screen class
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_SCREEN_CLASSES);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//        
//				childDBO = ((HtmlConnector) parentDatabaseObject).getDefaultScreenClass();
//				childTreeObject = new ScreenClassTreeObject(viewer, childDBO, false);
//				objectsFolderTreeObject.addChild(childTreeObject);
//		        monitor.worked(1);
//				loadDatabaseObject(childTreeObject, childDBO);
//			}			
//		}
//		else if (parentDatabaseObject instanceof Sequence) {
//			Sequence sequence = (Sequence) parentDatabaseObject;
//			
//			// Steps
//			vChildDatabaseObjects = sequence.getSteps();
//			len = vChildDatabaseObjects.size();
//			if (len != 0) {
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_STEPS);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//					childTreeObject = new StepTreeObject(viewer, childDatabaseObject, false);
//					objectsFolderTreeObject.addChild(childTreeObject);
//			        monitor.worked(1);
//					loadDatabaseObject(childTreeObject, childDatabaseObject);
//				}
//			}
//			
//            // Sheets
//            vChildDatabaseObjects = sequence.getSheets();
//            len = vChildDatabaseObjects.size();
//            if (len != 0) {
//                objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_SHEETS);
//                parentTreeObject.addChild(objectsFolderTreeObject);
//                
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new SheetTreeObject(viewer, childDatabaseObject, (parentDatabaseObject != childDatabaseObject.getParent()));
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                    loadDatabaseObject(childTreeObject, childDatabaseObject);
//                }
//            }
//
//			// Variables
//            vChildDatabaseObjects = sequence.getVariablesList();
//            
//            len = vChildDatabaseObjects.size();
//			if (len != 0) {
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new VariableTreeObject2(viewer, childDatabaseObject, false);
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                }
//			}
//		}
//		else if (parentDatabaseObject instanceof Transaction) {
//            Transaction transaction = (Transaction) parentDatabaseObject;
//            
//            // Sheets
//            vChildDatabaseObjects = transaction.getSheets();
//            
//            len = vChildDatabaseObjects.size();
//            if (len != 0) {
//                objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_SHEETS);
//                parentTreeObject.addChild(objectsFolderTreeObject);
//                
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new SheetTreeObject(viewer, childDatabaseObject, false);
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                }
//            }
//            
//            // Statements (for HtmlTransaction only)
//            if (parentDatabaseObject instanceof HtmlTransaction) {
//            	HtmlTransaction htmlTransaction = ((HtmlTransaction)parentDatabaseObject);
//            	
//                vChildDatabaseObjects = htmlTransaction.getStatements();
//                
//                len = vChildDatabaseObjects.size();
//                if (len != 0) {
//                    objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_FUNCTIONS);
//                    parentTreeObject.addChild(objectsFolderTreeObject);
//                    
//    				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                        childTreeObject = new StatementTreeObject(viewer, childDatabaseObject, false);
//                        objectsFolderTreeObject.addChild(childTreeObject);
//                        monitor.worked(1);
//                        loadDatabaseObject(childTreeObject, childDatabaseObject);
//                    }
//                }
//            }
//            
//			// Functions
//			Vector treeObjects = new Vector(32);
//			String line, functionName;
//			int lineNumber = 0;
//			BufferedReader br = new BufferedReader(new StringReader(transaction.handlers));
//	
//			HandlersDeclarationTreeObject handlersDeclarationTreeObject; 
//			while ((line = br.readLine()) != null) {
//				line = line.trim();
//				lineNumber++;
//				if (line.startsWith("function ")) {
//					try {
//						functionName = line.substring(9, line.indexOf(')') + 1);
//
//						if (functionName.endsWith(JavelinTransaction.EVENT_ENTRY_HANDLER + "()")) {
//							handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(viewer, functionName, HandlersDeclarationTreeObject.TYPE_FUNCTION_SCREEN_CLASS_ENTRY, lineNumber);
//						}
//						else if (functionName.endsWith(JavelinTransaction.EVENT_EXIT_HANDLER + "()")) {
//							handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(viewer, functionName, HandlersDeclarationTreeObject.TYPE_FUNCTION_SCREEN_CLASS_EXIT, lineNumber);
//						}
//						else {
//							handlersDeclarationTreeObject = new HandlersDeclarationTreeObject(viewer, functionName, HandlersDeclarationTreeObject.TYPE_OTHER, lineNumber);
//						}
//						treeObjects.add(handlersDeclarationTreeObject);
//					}
//					catch(StringIndexOutOfBoundsException e) {
//						// Ignore
//					}
//				}
//			}
//
//			len = treeObjects.size();
//			if (len != 0) {
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_FUNCTIONS);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//
//				Enumeration enumTreeObjects = treeObjects.elements();
//				while (enumTreeObjects.hasMoreElements()) {
//					objectsFolderTreeObject.addChild((HandlersDeclarationTreeObject) enumTreeObjects.nextElement());
//				}
//			}
//            
//			// Variables
//			if (parentDatabaseObject instanceof TransactionWithVariables) {
//				TransactionWithVariables transactionWV = (TransactionWithVariables) transaction;
//	            vChildDatabaseObjects = transactionWV.getVariablesList();
//	            
//	            len = vChildDatabaseObjects.size();
//				if (len != 0) {
//					objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES);
//					parentTreeObject.addChild(objectsFolderTreeObject);
//
//					for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//	                    childTreeObject = new VariableTreeObject2(viewer, childDatabaseObject, false);
//	                    objectsFolderTreeObject.addChild(childTreeObject);
//	                    monitor.worked(1);
//	                }
//				}
//				
//			}
//        }
//		else if (parentDatabaseObject instanceof StatementWithExpressions) {
//			StatementWithExpressions statement = (StatementWithExpressions)parentDatabaseObject;
//            
//			// Statements
//            vChildDatabaseObjects = statement.getStatements();
//            
//            len = vChildDatabaseObjects.size();
//            if (len != 0) {
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new StatementTreeObject(viewer, childDatabaseObject, false);
//                    parentTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                    loadDatabaseObject(childTreeObject, childDatabaseObject);
//                }
//            }
//		}
//		else if (parentDatabaseObject instanceof HTTPStatement) {
//			HTTPStatement httpStatement = (HTTPStatement)parentDatabaseObject;
//			
//			// Variables
//            vChildDatabaseObjects = httpStatement.getVariables();
//            
//            len = vChildDatabaseObjects.size();
//			if (len != 0) {
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new VariableTreeObject2(viewer, childDatabaseObject, false);
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                }
//			}
//		}
//		else if (parentDatabaseObject instanceof StepWithExpressions) {
//			StepWithExpressions step = (StepWithExpressions)parentDatabaseObject;
//            
//			// Steps
//            vChildDatabaseObjects = step.getSteps();
//            
//            len = vChildDatabaseObjects.size();
//            if (len != 0) {
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new StepTreeObject(viewer, childDatabaseObject, false);
//                    parentTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                    loadDatabaseObject(childTreeObject, childDatabaseObject);
//                }
//            }
//		}
//        else if (parentDatabaseObject instanceof RequestableStep) {
//        	RequestableStep requestableStep = (RequestableStep) parentDatabaseObject;
//			
//			// Variables
//            vChildDatabaseObjects = requestableStep.getVariables();
//            len = vChildDatabaseObjects.size();
//			if (len != 0) {
//				objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_VARIABLES);
//				parentTreeObject.addChild(objectsFolderTreeObject);
//
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new VariableTreeObject2(viewer, childDatabaseObject, false);
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                }
//			}
//		}
//        else if (parentDatabaseObject instanceof ScreenClass) {
//            ScreenClass screenClass = (ScreenClass) parentDatabaseObject;
//            
//            // Block factory
//            if (screenClass instanceof JavelinScreenClass) {
//                childDBO = ((JavelinScreenClass) screenClass).getBlockFactory();
//                childTreeObject = new DatabaseObjectTreeObject(viewer, childDBO, (parentDatabaseObject != childDBO.getParent()));
//                parentTreeObject.addChild(childTreeObject);
//                monitor.worked(1);
//            }
//            
//            // Criterias
//            vChildDatabaseObjects = screenClass.getCriterias();
//            len = vChildDatabaseObjects.size();
//            if (len != 0) {
//                objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_CRITERIAS);
//                parentTreeObject.addChild(objectsFolderTreeObject);
//                
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new CriteriaTreeObject(viewer, childDatabaseObject, (parentDatabaseObject != childDatabaseObject.getParent()));
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                }
//            }
//            
//            // Extraction rules
//            vChildDatabaseObjects = screenClass.getExtractionRules();
//            len = vChildDatabaseObjects.size();
//            if (len != 0) {
//                objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_EXTRACTION_RULES);
//                parentTreeObject.addChild(objectsFolderTreeObject);
//                
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new ExtractionRuleTreeObject(viewer, childDatabaseObject, (parentDatabaseObject != childDatabaseObject.getParent()));
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                    loadDatabaseObject(childTreeObject, childDatabaseObject);
//                }
//            }
//            
//            // Sheets
//            vChildDatabaseObjects = screenClass.getSheets();
//            
//            len = vChildDatabaseObjects.size();
//            if (len != 0) {
//                objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_SHEETS);
//                parentTreeObject.addChild(objectsFolderTreeObject);
//                
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new SheetTreeObject(viewer, childDatabaseObject, (parentDatabaseObject != childDatabaseObject.getParent()));
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                    //addTemplates((Sheet)childDatabaseObject, childTreeObject);
//                    loadDatabaseObject(childTreeObject, childDatabaseObject);
//                }
//            }
//            
//            // Inherited screen classes
//            vChildDatabaseObjects = screenClass.getInheritedScreenClasses();
//            
//            len = vChildDatabaseObjects.size();
//            if (len != 0) {
//                objectsFolderTreeObject = new ObjectsFolderTreeObject(viewer, ObjectsFolderTreeObject.FOLDER_TYPE_INHERITED_SCREEN_CLASSES);
//                parentTreeObject.addChild(objectsFolderTreeObject);
//                
//				for (DatabaseObject childDatabaseObject : vChildDatabaseObjects) {
//                    childTreeObject = new ScreenClassTreeObject(viewer, childDatabaseObject, (parentDatabaseObject != childDatabaseObject.getParent()));
//                    objectsFolderTreeObject.addChild(childTreeObject);
//                    monitor.worked(1);
//                    loadDatabaseObject(childTreeObject, childDatabaseObject);
//                }
//            }
//        }
//        else if (parentDatabaseObject instanceof Sheet) {
//        	addTemplates((Sheet)parentDatabaseObject, (DatabaseObjectTreeObject)parentTreeObject);
//        }
//        else if (parentDatabaseObject instanceof ITablesProperty) {
//    		String[] tablePropertyNames = ((ITablesProperty)parentDatabaseObject).getTablePropertyNames();
//    		for (int i=0; i<tablePropertyNames.length; i++) {
//    			String tablePropertyName = tablePropertyNames[i];
//    			String tableRenderer = ((ITablesProperty)parentDatabaseObject).getTableRenderer(tablePropertyName);
//    			XMLVector xmlv = ((ITablesProperty)parentDatabaseObject).getTableData(tablePropertyName);
//    			if (tableRenderer.equals("XMLTableDescriptionTreeObject")) {
//        			XMLTableDescriptionTreeObject propertyXMLTableTreeObject = new XMLTableDescriptionTreeObject(viewer,tablePropertyName,xmlv,(DatabaseObjectTreeObject)parentTreeObject);
//        			parentTreeObject.addChild(propertyXMLTableTreeObject);
//    			}
//    			else if (tableRenderer.equals("XMLRecordDescriptionTreeObject")) {
//    				XMLRecordDescriptionTreeObject propertyXMLRecordTreeObject = new XMLRecordDescriptionTreeObject(viewer,tablePropertyName,xmlv,(DatabaseObjectTreeObject)parentTreeObject);
//        			parentTreeObject.addChild(propertyXMLRecordTreeObject);
//    			}
//    		}
//        }
	}

	public void databaseObjectLoaded(DatabaseObjectLoadedEvent event) {
		monitor.subTask("Object \"" + ((DatabaseObject) event.getSource()).getName() + "\" loaded");
		monitor.worked(1);
	}
	
	public void databaseObjectImported(DatabaseObjectImportedEvent event) {
		
	}
	
//	private void addTemplates(Sheet sheet, DatabaseObjectTreeObject treeObject)
//	{
//		TemplateTreeObject templateTreeObject;
//		
//        String xslFileName = sheet.getUrl();
//        
//		try {
//			// Refresh project resource
//			if(project==null) project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName, monitor);
//			
//			IFile file = project.getFile(new Path(xslFileName));
//			Document doc = parseXslFile(file);
//			
//			NodeList nl = doc.getElementsByTagName("xsl:include");
//			for (int i=0; i< nl.getLength(); i++) {
//				Node node = nl.item(i);
//				NamedNodeMap attributes = node.getAttributes();
//				Node 	href    = attributes.getNamedItem("href");
//				String	name    = href.getNodeValue();
//				// do not add includes statring by ../ as thre are system includes
//				if (!name.startsWith("../")) {
//	                templateTreeObject = new TemplateTreeObject(viewer, "["+name.substring(name.lastIndexOf('/')+1, name.lastIndexOf('.')) + "]", name);
//	                treeObject.addChild(templateTreeObject);
//				}
//			}
//		}	
//		catch (CoreException e) {
//			ConvertigoPlugin.logInfo("Error opening Ressources for project '" + sheet.getProject().getName() + "'");
//		}
//		catch (Exception ee) {
//			ConvertigoPlugin.logInfo("Error Parsing XSL file '" + xslFileName + "'");
//		}
//	}
	
	/**
	 * Parses as a DOM the IFile passed in argument ..
	 * 
	 * @param 	file to parse
	 * @return 	parsed Document
	 */
//	private Document parseXslFile(IFile file) throws Exception
//	{
//		Document doc;
//		doc = XMLUtils.documentBuilderDefault.parse(new InputSource(file.getContents()));
//		return doc;
//	}

	public void setDefaultConnectorTreeObject(ConnectorTreeObject defaultConnectorTreeObject) {
		this.defaultConnectorTreeObject = defaultConnectorTreeObject;
	}

	public IProgressMonitor getMonitor() {
		return monitor;
	}
}
