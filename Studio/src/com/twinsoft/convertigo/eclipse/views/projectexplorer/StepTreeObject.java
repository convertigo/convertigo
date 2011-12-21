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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.IStepSourceContainer;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.steps.IThenElseContainer;
import com.twinsoft.convertigo.beans.steps.IfExistStep;
import com.twinsoft.convertigo.beans.steps.IfExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.IfFileExistStep;
import com.twinsoft.convertigo.beans.steps.IfFileExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.IfStep;
import com.twinsoft.convertigo.beans.steps.IfThenElseStep;
import com.twinsoft.convertigo.beans.steps.InputVariablesStep;
import com.twinsoft.convertigo.beans.steps.IsInStep;
import com.twinsoft.convertigo.beans.steps.IsInThenElseStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.variables.RequestableVariable;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStepEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLSequenceStepEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionStepEditorInput;

public class StepTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject, IOrderableTreeObject {
	
	public StepTreeObject(Viewer viewer, Step object) {
		this(viewer, object, false);
	}
	
	public StepTreeObject(Viewer viewer, Step object, boolean inherited) {
		super(viewer, object, inherited);
		setEnabled(getObject().isEnable());
	}
	
	@Override
	public Step getObject(){
		return (Step) super.getObject();
	}

	@Override
    public boolean isEnabled() {
		setEnabled(getObject().isEnable());
    	return super.isEnabled();
    }

	@Override
	public void hasBeenModified(boolean modified) {
		super.hasBeenModified(modified);
		if (modified)
			getObject().setWsdlDomDirty();
	}
	
	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			String propertyName = treeObjectEvent.propertyName;
			propertyName = ((propertyName == null) ? "":propertyName);
			
			// If a bean name has changed
			if (propertyName.equals("name")) {
				handlesBeanNameChanged(treeObjectEvent);
			}
			
