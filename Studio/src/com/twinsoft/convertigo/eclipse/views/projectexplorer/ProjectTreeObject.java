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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.StepWithExpressions;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.transactions.XmlHttpTransaction;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.browser.BrowserEditorInput;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditor;
import com.twinsoft.convertigo.eclipse.editors.connector.ConnectorEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStatementEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStepEditorInput;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditor;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditorInput;
import com.twinsoft.convertigo.eclipse.editors.text.TraceFileEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionStepEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xsl.XslFileEditorInput;
import com.twinsoft.convertigo.eclipse.property_editors.validators.NamespaceUriValidator;
import com.twinsoft.convertigo.eclipse.views.sourcepicker.SourcePickerView;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.WSDLUtils.WSDL;
import com.twinsoft.convertigo.engine.util.XSDUtils.XSD;

public class ProjectTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject, IResourceChangeListener {
	private IProject iProject = null;
	private boolean bModified = false;

	public ProjectTreeObject(Viewer viewer, Project object) {
		this(viewer, object, false);
	}

	public ProjectTreeObject(Viewer viewer, Project object, boolean inherited) {
		super(viewer, object, inherited);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	public Project getObject(){
		return (Project) super.getObject();
	}

	public boolean getModified() {
		return bModified;
	}

	@Override
	public void update() {
		// does nothing
	}

	/**
     * Means one or more of the project items has been modified.
     */
	@Override
	public void hasBeenModified(boolean bModified) {
		if (bModified) {
			nModifications++;
		} else {
			nModifications--;
			if (nModifications <= 0) {
				nModifications = 0;
			}
		}
		ConvertigoPlugin.logDebug("Project modified "+ nModifications + " times.");
		this.bModified = (nModifications != 0);
	}

	/**
	 * Closes a project.
	 * 
	 * @return <code>false</code> if the close process has been canceled by user.
	 */
	public boolean close() {
		// save project and copy temporary files to project files
		boolean bRet = save(true);
		if (bRet) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
			// close opened editors
			closeAllEditors();
			// remove temporary files
			removeTempFiles();
			// clear Source picker view if needed
			clearSourcePickerView();
		}
		return bRet;
	}

