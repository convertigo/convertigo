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

package com.twinsoft.convertigo.eclipse.views.projectexplorer.model;

import java.util.ArrayList;
import java.util.List;
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
import org.eclipse.jface.operation.ModalContext;
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
import com.twinsoft.convertigo.beans.core.RequestableStep;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStatementEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStepEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.editors.mobile.ApplicationComponentEditorInput;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditor;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditorInput;
import com.twinsoft.convertigo.eclipse.editors.text.TraceFileEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionStepEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xsl.XslFileEditorInput;
import com.twinsoft.convertigo.eclipse.property_editors.validators.NamespaceUriValidator;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView;
import com.twinsoft.convertigo.engine.ConvertigoException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.providers.couchdb.CouchDbManager;


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
	
	/**
	 * Closes a project.
	 * 
	 * @return <code>false</code> if the close process has been canceled by user.
	 */
	public boolean close() {
		// close opened editors
		closeAllEditors();
		
		// save project and copy temporary files to project files
		boolean bRet = save(true);
		if (bRet) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

			// clear Source picker view if needed
			clearSourcePickerView();
			
			Engine.theApp.databaseObjectsManager.clearCache(getObject());
		}
		return bRet;
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
		ConvertigoPlugin.getDefault().createProjectPluginResource(newName);
	}
	
	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject#rename(java.lang.String, java.lang.Boolean)
	 */
	@Override
	public boolean rename(String newName, boolean bDialog) {
		closeAllEditors();
		return super.rename(newName, bDialog);
	}

	/**
	 * Saves a project.
	 *
	 * @return <code>false</code> if the save process has been canceled by user.
	 */
	public boolean save(boolean bDialog) {
		boolean ret = true;
		
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
						
						getIProject().refreshLocal(IResource.DEPTH_ONE, null);
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
			if ((treeObject != this && treeObject instanceof ProjectTreeObject) ||
				(databaseObject instanceof RequestableStep && treeObject.getProjectTreeObject() == this)) {
					checkMissingProjects();
			}
			
			if (databaseObject instanceof CouchDbConnector) {
				CouchDbConnector couchDbConnector = (CouchDbConnector) databaseObject;
				if (couchDbConnector.bNew) {
					CouchDbManager.syncDocument(couchDbConnector);
				}
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
								
								Engine.theApp.schemaManager.clearCache(getName());
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
								
								Engine.theApp.schemaManager.clearCache(getName());
							} catch (Exception e) {
								ConvertigoPlugin.logWarning(e, "Could not delete file \""+ filePath +"\"!");
							}
						}
					}
				}
				else if (databaseObject instanceof ProjectSchemaReference) {
					checkMissingProjects();
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
			
			else if ((databaseObject instanceof RequestableStep && propertyName.startsWith("source"))
					|| (databaseObject instanceof ProjectSchemaReference && propertyName.equals("projectName"))) {
				checkMissingProjects();
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
						// close xsl editors
						else if (editorInput instanceof XslFileEditorInput) {
							if (((XslFileEditorInput)editorInput).getProjectName().equals(project.getName())) {
								closeEditor(activePage, editorRef);
							}
						}
						// close js editors
						else if (editorInput instanceof JscriptTransactionEditorInput) {
							if (((JscriptTransactionEditorInput)editorInput).getTransaction().getProject().equals(project)) {
								closeEditor(activePage, editorRef);
							}
						}
						else if (editorInput instanceof JscriptStatementEditorInput) {
							if (((JscriptStatementEditorInput)editorInput).getStatement().getProject().equals(project)) {
								closeEditor(activePage, editorRef);
							}
						}
						else if (editorInput instanceof JscriptStepEditorInput) {
							if (((JscriptStepEditorInput)editorInput).getStep().getProject().equals(project)) {
								closeEditor(activePage, editorRef);
							}
						}
						// close xml editors
						else if (editorInput instanceof XMLTransactionEditorInput) {
							if (((XMLTransactionEditorInput)editorInput).getTransaction().getProject().equals(project)) {
								closeEditor(activePage, editorRef);
							}
						}						
						else if (editorInput instanceof XMLTransactionStepEditorInput) {
							if (((XMLTransactionStepEditorInput)editorInput).getTransactionStep().getProject().equals(project)) {
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
						else if (editorInput instanceof ApplicationComponentEditorInput) {
							if (((ApplicationComponentEditorInput)editorInput).getApplication().getProject().equals(project)) {
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
		if (isCheckMissingProjects) {
			return;
		}
		
		final Project project = getObject();
		
		final Set<String> missingProjects = project.getMissingProjects();
		final Set<String> missingProjectReferences = project.getMissingProjectReferences();

		if (!missingProjects.isEmpty() || !missingProjectReferences.isEmpty()) {
			isCheckMissingProjects = true;
			
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					try {
						int level = ModalContext.getModalLevel();
						if (level > 0) {
							// prevents double modal windows: dead lock on linux/gtk studio
							Display.getDefault().asyncExec(this);
							return;
						} 

						String message = "For \"" + project.getName() + "\" project :\n";

						for (String targetProjectName: missingProjects) {
							message += "  > The project \"" + targetProjectName + "\" is missing\n";
						}

						for (String targetProjectName: missingProjectReferences) {
							message += "  > The reference to project \"" + targetProjectName + "\" is missing\n";
						}

						message += "\nPlease create missing reference(s) and import missing project(s),\nor correct your sequence(s).";

						if (!missingProjectReferences.isEmpty()) {
							int response = ConvertigoPlugin.questionMessageBox(null, message + "\n\nDo you want to automatically add reference objects ?");
							if (response == SWT.YES) {
								for (String targetProjectName: missingProjectReferences) {
									try {
										ProjectSchemaReference reference = new ProjectSchemaReference();
										reference.setProjectName(targetProjectName);
										reference.setName(targetProjectName + "_reference");
										project.add(reference);

										ProjectExplorerView explorerView = ConvertigoPlugin.projectManager.getProjectExplorerView();
										explorerView.reloadTreeObject(ProjectTreeObject.this);
									} catch (Exception e) {
										ConvertigoPlugin.logException(e, "failed to add a reference to '" + targetProjectName + "'");
									}
								}
								hasBeenModified(true);

							}
						} else {
							ConvertigoPlugin.warningMessageBox(message);
						}
					} finally {
						isCheckMissingProjects = false;
					}
				}

			});
		}
	}
}