			if (databaseObject instanceof Project) {
				if (propertyName.equals("xsdFile")) {
					if (getObject() instanceof SequenceStep) {
						SequenceStep sequenceStep = (SequenceStep)getObject();
						if (sequenceStep.getProjectName().equals(databaseObject.getName())) {
							sequenceStep.setWsdlDomDirty();
						}
					}
					else if (getObject() instanceof TransactionStep) {
						TransactionStep transactionStep = (TransactionStep)getObject();
						if (transactionStep.getProjectName().equals(databaseObject.getName())) {
							transactionStep.setWsdlDomDirty();
						}
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
		int update = treeObjectEvent.update;
		
		// Updates project, connector and transaction names references
		if (update != TreeObjectEvent.UPDATE_NONE) {
			boolean isLocalProject = false;
			boolean isSameValue = false;
			boolean shouldUpdate = false;
			
			if (getObject() instanceof TransactionStep) {
				TransactionStep step = (TransactionStep)getObject();
				
				// Case of project rename
				if (databaseObject instanceof Project) {
					isLocalProject = step.getProject().equals(databaseObject);
					isSameValue = step.getProjectName().equals(oldValue);
					shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
					if (isSameValue && shouldUpdate) {
						step.setSourceTransaction((String)newValue + TransactionStep.SOURCE_SEPARATOR + step.getConnectorName() +
								TransactionStep.SOURCE_SEPARATOR + step.getTransactionName());
						hasBeenModified(true);
						viewer.refresh();
						
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
				// Case of bean rename in same targeted project
				else if (databaseObject.getProject().getName().equals(step.getProjectName())) {
					isLocalProject = step.getProject().equals(databaseObject.getProject());
					shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
					
					// Case of connector rename
					if (databaseObject instanceof Connector) {
						isSameValue = step.getConnectorName().equals(oldValue);
						if (isSameValue && shouldUpdate) {
							step.setSourceTransaction(step.getProjectName() + TransactionStep.SOURCE_SEPARATOR + (String) newValue +
									TransactionStep.SOURCE_SEPARATOR + step.getTransactionName());
							hasBeenModified(true);
							viewer.refresh();
							
							getDescriptors();// refresh editors (e.g labels in combobox)
						}
					}
					// Case of transaction rename
					else if (databaseObject instanceof Transaction) {
						isSameValue = step.getTransactionName().equals(oldValue);
						if (isSameValue && shouldUpdate) {
							step.setSourceTransaction(step.getProjectName() + TransactionStep.SOURCE_SEPARATOR + step.getConnectorName() +
									TransactionStep.SOURCE_SEPARATOR + (String) newValue);
							hasBeenModified(true);
							viewer.refresh();
							
							getDescriptors();// refresh editors (e.g labels in combobox)
						}
					}
				}
			}
			
			if (getObject() instanceof SequenceStep) {
				SequenceStep step = (SequenceStep)getObject();
				
				// Case of project rename
				if (databaseObject instanceof Project) {
					isLocalProject = step.getProject().equals(databaseObject);
					isSameValue = step.getProjectName().equals(oldValue);
					shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
					if (isSameValue && shouldUpdate) {
						step.setSourceSequence((String) newValue + SequenceStep.SOURCE_SEPARATOR + step.getSequenceName());
						hasBeenModified(true);
						viewer.refresh();
						
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
				// Case of bean rename in same targeted project
				else if (databaseObject.getProject().getName().equals(step.getProjectName())) {
					isLocalProject = step.getProject().equals(databaseObject.getProject());
					shouldUpdate = (update == TreeObjectEvent.UPDATE_ALL) || ((update == TreeObjectEvent.UPDATE_LOCAL) && (isLocalProject));
					
					// Case of sequence rename
					if (databaseObject instanceof Sequence) {
						isSameValue = step.getSequenceName().equals(oldValue);
						if (isSameValue && shouldUpdate) {
							step.setSourceSequence(step.getProjectName() + SequenceStep.SOURCE_SEPARATOR + (String) newValue);
							hasBeenModified(true);
							viewer.refresh();
							
							getDescriptors();// refresh editors (e.g labels in combobox)
						}
					}
				}
			}
		}
		
		if (getObject() instanceof InputVariablesStep) {
			// Case of variable rename
			if (databaseObject instanceof RequestableVariable) {
				InputVariablesStep ivs = (InputVariablesStep)getObject();
				if (ivs.getSequence().equals(databaseObject.getParent())) {
					ivs.setWsdlDomDirty(); // set dirty flag in order to regenerate dom
				}
			}
		}
		
	}
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Object object = getObject();
		
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			
			// A requestable variable has been added
			if (databaseObject instanceof RequestableVariable) {
				// Case this is an InputVariableStep
				if (object instanceof InputVariablesStep) {
					InputVariablesStep ivs = (InputVariablesStep)getObject();
					if (ivs.getSequence().equals(databaseObject.getParent())) {
						ivs.setWsdlDomDirty(); // set dirty flag in order to regenerate dom
					}
				}
			}
		}
	}

	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		Object object = getObject();
		
		// Case this is a transaction step
		if (object instanceof TransactionStep) {
			if ((treeObject instanceof ProjectTreeObject) ||
				(treeObject instanceof UnloadedProjectTreeObject) ||
				(treeObject instanceof ConnectorTreeObject) ||
				(treeObject instanceof TransactionTreeObject)) {
			
		    	// Refresh label in case of broken properties
		    	try {
		    		ConvertigoPlugin.getDefault().getProjectExplorerView().updateTreeObject(this);
		    		
				} catch (Exception e) {
					ConvertigoPlugin.logWarning(e, "Could not update Transaction step\""+ getName()+"\" !");
				}
			}
		}

		// Case this is a sequence step
		if (object instanceof SequenceStep) {
			if ((treeObject instanceof ProjectTreeObject) ||
				(treeObject instanceof UnloadedProjectTreeObject) ||
				(treeObject instanceof SequenceTreeObject)) {
			
		    	// Refresh label in case of broken properties
		    	try {
		    		ConvertigoPlugin.getDefault().getProjectExplorerView().updateTreeObject(this);
		    		
				} catch (Exception e) {
					ConvertigoPlugin.logWarning(e, "Could not update Sequence step \""+ getName()+"\" !");
				}
			}
		}
		
		// Case this is an InputVariables step
		if (object instanceof InputVariablesStep) {
			InputVariablesStep ivs = (InputVariablesStep)object;
			if (treeObject.getObject() instanceof RequestableVariable) {
				try {
					DatabaseObject dbo = (DatabaseObject)treeObject.getParent().getParent().getObject();
					if (dbo.equals(ivs.getSequence())) {
						ivs.setWsdlDomDirty(); // set dirty flag in order to regenerate dom
					}
				} catch (Exception e) {
					ConvertigoPlugin.logWarning(e, "Could not notify Sequence step \""+ getName()+"\" !");
				}
			}
		}
	}
	
	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = ((DatabaseObject)getObject()).getProject().getName();
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Get editor type
			if (editorType == null) {
				if (((DatabaseObject)getObject()) instanceof SimpleStep)
					editorType = "JscriptStepEditor";
			}
				
			// Open editor
			if ((editorType != null) && (editorType.equals("JscriptStepEditor")))
				openJscriptStepEditor(project);
			if ((editorType != null) && (editorType.equals("XMLTransactionStepEditor")))
				openXMLTransactionStepEditor(project);
			if ((editorType != null) && (editorType.equals("XMLSequenceStepEditor")))
				openXMLSequenceStepEditor(project);

			
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}
	
	private void openJscriptStepEditor(IProject project) {
		Step step = (Step)this.getObject();

		String filename = step.getPath();
		filename = filename.substring(filename.indexOf("/_data/")+7);
		filename = filename.replace('/', '.') + " " + step.getName();
		IFile file = project.getFile("/_data/" + filename);
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new JscriptStepEditorInput(file,step),
										"com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStepEditor");
			}
			catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the step editor '" + step.getName() + "'");
			} 
		}
	}

	public void openXMLTransactionStepEditor(IProject project)
	{
		TransactionStep transactionStep = (TransactionStep)this.getObject();
		
		IFile	file = project.getFile("_private/"+transactionStep.getName()+".xml");
		
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new XMLTransactionStepEditorInput(file,transactionStep),
										"com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionStepEditor");
			}
			catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the step editor '" + transactionStep.getName() + "'");
			} 
		}
	}
	
	public void openXMLSequenceStepEditor(IProject project)
	{
		SequenceStep sequenceStep = (SequenceStep)this.getObject();
		
		IFile	file = project.getFile("_private/"+sequenceStep.getName()+".xml");
		
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new XMLSequenceStepEditorInput(file,sequenceStep),
										"com.twinsoft.convertigo.eclipse.editors.xml.XMLSequenceStepEditor");
			}
			catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the step editor '" + sequenceStep.getName() + "'");
			} 
		}
	}

	@Override
	public boolean testAttribute(Object target, String name, String value) {
		if (name.equals("isEnable")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(isEnabled()));
		}
		if (name.equals("isTransactionStep")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(getObject() instanceof TransactionStep));
		}
		if (name.equals("isSequenceStep")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(getObject() instanceof SequenceStep));
		}
		if (name.equals("isThenElseStep")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(getObject() instanceof IThenElseContainer));
		}
		if (name.equals("workOnSource")) {
			Boolean bool = Boolean.valueOf(value);
			return bool.equals(Boolean.valueOf(getObject() instanceof IStepSourceContainer));
		}
		if (name.equals("canChangeTo")) {
			Boolean bool = Boolean.valueOf(value);
			return 	bool.equals(Boolean.valueOf(getObject() instanceof IfStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof IfThenElseStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof IsInStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof IsInThenElseStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof IfExistStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof IfExistThenElseStep)) || 
					bool.equals(Boolean.valueOf(getObject() instanceof IfFileExistStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof IfFileExistThenElseStep));
		}
		return super.testAttribute(target, name, value);
	}
}