	private void clearSourcePickerView() {
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

	private void removeTempFiles() {
		String projectName = getName();
		try {
			deleteFile(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.xsd");
			deleteFile(Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.wsdl");
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Unable to remove temporary file for project '"+ projectName +"'", Boolean.FALSE);
		}
	}

	private void deleteFile(String filePath) throws EngineException {
		File file = new File(filePath);
		if (file.exists()) {
			try {
				file.delete();
			} catch (Exception e) {
				throw new EngineException("Unable to delete file '"+ filePath +"'");
			}
		}
	}

	private transient boolean isRenaming = false;

	/* (non-Javadoc)
	 * @see com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject#rename(java.lang.String, java.lang.Boolean)
	 */
	@Override
	public boolean rename(String newName, Boolean bDialog) {
		closeAllEditors();
		isRenaming = true;
		return super.rename(newName, bDialog);
	}

	/**
	 * Saves a project.
	 *
	 * @return <code>false</code> if the save process has been canceled by user.
	 */
	@Override
	public boolean save(boolean bDialog) {
		boolean ret = true;
		
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = display.getActiveShell();
		if (shell != null) {
			shell.setCursor(waitCursor);
			
			try {
				//TODO: saveTreeState();
				
				if (bModified) {
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
						ConvertigoPlugin.projectManager.save(project, true);
						nModifications = 0;
						hasBeenModified(false);
						ConvertigoPlugin.logInfo("Project '" + projectName + "' saved!");
						
						saveFiles(projectName);
						
						ConvertigoPlugin.logInfo("Project's XML automatically built");
						//ConvertigoPlugin.projectManager.exportProject(project, Engine.PROJECTS_DIRECTORY + "/" + projectName + "/" + projectName + ".xml");
						CarUtils.exportProject(project, Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xml");
						getIProject().refreshLocal(IResource.DEPTH_ONE, null);
					} else if (response == SWT.NO) {
						Engine.theApp.databaseObjectsManager.cacheRemoveObjects("/" + projectName);
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
		
		isRenaming = false;
		return ret;
	}

	private void saveFiles(String projectName) {
		try {
			saveXsdFile(projectName);
			ConvertigoPlugin.logInfo("Project xsd file '"+ projectName +".xsd' saved!");
			saveWsdlFile(projectName);
			ConvertigoPlugin.logInfo("Project wsdl file '"+ projectName +".wsdl' saved!");
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Unable to save project files!");
		}
	}

	private void saveXsdFile(String projectName) throws EngineException {
		String tempPath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.xsd";
		String xsdPath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".xsd";
		copyToFile(projectName,tempPath, xsdPath);
	}

	private void saveWsdlFile(String projectName) throws EngineException {
		String tempPath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".temp.wsdl";
		String wsdlPath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + ".wsdl";
		copyToFile(projectName,tempPath, wsdlPath);
	}

	private void copyToFile(String projectName, String sourceFilePath, String targetFilePath) throws EngineException {
		try {
			File sourceFile = new File(sourceFilePath);
			if (sourceFile.exists()) {
				File targetFile = new File(targetFilePath);
				if (targetFile.exists()) {
					String line;
					BufferedReader br = new BufferedReader(new FileReader(sourceFilePath));
					BufferedWriter bw = new BufferedWriter(new FileWriter(targetFilePath));
					while((line = br.readLine()) != null) {
						line = line.replaceAll(projectName + ".temp.xsd", projectName + ".xsd");
						bw.write(line + '\n');
					}
					bw.close();
					br.close();
				} else {
					throw new EngineException("'" + targetFilePath + "' does not exist");
				}
			} else {
				if (!isRenaming) {
					throw new EngineException("'" + sourceFilePath + "' does not exist");
				}
			}
		} catch (Exception e) {
			throw new EngineException("Unable to copy '" + sourceFilePath + "' to '" + targetFilePath + "'", e);
		}
	}

	public void resourceChanged(IResourceChangeEvent event) {
		//we are only interested in POST_CHANGE events
		if (event.getType() != IResourceChangeEvent.POST_CHANGE) {
			return;
		}
		
		final ProjectTreeObject projectTreeObject = this;
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
						Project project = getObject();
						project.setXsdDirty(true);
						TreeObjectEvent treeObjectEvent1 = new TreeObjectEvent(projectTreeObject, "schemaType", null, null, 0);
						ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent1);
						TreeObjectEvent treeObjectEvent2 = new TreeObjectEvent(projectTreeObject, "xsdFile", null, null, 0);
						ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent2);
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

	protected boolean dymamicSchemaUpdate = true;
	
	public void setDynamicSchemaUpdate(boolean update) {
		dymamicSchemaUpdate = update;
	}
	
	public void resetDynamicSchemaUpdate() {
		dymamicSchemaUpdate = true;
	}
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			
			if (dymamicSchemaUpdate) {
				if (databaseObject.getProject().getName().equals(getName())) {
					// A transaction or sequence has been added
					if ((databaseObject instanceof Transaction) || (databaseObject instanceof Sequence)) {
						RequestableObject requestable = (RequestableObject)databaseObject;
						if (requestable.getProject().getName().equals(getName())) {
							try {
								updateWebService(requestable.getParent(), requestable, null, true);
							} catch (Exception e) {
							}
						}
					}
					// A step has been added
					else if (databaseObject instanceof Step) {
						RequestableObject requestable = (RequestableObject)((Step)databaseObject).getSequence();
						if (requestable.getProject().getName().equals(getName())) {
							try {
								String xsdTypes = requestable.generateXsdTypes(null, false);
								updateXSDFile(requestable.getParent(), requestable, xsdTypes, false);
								
								if (databaseObject instanceof TransactionStep) {
									TransactionStep transactionStep = (TransactionStep)databaseObject;
									if (!transactionStep.getProjectName().equals(getName())) {
										addXSDFileImport(transactionStep.getProjectName());
									}
								}
								else if (databaseObject instanceof SequenceStep) {
									SequenceStep sequenceStep = (SequenceStep)databaseObject;
									if (!sequenceStep.getProjectName().equals(getName())) {
										addXSDFileImport(sequenceStep.getProjectName());
									}
								}
							} catch (Exception e) {
							}
						}
					}
					// A requestable's variable has been added
					else if (databaseObject instanceof RequestableVariable) {
						RequestableObject requestable = (RequestableObject)((RequestableVariable)databaseObject).getParent();
						if (requestable.getProject().getName().equals(getName())) {
							try {
								/*String xsdTypes = requestable.generateXsdRequestData();
								updateXSDRequest(requestable.getParent(), requestable, xsdTypes);
								updateWSDLRequest(requestable.getParent(), requestable, xsdTypes);*/
								String xsdTypes = requestable.generateXsdTypes(null, false);
								updateWebService(requestable.getParent(), requestable, xsdTypes, false);
							} catch (Exception e) {
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (!(treeObject.equals(this)) && (treeObject.getParents().contains(this))) {
			if (treeObject instanceof DatabaseObjectTreeObject) {
				DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
				
				// A connector has been removed
				if (databaseObject instanceof Connector) {
					Connector connector = (Connector)databaseObject;
					List<Transaction> v = connector.getTransactionsList();
					for (Transaction transaction : v) {
						transaction.setParent(null);//
						updateWebService(connector, transaction, null, false);
					}
				}				
				//  A transaction or sequence has been removed
				else if ((databaseObject instanceof Transaction) || (databaseObject instanceof Sequence)) {
					DatabaseObject parentDatabaseObject = (DatabaseObject)treeObject.getParent().getParent().getObject();
					updateWebService(parentDatabaseObject, (RequestableObject)databaseObject, null, false);
				}
				// A step has been removed
				else if (databaseObject instanceof Step) {
					RequestableObject requestable = null;
					TreeParent treeParent = treeObject.getParent();
					while (!(treeParent instanceof DatabaseObjectTreeObject))
						treeParent = treeParent.getParent();
					DatabaseObject parentObject = (DatabaseObject)treeParent.getObject();
					if (parentObject instanceof Sequence) requestable = (Sequence)parentObject;
					else requestable = ((Step)parentObject).getSequence();
					if (requestable.getProject().getName().equals(getName())) {
						try {
							String xsdTypes = requestable.generateXsdTypes(null, false);
							updateXSDFileWithNS(requestable.getParent(), requestable, xsdTypes, false);
						} catch (Exception e) {
						}
					}
				}
				// A requestable's variable has been removed
				else if (databaseObject instanceof RequestableVariable) {
					RequestableObject requestable = (RequestableObject)treeObject.getParent().getParent().getObject();
					if (requestable.getProject().getName().equals(getName())) {
						try {
							String xsdTypes = requestable.generateXsdTypes(null, false);
							updateWebService(requestable.getParent(), requestable, xsdTypes, false);
						} catch (Exception e) {
						}
					}
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
					updateWebServiceStyle();
				}
			}
			// Case schemaElementForm of this project has changed
			else if (propertyName.equals("schemaElementForm")) {
				if (databaseObject.equals(getObject())) {
					setXSDDefaultForms();
				}
			}
			// Case namespaceUri of a project has changed
			else if (propertyName.equals("namespaceUri")) {
				// Makes replacements in XSD and WSDL files
				Object oldValue = treeObjectEvent.oldValue;
				Object newValue = treeObjectEvent.newValue;
				if (oldValue.equals("")) oldValue = Project.CONVERTIGO_PROJECTS_NAMESPACEURI + databaseObject.getName();
				if (newValue.equals("")) newValue = Project.CONVERTIGO_PROJECTS_NAMESPACEURI + databaseObject.getName();
				
				makeTargetNamespaceReplacements(true, (String)oldValue, (String)newValue);
				makeTargetNamespaceReplacements(false, (String)oldValue, (String)newValue);
			}
			// Case of a requestable changed its WS exposition
			else if (propertyName.equals("publicMethod")) {
				if (databaseObject.getProject().getName().equals(getName())) {
					if ((databaseObject instanceof Transaction) || (databaseObject instanceof Sequence)) {
						RequestableObject requestable = (RequestableObject)databaseObject;
						if (requestable.isPublicAccessibility()) {
							try {
								String xsdTypes = requestable.generateXsdTypes(null, false);
								updateWebService(requestable.getParent(), requestable, xsdTypes, false);
							} catch (Exception e) {
							}
						}
						else
							updateWSDLFile(requestable.getParent(), requestable);
					}
				}
			}
			else if (propertyName.equals("sourceTransaction")) {
				if (databaseObject instanceof TransactionStep) {
					TransactionStep transactionStep = (TransactionStep)databaseObject;
					if (transactionStep.getProject().getName().equals(getName())) {
						if (!transactionStep.getProjectName().equals(getName())) {
							addXSDFileImport(transactionStep.getProjectName());
						}
					}
				}
			}
			else if (propertyName.equals("sourceSequence")) {
				if (databaseObject instanceof SequenceStep) {
					SequenceStep sequenceStep = (SequenceStep)databaseObject;
					if (sequenceStep.getProject().getName().equals(getName())) {
						if (!sequenceStep.getProjectName().equals(getName())) {
							addXSDFileImport(sequenceStep.getProjectName());
						}
					}
				}
			}
			else if (propertyName.equals("comment")) {
				RequestableObject requestable = null;
				if ((databaseObject instanceof Transaction) || (databaseObject instanceof Sequence)) {
					requestable = (RequestableObject)databaseObject;
				}
				else if (databaseObject instanceof Step) {
					requestable = (RequestableObject)((Step)databaseObject).getSequence();
				}
				else if (databaseObject instanceof RequestableVariable) {
					requestable = (RequestableObject)((RequestableVariable)databaseObject).getParent();
				}
				if (requestable!=null) {
					if (requestable.getProject().getName().equals(getName())) {
						try {
							String xsdTypes = requestable.generateXsdRequestData();
							updateWebService(requestable.getParent(), requestable, xsdTypes, false);
						} catch (Exception e) {
						}
					}
				}
			}
			// Case of a step has changed
			else if (databaseObject instanceof Step) {
				RequestableObject requestable = (RequestableObject)((Step)databaseObject).getSequence();
				if (requestable.getProject().getName().equals(getName())) {
					try {
						String xsdTypes = requestable.generateXsdTypes(null, false);
						updateXSDFile(requestable.getParent(), requestable, xsdTypes, false);
					} catch (Exception e) {
					}
				}
			}
			// Case of a requestable's variable has changed
			else if (databaseObject instanceof RequestableVariable) {
				RequestableObject requestable = (RequestableObject)((RequestableVariable)databaseObject).getParent();
				if (requestable.getProject().getName().equals(getName())) {
					try {
						String xsdTypes = requestable.generateXsdTypes(null, false);
						updateWebService(requestable.getParent(), requestable, xsdTypes, false);
					} catch (Exception e) {
					}
				}
			}
		}
	}
	
	protected void handlesBeanNameChanged(TreeObjectEvent treeObjectEvent) {
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		if (!(treeObject.equals(this))) {
			// Makes replacements in XSD and WSDL files if needed
			if (databaseObject instanceof Project) {
				makeProjectReplacements(true, (String)oldValue, (String)newValue);
			}
			else if (databaseObject instanceof Connector) {
				String projectName = databaseObject.getProject().getName();
				makeConnectorReplacements(true, projectName, (String)oldValue, (String)newValue);
				makeConnectorReplacements(false, projectName, (String)oldValue, (String)newValue);
			}
			else if ((databaseObject instanceof Transaction) || (databaseObject instanceof Sequence)) {
				String projectName = databaseObject.getProject().getName();
				makeRequestableObjectReplacements(true, projectName, (RequestableObject)databaseObject, (String)oldValue, (String)newValue);
				makeRequestableObjectReplacements(false, projectName, (RequestableObject)databaseObject, (String)oldValue, (String)newValue);
			}
			if (databaseObject.getProject().getName().equals(getName())) {
				if (databaseObject instanceof RequestableVariable) {
					RequestableObject requestable = (RequestableObject)((RequestableVariable)databaseObject).getParent();
					try {
						String xsdTypes = requestable.generateXsdTypes(null, false);
						updateWebService(requestable.getParent(), requestable, xsdTypes, false);
					} catch (Exception e) {
					}
				}
			}
		}
	}
	
	private void makeTargetNamespaceReplacements(boolean inXsd, String oldValue, String newValue) {
		String projectName = getName();
		String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + (inXsd ? ".temp.xsd":".temp.wsdl");
		File file = new File(filePath);
		if (file.exists()) {
			try {
				String line;
				StringBuffer sb = new StringBuffer();
				boolean bFound = false;
				
				BufferedReader br = new BufferedReader(new FileReader(filePath));
				while((line = br.readLine()) != null) {
					if (!bFound) bFound = line.indexOf(oldValue) != -1;
					line = line.replaceAll("targetNamespace=\""+oldValue+"\"", "targetNamespace=\""+newValue+"\"");
					line = line.replaceAll("namespace=\""+oldValue+"\"", "namespace=\""+newValue+"\"");
					line = line.replaceAll("_ns=\""+oldValue+"\"", "_ns=\""+newValue+"\"");
					sb.append(line+"\n");
				}
				br.close();
				
				BufferedWriter out= new BufferedWriter(new FileWriter(filePath));
				out.write(sb.toString());
				out.close();
				
				if (bFound && !getModified())
					hasBeenModified(true);
			}
			catch (IOException e) {
				ConvertigoPlugin.logInfo("Error updating "+ (inXsd ? "xsd":"wsdl") +" file for project '" + projectName + "'");
			}
		}
	}
	
	private void makeProjectReplacements(boolean inXsd, String oldValue, String newValue) {
		String projectName = getName();
		String filePath = Engine.PROJECTS_PATH + "/" + projectName + "/" + projectName + (inXsd ? ".temp.xsd":".temp.wsdl");
		File file = new File(filePath);
		if (file.exists()) {
			try {
				String line;
				StringBuffer sb = new StringBuffer();
				boolean bFound = false;
				
				BufferedReader br = new BufferedReader(new FileReader(filePath));
				while((line = br.readLine()) != null) {
					if (!bFound) bFound = line.indexOf(oldValue) != -1;
					line = line.replaceAll("/"+oldValue, "/"+newValue);
					line = line.replaceAll("xmlns:"+oldValue+"_ns=\"", "xmlns:"+newValue+"_ns=\"");
					line = line.replaceAll("=\""+oldValue+"_ns:", "=\""+newValue+"_ns:");
					sb.append(line+"\n");
				}
				br.close();
				
				BufferedWriter out= new BufferedWriter(new FileWriter(filePath));
				out.write(sb.toString());
				out.close();
				
				if (bFound && !getModified())
					hasBeenModified(true);
			}
			catch (IOException e) {
				ConvertigoPlugin.logInfo("Error updating "+ (inXsd ? "xsd":"wsdl") +" file for project '" + projectName + "'");
			}
		}
	}

	private void makeConnectorReplacements(boolean inXsd, String projectName, String oldValue, String newValue) {
		String filePath = Engine.PROJECTS_PATH + "/" + getName() + "/" + getName() + (inXsd ? ".temp.xsd":".temp.wsdl");
		File file = new File(filePath);
		if (file.exists()) {
			try {
				String line;
				StringBuffer sb = new StringBuffer();
				boolean bFound = false;
				
				BufferedReader br = new BufferedReader(new FileReader(filePath));
				while((line = br.readLine()) != null) {
					if (!bFound) bFound = line.indexOf(oldValue) != -1;
					line = line.replaceAll("=\""+oldValue+"__", "=\""+newValue+"__");
					line = line.replaceAll("=\""+projectName+"?"+oldValue+"__", "=\""+projectName+"?"+newValue+"__");
					line = line.replaceAll("=\""+projectName+"_ns:"+oldValue+"__", "=\""+projectName+"_ns:"+newValue+"__");
					sb.append(line+"\n");
				}
				br.close();
				
				BufferedWriter out= new BufferedWriter(new FileWriter(filePath));
				out.write(sb.toString());
				out.close();
				
				if (bFound && !getModified())
					hasBeenModified(true);
			}
			catch (IOException e) {
				ConvertigoPlugin.logInfo("Error updating "+ (inXsd ? "xsd":"wsdl") +" file for project '" + getName() + "'");
			}
		}
	}
	
	private void makeRequestableObjectReplacements(boolean inXsd, String projectName, RequestableObject requestable, String oldValue, String newValue) {
		String filePath = Engine.PROJECTS_PATH + "/" + getName() + "/" + getName() + (inXsd ? ".temp.xsd":".temp.wsdl");
		File file = new File(filePath);
		if (file.exists()) {
			try {
				String line;
				StringBuffer sb = new StringBuffer();
				boolean bFound = false;
				
				BufferedReader br = new BufferedReader(new FileReader(filePath));
				while((line = br.readLine()) != null) {
					String prefix = (requestable instanceof Sequence ? "":((Transaction)requestable).getConnector().getName() + "__");
					if (!bFound) bFound = line.indexOf(prefix + oldValue) != -1;
					line = line.replaceAll("=\""+ prefix + oldValue, "=\""+ prefix + newValue);
					line = line.replaceAll("=\""+projectName+"?"+ prefix + oldValue, "=\""+projectName+"?"+ prefix + newValue);
					line = line.replaceAll("=\""+projectName+"_ns:"+ prefix + oldValue, "=\""+projectName+"_ns:"+ prefix + newValue);
					sb.append(line+"\n");
				}
				br.close();
				
				BufferedWriter out= new BufferedWriter(new FileWriter(filePath));
				out.write(sb.toString());
				out.close();
				
				if (bFound && !getModified())
					hasBeenModified(true);
			}
			catch (IOException e) {
				ConvertigoPlugin.logInfo("Error updating "+ (inXsd ? "xsd":"wsdl") +" file for project '" + getName() + "'");
			}
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
    
	public boolean isXsdValid() throws Exception {
		return ProjectUtils.getXSD(this.getName(), true) != null;
	}
	
	public synchronized void cleanWSFiles() throws EngineException {
		Project p = getObject();
		String projectName = p.getName();
		XSD xsd = null;
		WSDL wsdl = null;
		
		// Check for missing targeted projects (deleted or not already loaded)
		List<String> list = getMissingTargetProjectList();
		if (!list.isEmpty()) {
			String message = "Some target project(s) are missing :\n";
			for (String s: list)
				message += "   > \""+s+"\" project\n";
			message += "Please import missing project(s),\nor correct your sequence(s) before clean.";
			throw new EngineException(message);
		}
		
		// Clean schema (remove unused namespaces/imports, sequence and step schemas)
		try {
			List<String> names = new ArrayList<String>();
			for (Sequence s : p.getSequencesList())
				names.add(s.getName()+"ResponseData");
			
			ProjectUtils.cleanSchema(names, projectName, true);
		}
		catch (Exception e) {
			try {
				ProjectUtils.restoreTempXSD(projectName);
				ConvertigoPlugin.logException(e,"Project's temporary XSD file has been restored because an error occured while cleaning");
			} catch (Exception e1) {
				ConvertigoPlugin.logException(e1,"Error while restoring project's temporary XSD file");
			}
			return;
		}

		// Retrieve XSD file
		try {
			xsd = ProjectUtils.getXSD(projectName, true);
		} catch (Exception e) {
			throw new EngineException("Error while retrieving project's temporary XSD file",e);
		}
		// Retrieve WSDL file
		try {
			wsdl = ProjectUtils.getWSDL(projectName, true);
		} catch (Exception e) {
			throw new EngineException("Error while retrieving project's temporary WSDL file",e);
		}
		
		// Add SoapEnc namespace for Rpc array encoding (multivalued variable) : Fixed #1197
		try {
			ProjectUtils.addWsdlNamespaceDeclaration(wsdl, "soapenc", "http://schemas.xmlsoap.org/soap/encoding/");
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while adding namespace declaration to '" + projectName + "' project WSDL file");
		}
		
		// Regenerate schema of all transactions
		for (Connector c : p.getConnectorsList()) {
			for (Transaction t: c.getTransactionsList()) {
				try {
					// Update xsd and wsdl files
					String xsdTypes = t.generateXsdRequestData();
					if ((t instanceof XmlHttpTransaction) && (!((XmlHttpTransaction)t).getResponseElementQName().equals("")))
						xsdTypes = t.generateXsdTypes(null, false);
	                if ((xsdTypes != null) && (!xsdTypes.equals(""))) {
	                	ProjectUtils.updateWebService(xsd, wsdl, projectName, c, t, xsdTypes, false);
	               		t.hasChanged = true;
	                }
				}
				catch (Exception e) {
					ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project WS files for transaction '"+ t.getName() +"'");
				}
			}
		}
		
		// Regenerate schema of all sequences
		for (Sequence s : p.getSequencesList()) {
			try {
				// Add necessary namespace declarations and imports
				addNamespaceAndImports(xsd, p.getName(), s.getSteps());
				try {
					// Update xsd and wsdl files
					String xsdTypes = s.generateXsdTypes(null, false);
		            if ((xsdTypes != null) && (!xsdTypes.equals(""))) {
		            	ProjectUtils.updateWebService(xsd, wsdl, projectName, p, s, xsdTypes, false);
		            	s.hasChanged = true;
		            }
				}
				catch (Exception e) {
					ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project WS files for sequence '"+ s.getName() +"'");
				}
			}
			catch (Exception e) {
				ConvertigoPlugin.logException(e, "Error while adding schema imports and namespaces for sequence '"+ s.getName() +"' in project '" + projectName + "'");
			}
		}
		
		if (!getModified()) hasBeenModified(true);
		
		p.setXsdDirty(true);
		TreeObjectEvent treeObjectEvent1 = new TreeObjectEvent(this, "schemaType", null, null, 0);
        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent1);
		TreeObjectEvent treeObjectEvent2 = new TreeObjectEvent(this, "xsdFile", null, null, 0);
        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent2);
	}
	
	private void addNamespaceAndImports(XSD xsd, String projectName, List<Step> steps) throws Exception {
		String targetProjectName = projectName;
		for (Step step: steps) {
			if (step instanceof StepWithExpressions) {
				addNamespaceAndImports(xsd, projectName, ((StepWithExpressions)step).getSteps());
			}
			else {
				if (step instanceof TransactionStep)
					targetProjectName = ((TransactionStep)step).getProjectName();
				else if (step instanceof SequenceStep)
					targetProjectName = ((SequenceStep)step).getProjectName();
				
				if (!targetProjectName.equals(projectName))
					ProjectUtils.addXSDFileImport(xsd, projectName, targetProjectName);
			}
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
	
	private synchronized void setXSDDefaultForms() {
		String projectName = getName();
		try {
			Project p = getObject();
			ProjectUtils.setXSDDefaultForms(projectName, p.getSchemaElementForm(), true);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while updating schema forms for project '" + projectName + "'");
		}
	}
	
	private synchronized boolean updateWebServiceStyle() {
		String projectName = getName();
		try {
			Project p = getObject();

			String wsdlStyle = p.getWsdlStyle();
			if (wsdlStyle.equals("")) {
				ConvertigoPlugin.logError("Project WSDL style is not valid: it must not be empty.",Boolean.TRUE);
				return false;
			}
			
			// Create wsdl file for given style (Doc/literal, Rpc or both types)
			if (ProjectUtils.createWsdlFile(Engine.PROJECTS_PATH, projectName, wsdlStyle, true, true)) {
				
				// Load wsdl from file
				WSDL wsdl = ProjectUtils.getWSDL(projectName, true);
				
				// Adds all needed operations
				for (Connector c : p.getConnectorsList()) {
					for (Transaction t: c.getTransactionsList()) {
						try {
							ProjectUtils.updateWSDLFile(wsdl, projectName, c, t);
						}
						catch (Exception e) {
							ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project WSDL style for transaction '"+ t.getName() +"'");
						}
					}
				}
				for (Sequence s : p.getSequencesList()) {
					try {
						ProjectUtils.updateWSDLFile(wsdl, projectName, p, s);
					}
					catch (Exception e) {
						ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project WSDL style for sequence '"+ s.getName() +"'");
					}
				}
				
				if (!getModified()) hasBeenModified(true);
				return true;
			}
			
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while updating web service definitions for project '" + projectName + "'");
		}
		return false;
	}
	
	protected synchronized boolean updateWebService(DatabaseObjectTreeObject targetDboTo) {
		String projectName = getName();
		try {
			ConvertigoPlugin.logDebug("Update web service: start");
			ProjectUtils.updateWebService(projectName, getObject(), targetDboTo.getObject());
			ConvertigoPlugin.logDebug("Update web service: end");
			
			if (!getModified()) hasBeenModified(true);
			
			getObject().setXsdDirty(true);
			TreeObjectEvent treeObjectEvent1 = new TreeObjectEvent(this, "schemaType", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent1);
			TreeObjectEvent treeObjectEvent2 = new TreeObjectEvent(this, "xsdFile", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent2);
	        
	        return true;
			
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project web service");
		}
		return false;
	}

	public synchronized boolean updateWebService(DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add) {
		String projectName = getName();
		try {
			ProjectUtils.updateWebService(projectName, parentOfRequestable, requestable, xsdTypes, add, true);
			
			if (!getModified()) hasBeenModified(true);
			
			getObject().setXsdDirty(true);
			TreeObjectEvent treeObjectEvent1 = new TreeObjectEvent(this, "schemaType", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent1);
			TreeObjectEvent treeObjectEvent2 = new TreeObjectEvent(this, "xsdFile", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent2);
	        
	        return true;
			
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project web service");
		}
		return false;
	}
	
	private synchronized void addXSDFileImport(String targetProjectName) {
		if (targetProjectName == null)
			return;
		if (targetProjectName.equals(""))
			return;
		
		String projectName = getName();
		try {
			ProjectUtils.addXSDFileImport(projectName, targetProjectName, true);
			
			if (!getModified()) hasBeenModified(true);
				
			getObject().setXsdDirty(true);
			TreeObjectEvent treeObjectEvent1 = new TreeObjectEvent(this, "schemaType", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent1);
			TreeObjectEvent treeObjectEvent2 = new TreeObjectEvent(this, "xsdFile", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent2);
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while adding import to xsd file for project '" + projectName + "'");
		}
	}
	
	private synchronized boolean updateXSDFileWithNS(DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add) {
		String projectName = getName();
		try {
			
			ProjectUtils.updateXSDFileWithNS(projectName, parentOfRequestable, requestable, xsdTypes, add, true);
			
			if (!getModified()) hasBeenModified(true);
				
			getObject().setXsdDirty(true);
			TreeObjectEvent treeObjectEvent1 = new TreeObjectEvent(this, "schemaType", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent1);
			TreeObjectEvent treeObjectEvent2 = new TreeObjectEvent(this, "xsdFile", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent2);

    		return true;
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project xsd file for '"+ requestable.getName() +"' requestable");
		}
		return false;
	}
	
	private synchronized boolean updateXSDFile(DatabaseObject parentOfRequestable, RequestableObject requestable, String xsdTypes, boolean add) {
		String projectName = getName();
		try {
			ProjectUtils.updateXSDFile(projectName, parentOfRequestable, requestable, xsdTypes, add, true);
			
			if (!getModified()) hasBeenModified(true);
				
			getObject().setXsdDirty(true);
			TreeObjectEvent treeObjectEvent1 = new TreeObjectEvent(this, "schemaType", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent1);
			TreeObjectEvent treeObjectEvent2 = new TreeObjectEvent(this, "xsdFile", null, null, 0);
	        ConvertigoPlugin.projectManager.getProjectExplorerView().fireTreeObjectPropertyChanged(treeObjectEvent2);

    		return true;
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project xsd file for '"+ requestable.getName() +"' requestable");
		}
		return false;
	}
	
	private synchronized boolean updateWSDLFile(DatabaseObject parentOfRequestable, RequestableObject requestable) {
		String projectName = getName();
		try {
			ProjectUtils.updateWSDLFile(projectName, parentOfRequestable, requestable, true);
			
			if (!getModified()) hasBeenModified(true);
			
			return true;
		} catch (Exception e) {
			ConvertigoPlugin.logException(e, "Error while updating '" + projectName + "' project wsdl file for '"+ requestable.getName() +"' requestable");
		}
		return false;
	}
	
	public void openXsdEditor(IProject project)
	{
		IFile	file = project.getFile("/"+ project.getName()+".temp.xsd");
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException e1) {
		}
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				IDE.openEditor(activePage, file, true);
			}
			catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the xsd editor for '" + file.getName() + "'");
			} 
		}
	}
	
	public void openWsdlEditor(IProject project)
	{
		IFile	file = project.getFile("/" + project.getName()+".temp.wsdl");
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (CoreException e1) {
		}
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				IDE.openEditor(activePage, file, true);
			}
			catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the wsdl editor for '" + file.getName() + "'");
			} 
		}
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
							if (((ConnectorEditorInput)editorInput).connector.equals(connector)) {
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
							if (((SequenceEditorInput)editorInput).sequence.equals(sequence)) {
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
					if (editorInput != null) {
						// close connector editor
						if (editorInput instanceof ConnectorEditorInput) {
							if (((ConnectorEditorInput)editorInput).connector.getParent().equals(project)) {
								closeEditor(activePage, editorRef);
							}
						}
						// close sequence editors
						else if (editorInput instanceof SequenceEditorInput) {
							if (((SequenceEditorInput)editorInput).sequence.getProject().equals(project)) {
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
						// close browser editors
						else if (editorInput instanceof BrowserEditorInput) {
							closeEditor(activePage, editorRef);
						}
						// close other file editors
						else if (editorInput instanceof FileEditorInput) {
							if (((FileEditorInput)editorInput).getFile().getLocation().toString().indexOf(project.getName()) != -1) {
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
						if (((ConnectorEditorInput)editorInput).connector.getParent().equals(getObject()))
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
							if (((ConnectorEditorInput)editorInput).connector.equals(connector)) {
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
							if (((SequenceEditorInput)editorInput).sequence.equals(sequence)) {
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

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isModified")) {
			isModified = bModified || hasChanged();
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isModified));
		}
		return super.testAttribute(target, name, value);
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
	
}
