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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.core.Transaction;
import com.twinsoft.convertigo.beans.steps.AttributeStep;
import com.twinsoft.convertigo.beans.steps.ElementStep;
import com.twinsoft.convertigo.beans.steps.IThenElseContainer;
import com.twinsoft.convertigo.beans.steps.IfExistStep;
import com.twinsoft.convertigo.beans.steps.IfExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.IfFileExistStep;
import com.twinsoft.convertigo.beans.steps.IfFileExistThenElseStep;
import com.twinsoft.convertigo.beans.steps.IfStep;
import com.twinsoft.convertigo.beans.steps.IfThenElseStep;
import com.twinsoft.convertigo.beans.steps.IsInStep;
import com.twinsoft.convertigo.beans.steps.IsInThenElseStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.SimpleSourceStep;
import com.twinsoft.convertigo.beans.steps.SimpleStep;
import com.twinsoft.convertigo.beans.steps.SourceStep;
import com.twinsoft.convertigo.beans.steps.TransactionStep;
import com.twinsoft.convertigo.beans.steps.XMLAttributeStep;
import com.twinsoft.convertigo.beans.steps.XMLConcatStep;
import com.twinsoft.convertigo.beans.steps.XMLElementStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.jscript.JscriptStepEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLSequenceStepEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLTransactionStepEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;

public class StepTreeObject extends DatabaseObjectTreeObject implements INamedSourceSelectorTreeObject, IEditableTreeObject, IOrderableTreeObject {
	
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
	}
	
	@Override
	public NamedSourceSelector getNamedSourceSelector() {
		return new NamedSourceSelector() {
			@Override
			Object thisTreeObject() {
				return StepTreeObject.this;
			}
			
			@Override
			protected List<String> getPropertyNamesForSource(Class<?> c) {
				List<String> list = new ArrayList<String>();
				
				if (getObject() instanceof TransactionStep) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						ConnectorTreeObject.class.isAssignableFrom(c) ||
						TransactionTreeObject.class.isAssignableFrom(c))
					{
							list.add("sourceTransaction");
					}
				}
				if (getObject() instanceof SequenceStep) {
					if (ProjectTreeObject.class.isAssignableFrom(c) ||
						SequenceTreeObject.class.isAssignableFrom(c))
					{
							list.add("sourceSequence");
					}
				}
				
				return list;
			}
			
			@Override
			protected boolean isNamedSource(String propertyName) {
				if (getObject() instanceof TransactionStep) {
					return "sourceTransaction".equals(propertyName);
				}
				if (getObject() instanceof SequenceStep) {
					return "sourceSequence".equals(propertyName);
				}
				return false;
			}
			
			@Override
			public boolean isSelectable(String propertyName, Object nsObject) {
				if (getObject() instanceof TransactionStep) {
					if ("sourceTransaction".equals(propertyName)) {
						return nsObject instanceof Transaction;
					}
				}
				if (getObject() instanceof SequenceStep) {
					if ("sourceSequence".equals(propertyName)) {
						return nsObject instanceof Sequence;
					}
				}
				return false;
			}

			@Override
			protected void handleSourceCleared(String propertyName) {
				// nothing to do
			}

			@Override
			protected void handleSourceRenamed(String propertyName, String oldName, String newName) {
				if (isNamedSource(propertyName)) {
					boolean hasBeenRenamed = false;
					
					String pValue = (String) getPropertyValue(propertyName);
					if (pValue != null && pValue.startsWith(oldName)) {
						String _pValue = newName + pValue.substring(oldName.length());
						if (!pValue.equals(_pValue)) {
							if (getObject() instanceof TransactionStep) {
								if ("sourceTransaction".equals(propertyName)) {
									((TransactionStep)getObject()).setSourceTransaction(_pValue);
									hasBeenRenamed = true;
								}
							}
							if (getObject() instanceof SequenceStep) {
								if ("sourceSequence".equals(propertyName)) {
									((SequenceStep)getObject()).setSourceSequence(_pValue);
									hasBeenRenamed = true;
								}
							}
						}
					}
			
					if (hasBeenRenamed) {
						hasBeenModified(true);
						viewer.refresh();
						
						getDescriptors();// refresh editors (e.g labels in combobox)
					}
				}
			}
		};
	}
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
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
		Step step = (Step) this.getObject();


		IFile file = project.getFile("/_private/" + step.getQName() + " " + step.getName());
		
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
			return bool == getObject().workOnSource();
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
					bool.equals(Boolean.valueOf(getObject() instanceof IfFileExistThenElseStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof XMLConcatStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof XMLElementStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof XMLAttributeStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof SourceStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof SimpleSourceStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof ElementStep)) ||
					bool.equals(Boolean.valueOf(getObject() instanceof AttributeStep));
		}
		return super.testAttribute(target, name, value);
	}
}
