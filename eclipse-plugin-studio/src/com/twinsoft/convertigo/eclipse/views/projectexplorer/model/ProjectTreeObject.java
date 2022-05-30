/*
 * Copyright (c) 2001-2022 Convertigo SA.
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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.twinsoft.convertigo.beans.connectors.CouchDbConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.couchdb.JsonIndex;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.ButtonSpec;
import com.twinsoft.convertigo.eclipse.dialogs.CustomDialog;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JScriptEditorInput;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditor;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditorInput;
import com.twinsoft.convertigo.eclipse.editors.text.TraceFileEditorInput;
import com.twinsoft.convertigo.eclipse.property_editors.validators.NamespaceUriValidator;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineEvent;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.ReadmeBuilder;
import com.twinsoft.convertigo.engine.ReadmeBuilder.MarkdownType;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;


public class ProjectTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject, IResourceChangeListener {
	private IProject iProject = null;
	private boolean isCheckMissingProjects = false;

	public ProjectTreeObject(Viewer viewer, Project object) {
		this(viewer, object, false);
	}

	public ProjectTreeObject(Viewer viewer, Project object, boolean inherited) {
		super(viewer, object, inherited);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	public Project getObject() {
		return (Project) super.getObject();
	}

	public boolean getModified() {
		return hasChanged();
	}

	@Override
	public void update() {
		// does nothing
	}

	@Override
	public DatabaseObjectTreeObject getOwnerDatabaseObjectTreeObject() {
		return this;
	}
	
	@Override
	public void hasBeenModified(boolean bModified) {
		if (bModified && !isInherited) {
			markAsChanged(true);
		}
	}
	
	public boolean close() {
		// close opened editors
		closeAllEditors();
		
		// save project and copy temporary files to project files
		save(true);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

		// clear Source picker view if needed
		clearSourcePickerView();
		
		Engine.execute(() -> Engine.theApp.databaseObjectsManager.clearCache(getObject()));
		return true;
	}

	private void clearSourcePickerView() {
		try {
			IWorkbenchPage activePage = PlatformUI
					.getWorkbench()
					.getActiveWorkbenchWindow()
					.getActivePage();
			
			if (activePage != null) {
				IViewPart viewPart = activePage.findView("com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView");
				if (viewPart != null) {
					SourcePickerView spv = (SourcePickerView)viewPart;
					DatabaseObject dbo = (DatabaseObject)spv.getObject();
					boolean bClose = true;
					try {
						bClose = dbo.getProject().equals(getObject());
					} catch (Exception e) {}
					if (bClose) {
						spv.close();
					}
				}
			}
		}
		catch (Exception e) {}
	}

	@Override
	protected void rename_(String newName, boolean bDialog) throws ConvertigoException, CoreException {
		Project project = getObject();
		String oldName = project.getName();
		
		// First verify if an object with the same name exists
		if (Engine.theApp.databaseObjectsManager.existsProject(newName)) {
			throw new ConvertigoException("The project \"" + newName + "\" already exist!");
		}
		
		// save only objects which have changed
		save(bDialog);
		
		Engine.theApp.databaseObjectsManager.renameProject(project, newName, true);
		
		// delete old resources plugin
		ConvertigoPlugin.getDefault().deleteProjectPluginResource(oldName);
		// create new resources plugin
		ConvertigoPlugin.getDefault().createProjectPluginResource(newName, project.getDirPath());
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject#rename(java.lang.String, java.lang.Boolean)
	 */
	@Override
	public boolean rename(String newName, boolean bDialog) {
		closeAllEditors();
		MobileBuilder.releaseBuilder(getObject());
		boolean renamed = super.rename(newName, bDialog);
		if (!renamed) {MobileBuilder.initBuilder(getObject());}
		return renamed;
	}

	public void generateReadme() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
		
		Shell shell = display.getActiveShell();
		if (shell != null) {
			shell.setCursor(waitCursor);
			
			try {
				Project project = getObject();
				String projectName = project.getName();
				
				if (hasChanged() && !save(true)) {
					return;
				}
				
				int response = SWT.YES;
				
				File mdFile = new File(project.getDirPath(),"readme.md");
				if (mdFile.exists()) {
					MessageBox messageBox = new MessageBox(shell,SWT.YES | SWT.NO | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
					messageBox.setMessage("The project already has a \"readme.md\" file.\nDo you want to overwrite it now?");
					response = messageBox.open();
				}
				
				if (response == SWT.YES) {
					ConvertigoPlugin.logInfo("Generating readme.md file for project \""+ projectName +"\"");
					ReadmeBuilder.process(project, MarkdownType.Readme);
					ConvertigoPlugin.logInfo("Project readme.md file updated");
					
					IProject iProject = getIProject();
					iProject.refreshLocal(IResource.DEPTH_ONE, null);
				}
				
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to generate the readme.md file for project!");
				ConvertigoPlugin.logInfo("Project readme.md NOT generated!");
			} finally {
				shell.setCursor(null);
				waitCursor.dispose();
			}
		}
	}
	
	/**
	 * Saves a project.
	 *
	 * @return <code>false</code> if the save process has been canceled by user.
	 */
	public boolean save(boolean bDialog) {
		boolean ret = false;
		
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);
		
		Shell shell = display.getActiveShell();
		if (shell != null) {
			shell.setCursor(waitCursor);
			
			try {
				if (hasChanged()) {
					Project project = getObject();
					String projectName = project.getName();
					
					int response = SWT.YES;
					if (bDialog) {
						MessageBox messageBox = new MessageBox(shell,SWT.YES | SWT.NO | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
						messageBox.setMessage("The project \"" + projectName + "\" has not been saved. Do you want to save your work now?");
						response = messageBox.open();
					}
					
					if (response == SWT.YES) {
						ConvertigoPlugin.logInfo("Saving the project '" + projectName + "'");

						Engine.theApp.databaseObjectsManager.exportProject(project);
						
						hasBeenModified(false);
						ConvertigoPlugin.logInfo("Project '" + projectName + "' saved!");
						
						IProject iProject = getIProject();
						iProject.refreshLocal(IResource.DEPTH_ONE, null);
						iProject.getFolder("_c8oProject").refreshLocal(IResource.DEPTH_INFINITE, null);
						
						// generate project.md file if needed
						ReadmeBuilder.process(project, MarkdownType.Project);
						ConvertigoPlugin.logInfo("For project '" + projectName + " : project.md file updated");
						
						ret = true;
					}
				}
			} catch (Exception e) {
				ConvertigoPlugin.logException(e, "Unable to save the project!");
				ConvertigoPlugin.logInfo("Project NOT saved!");
			} finally {
				shell.setCursor(null);
				waitCursor.dispose();
			}
		}
		
		return ret;
	}

	public void resourceChanged(IResourceChangeEvent event) {
		//we are only interested in POST_CHANGE events
		if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
			return;
		}
		
//		final ProjectTreeObject projectTreeObject = this;
		final String projectName = getName();
		IPath path = new Path(projectName);
		IResourceDelta projectDelta = event.getDelta().findMember(path);
		if (projectDelta == null) {
			return;
		}
		
		IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
			public boolean visit(IResourceDelta delta) {
				//only interested in changed resources (not added or removed)
				if (delta.getKind() != IResourceDelta.CHANGED) {
					return true;
				}
				//only interested in content changes
				if ((delta.getFlags() & IResourceDelta.CONTENT) == 0) {
					return true;
				}
				
				IResource resource = delta.getResource();
				
				//only interested in files
				if (resource.getType() == IResource.FILE) {
					if ("xsd".equalsIgnoreCase(resource.getFileExtension())) {
//						Project project = getObject();
//						project.setXsdDirty(true);
//						TreeObjectEvent treeObjectEvent1 = new TreeObjectEvent(projectTreeObject, "schemaType", null, null, 0);
//						ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent1);
//						TreeObjectEvent treeObjectEvent2 = new TreeObjectEvent(projectTreeObject, "xsdFile", null, null, 0);
//						ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent2);
					}
				}
				return true;
			}
		};
		try {
			projectDelta.accept(visitor);
		} catch (CoreException e) {
			
		}
	}

	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject) treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject) treeObject.getObject();
			
			if (databaseObject instanceof CouchDbConnector) {
				CouchDbConnector couchDbConnector = (CouchDbConnector) databaseObject;
				if (couchDbConnector.bNew) {
					Engine.execute(() -> {
						CouchDbManager.syncDocument(couchDbConnector);
					});
				}
			}
			
			if (databaseObject instanceof JsonIndex) {
				JsonIndex jsonIndex = (JsonIndex) databaseObject;
				if (jsonIndex.bNew && jsonIndex.getConnector() != null) {
					Engine.execute(() -> {
						CouchDbManager.syncDocument(jsonIndex.getConnector());
					});
				}
			}
			
			if (this.equals(treeObject.getProjectTreeObject())) {
				checkMissingProjects();
				
				Engine.theApp.schemaManager.clearCache(getName());
			}
		}
	}
	
	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject) treeObjectEvent.getSource();
		if (!(treeObject.equals(this)) && (treeObject.getParents().contains(this))) {
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
				String dboName = databaseObject.getName();
				
				// Case of a connector deletion
				if (databaseObject instanceof Connector) {
					// connector in this project
					if (treeObject.getProjectTreeObject().equals(this)) {
						String path = Project.XSD_FOLDER_NAME +"/"
									+ Project.XSD_INTERNAL_FOLDER_NAME;
						
						IFolder parentFolder = getProjectTreeObject().getFolder(path);
						IFolder folder = parentFolder.getFolder(dboName);
						if (folder.exists()) {
							IPath folderPath = folder.getLocation().makeAbsolute();
							try {
								// delete folder
								folder.delete(true, null);
								
								// refresh folder
								parentFolder.refreshLocal(IResource.DEPTH_ONE, null);
							} catch (Exception e) {
								ConvertigoPlugin.logWarning(e, "Could not delete folder \""+ folderPath +"\"!");
							}
						}
					}
				}
				// Case of a transaction deletion
				else if (databaseObject instanceof Transaction) {
					// transaction in this project
					if (treeObject.getProjectTreeObject().equals(this)) {
						String path = Project.XSD_FOLDER_NAME +"/"
									+ Project.XSD_INTERNAL_FOLDER_NAME;
						
						ConnectorTreeObject cto = ((TransactionTreeObject)treeObject).getConnectorTreeObject();
						IFolder parentFolder = getProjectTreeObject().getFolder(path).getFolder(cto.getName());
						IFile file = parentFolder.getFile(dboName+".xsd");
						if (file.exists()) {
							IPath filePath = file.getLocation().makeAbsolute();
							try {
								// delete file
								file.delete(true, null);
								
								// refresh folder
								parentFolder.refreshLocal(IResource.DEPTH_ONE, null);
							} catch (Exception e) {
								ConvertigoPlugin.logWarning(e, "Could not delete file \""+ filePath +"\"!");
							}
						}
					}
				}
				else if (databaseObject instanceof ProjectSchemaReference) {
					checkMissingProjects(false);
				}
				
				// Clear schema cache
				if (this.equals(treeObject.getProjectTreeObject())) {
					Engine.theApp.schemaManager.clearCache(getName());
				}
			}
		}
	}

	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			String propertyName = treeObjectEvent.propertyName;
			propertyName = ((propertyName == null) ? "":propertyName);
			
			// Case of a bean name has changed
			if (propertyName.equals("name")) {
				handlesBeanNameChanged(treeObjectEvent);
			}
			// Case wsdlStyle of this project has changed
			else if (propertyName.equals("wsdlStyle")) {
				if (databaseObject.equals(getObject())) {
					//TODO: remove this property
				}
			}
			// Case schemaElementForm of this project has changed
			else if (propertyName.equals("schemaElementForm")) {
				if (databaseObject.equals(getObject())) {
					updateTransactionSchemas();
				}
			}
			// Case namespaceUri of a project has changed
			else if (propertyName.equals("namespaceUri")) {
				if (databaseObject.equals(getObject())) {
					updateTransactionSchemas();
				}
			}
			// Case of a requestable changed its WS exposition
			else if (propertyName.equals("accessibility")) {
				// Nothing to do
			}
			
			if (this.equals(treeObject.getProjectTreeObject())) {
				if ((databaseObject instanceof RequestableStep && propertyName.startsWith("source"))
						|| (databaseObject instanceof ProjectSchemaReference && propertyName.equals("projectName"))
						|| (treeObject instanceof INamedSourceSelectorTreeObject && 
								((INamedSourceSelectorTreeObject)treeObject).getNamedSourceSelector().isNamedSource(propertyName))
				) {
					checkMissingProjects();
				}
			}
			
			// Clear schema cache
			if (this.equals(treeObject.getProjectTreeObject())) {
				Engine.theApp.schemaManager.clearCache(getName());
			}	
		}
	}
	
	private void updateTransactionSchemas() {
		for (Connector connector: getObject().getConnectorsList()) {
			for (Transaction transaction: connector.getTransactionsList()) {
				synchronized (transaction) {
					transaction.updateSchemaToFile();
				}
			}
		}
		Engine.theApp.schemaManager.clearCache(getName());
	}
	
	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		//Object oldValue = treeObjectEvent.oldValue;
		//Object newValue = treeObjectEvent.newValue;
		//int update = treeObjectEvent.update;
		
		if (!databaseObject.getProject().equals(getObject())) {
			Engine.theApp.schemaManager.clearCache(getName());
		}
		
	}
	
	public void launchEditor(String editorType) {
		if (editorType == null)
			return;
		
		// Retrieve the project name
		String projectName = getName();
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Open editor
			if (editorType.equals("xsd"))
				openXsdEditor(project);
			else if (editorType.equals("wsdl"))
				openWsdlEditor(project);

		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}
	
	public List<String> getMissingTargetProjectList() {
		List<String> missingList = new ArrayList<String>();
		Project p = getObject();
		for (Sequence s: p.getSequencesList()) {
			checkForImports(missingList, s.getSteps());
		}
		return missingList;
	}

	private void checkForImports(List<String> missingList, List<Step> steps) {
		String targetProjectName;
		for (Step step: steps) {
			if (step instanceof StepWithExpressions) {
				checkForImports(missingList, ((StepWithExpressions)step).getSteps());
			}
			else {
				if (step instanceof TransactionStep) {
					TransactionStep transactionStep = ((TransactionStep)step);
					targetProjectName = transactionStep.getProjectName();
					if (!targetProjectName.equals(getName())) {
						try {
							transactionStep.getSequence().getLoadedProject(targetProjectName);
						} catch (EngineException e) {
							if (!missingList.contains(targetProjectName))
								missingList.add(targetProjectName);
						}
					}
				}
				else if (step instanceof SequenceStep) {
					SequenceStep sequenceStep = ((SequenceStep)step);
					targetProjectName = sequenceStep.getProjectName();
					if (!targetProjectName.equals(getName())) {
						try {
							sequenceStep.getSequence().getLoadedProject(targetProjectName);
						} catch (EngineException e) {
							if (!missingList.contains(targetProjectName))
								missingList.add(targetProjectName);
						}
					}
				}
			}
		}
	}
	
	public void openXsdEditor(IProject project)
	{
	}
	
	public void openWsdlEditor(IProject project)
	{
	}

	public void openConnectorEditors() {
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			List<Connector> vConnectors = getObject().getConnectorsList();
			for (Connector connector : vConnectors) {
				try {
					activePage.openEditor(new ConnectorEditorInput(connector),
							"com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor");
				}
				catch(PartInitException e) {
					ConvertigoPlugin.logException(e, "Error while loading the connector editor '" + connector.getName() + "'");
				}
			}
		}
	}
	
	public void openConnectorEditor(Connector connector) {
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			if (connector != null) {
				try {
					activePage.openEditor(new ConnectorEditorInput(connector),
							"com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor");
				}
				catch(PartInitException e) {
					ConvertigoPlugin.logException(e, "Error while loading the connector editor '" + connector.getName() + "'");
				}
			}
		}
	}
	
	public ConnectorEditor getConnectorEditor(Connector connector) {
		ConnectorEditor connectorEditor = null;
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			if (connector != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ConnectorEditorInput)) {
							if (((ConnectorEditorInput)editorInput).is(connector)) {
								connectorEditor = (ConnectorEditor)editorRef.getEditor(true);
								break;
							}
						}
					}
					catch(PartInitException e) {
						ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return connectorEditor;
	}

	public SequenceEditor getSequenceEditor(Sequence sequence) {
		SequenceEditor sequenceEditor = null;
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			if (sequence != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof SequenceEditorInput)) {
							if (((SequenceEditorInput)editorInput).is(sequence)) {
								sequenceEditor = (SequenceEditor)editorRef.getEditor(true);
								break;
							}
						}
					}
					catch(PartInitException e) {
						ConvertigoPlugin.logException(e, "Error while retrieving the sequence editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return sequenceEditor;
	}

	public void closeAllEditors() {
		Project project = getObject();
		IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage activePage = workbenchWindow != null ? workbenchWindow.getActivePage() : null;
		
		if (activePage != null) {
			IEditorReference[] editorRefs = activePage.getEditorReferences();
			for (int i=0;i<editorRefs.length;i++) {
				IEditorReference editorRef = (IEditorReference)editorRefs[i];
				try {
					IEditorInput editorInput = editorRef.getEditorInput();
					if (editorInput != null) {
						// close connector editor
						if (editorInput instanceof ConnectorEditorInput) {
							if (((ConnectorEditorInput)editorInput).is(project)) {
								closeEditor(activePage, editorRef);
							}
						}
						// close sequence editors
						else if (editorInput instanceof SequenceEditorInput) {
							if (((SequenceEditorInput) editorInput).is(project)) {
								closeEditor(activePage, editorRef);
							}
						}
						// close js editors
						else if (editorInput instanceof JScriptEditorInput) {
							DatabaseObject dbo = ((JScriptEditorInput) editorInput).getJScriptContainer().getDatabaseObject();
							if (dbo != null && project.equals(dbo.getProject())) {
								closeEditor(activePage, editorRef);
							}
						}						
						// close trace editors
						else if (editorInput instanceof TraceFileEditorInput) {
							if (((TraceFileEditorInput)editorInput).getConnector().getProject().equals(project)) {
								closeEditor(activePage, editorRef);
							}
						}
						// close other file editors
						else if (editorInput instanceof FileEditorInput) {
							IPath fullpath = ((FileEditorInput)editorInput).getFile().getFullPath();
							if (fullpath.toString().replaceFirst("/(.*?)/.*", "$1").equals(project.getName())) {
								closeEditor(activePage, editorRef);
							}
						}
						else if (editorInput instanceof com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput) {
							com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput acei = GenericUtils.cast(editorInput);
							if (acei.getApplication().getProject().equals(project)) {
								closeEditor(activePage, editorRef);
							}
						}
						else if (editorInput instanceof com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput) {
							com.twinsoft.convertigo.eclipse.editors.ngx.ApplicationComponentEditorInput acei = GenericUtils.cast(editorInput);
							if (acei.getApplication().getProject().equals(project)) {
								closeEditor(activePage, editorRef);
							}
						}
					}
				}
				catch(PartInitException e) {
					ConvertigoPlugin.logException(e, "Error while retrieving the editor '" + editorRef.getName() + "'");
				}
			}
		}
	}
	
	private boolean closeEditor(IWorkbenchPage activePage, IEditorReference editorRef) {
		if ((activePage != null) && (editorRef != null)) {
			try {
				return activePage.closeEditor(editorRef.getEditor(false),true);
			}
			catch (Exception e) {
				return false;
			}
		}
		return true;
	}
	
	public void closeConnectorEditors() {
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			IEditorReference[] editorRefs = activePage.getEditorReferences();
			for (int i=0;i<editorRefs.length;i++) {
				IEditorReference editorRef = (IEditorReference)editorRefs[i];
				try {
					IEditorInput editorInput = editorRef.getEditorInput();
					if ((editorInput != null) && (editorInput instanceof ConnectorEditorInput)) {
						if (((ConnectorEditorInput)editorInput).is(getObject()))
							activePage.closeEditor(editorRef.getEditor(false),true);
					}
				}
				catch(PartInitException e) {
					ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
				}
			}
		}
	}
	
	public void closeConnectorEditors(Connector connector) {
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			if (connector != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof ConnectorEditorInput)) {
							if (((ConnectorEditorInput)editorInput).is(connector)) {
								activePage.closeEditor(editorRef.getEditor(false),true);
								break;
							}
						}
					}
					catch(PartInitException e) {
						ConvertigoPlugin.logException(e, "Error while retrieving the connector editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
	}

	public void closeSequenceEditors(Sequence sequence) {
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			if (sequence != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof SequenceEditorInput)) {
							if (((SequenceEditorInput)editorInput).is(sequence)) {
								activePage.closeEditor(editorRef.getEditor(false),true);
								break;
							}
						}
					}
					catch(PartInitException e) {
						ConvertigoPlugin.logException(e, "Error while retrieving the sequence editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
	}
	
	public IProject getIProject() {
		if(iProject==null || !(getName()!=iProject.getName())){
			try{
				iProject = ConvertigoPlugin.getDefault().getProjectPluginResource(getName());
			}catch (Exception e) {
				ConvertigoPlugin.logException(e, "Error in getProject");
			}
		}
		return iProject;
	}
	
	public IFile getFile(String name) {
		return getIProject().getFile( name);
	}
	
	public IFolder getFolder(String name) {
		return getIProject().getFolder( name);
	}
	
	@Override
	protected ICellEditorValidator getValidator(String propertyName) {
		if ("namespaceUri".equals(propertyName))
			return new NamespaceUriValidator();
		return super.getValidator(propertyName);
	} 
	
	public void checkMissingProjects() {
		checkMissingProjects(true);
	}
	
	public void checkMissingProjects(final boolean doReload) {
		synchronized (this) {
			if (isCheckMissingProjects) {
				return;
			}
			isCheckMissingProjects = true;	
		}

		final Project project = getObject();
		Job.create("Check missing project for " + project.getName(), (monitor) -> {
			try {
				final Set<String> missingProjects = project.getMissingProjects().keySet();
				final Set<String> missingProjectReferences = project.getMissingProjectReferences().keySet();

				if (!missingProjects.isEmpty() || !missingProjectReferences.isEmpty()) {
					List<String> allProjects = Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false);
					for (Iterator<String> i = missingProjects.iterator() ; i.hasNext(); ) {
						String targetProjectName = i.next();
						if (allProjects.contains(targetProjectName)) {
							Display.getDefault().syncExec(() -> {
								try {
									ProjectExplorerView pev = getProjectExplorerView();
									TreeObject obj = pev.getProjectRootObject(targetProjectName);
									if (obj != null && obj instanceof UnloadedProjectTreeObject) {
										pev.loadProject(((UnloadedProjectTreeObject) obj));
										i.remove();
									}
								} catch (Exception e) {
									Engine.logStudio.warn("Failed to open \"" + targetProjectName + "\"", e);
								}
							});
						}
					}

					Map<String, ProjectUrlParser> refToImport = new HashMap<>();
					for (Reference ref: project.getReferenceList()) {
						if (ref instanceof ProjectSchemaReference) {
							ProjectSchemaReference prjRef = (ProjectSchemaReference) ref;
							if (missingProjects.contains(prjRef.getParser().getProjectName()) && prjRef.getParser().isValid()) {
								refToImport.put(prjRef.getParser().getProjectName(), prjRef.getParser());
							}
						}
					}

					if (!refToImport.isEmpty()) {
						Engine.execute(() -> {
							boolean loaded = false;
							for (ProjectUrlParser parser: refToImport.values()) {
								try {
									loaded |= Engine.theApp.referencedProjectManager.importProject(parser) != null;
								} catch (Exception e) {
									Engine.logStudio.warn("Failed to load '" + parser.getProjectName() + "'", e);
								}
							}
							if (loaded) {
								Engine.theApp.fireMigrationFinished(new EngineEvent(""));
							}
						});
						return;
					}

					String message = "For \"" + project.getName() + "\" project :\n";

					for (String targetProjectName: missingProjects) {
						message += "  > The project \"" + targetProjectName + "\" is missing\n";
					}

					for (String targetProjectName: missingProjectReferences) {
						message += "  > The reference to project \"" + targetProjectName + "\" is missing\n";
					}

					message += "\nPlease create missing reference(s) and import missing project(s), or correct your project.";

					if (!missingProjectReferences.isEmpty()) {
						final String msg = message;
						final String warn = message;
						Display.getDefault().syncExec(() -> {
							CustomDialog customDialog = new CustomDialog(
									null,
									"Project references",
									msg + "\n\nDo you want to automatically add reference objects ?",
									670, 250,
									new ButtonSpec("Always", true),
									new ButtonSpec("Never", false));
							
							String autoCreate = ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_AUTO_CREATE_PROJECT_REFERENCE);
							int response = autoCreate.isEmpty() ? customDialog.open() : (autoCreate.equalsIgnoreCase("true") ? 0 : 1);
							
							ConvertigoPlugin.setProperty(ConvertigoPlugin.PREFERENCE_AUTO_CREATE_PROJECT_REFERENCE, response == 0 ? "true" : "false");
							if (response == 0) {
								for (String targetProjectName: missingProjectReferences) {
									try {
										ProjectSchemaReference reference = new ProjectSchemaReference();
										String projectName = targetProjectName;
										reference.setName(targetProjectName + "_reference");
										projectName = ProjectUrlParser.getUrl(projectName);
										reference.setProjectName(projectName);
										reference.hasChanged = true;
										project.add(reference);
									} catch (Exception e) {
										ConvertigoPlugin.logException(e, "failed to add a reference to '" + targetProjectName + "'");
									}
								}
								
								try {
									if (doReload || autoCreate.isEmpty()) {
										ProjectExplorerView pev = getProjectExplorerView();
										pev.reloadTreeObject(ProjectTreeObject.this);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								hasBeenModified(true);
							} else {
								Engine.logBeans.warn(warn);
							}
						});
					} else if (!missingProjects.isEmpty()) {
						ConvertigoPlugin.warningMessageBox(message);
					}
				}
			} finally {
				isCheckMissingProjects = false;
			}
		}).schedule();
	}

	@Override
	public void closeAllEditors(boolean save) {
		closeAllEditors();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object getAdapter(Class adapter) {
		Object obj = super.getAdapter(adapter);
		
		if (obj == null) {
			obj = getIProject();
			if (obj != null && !adapter.isAssignableFrom(IProject.class)) {
				obj = ((IProject) obj).getAdapter(adapter);
			}
		}
		
		return obj;
	}
}
