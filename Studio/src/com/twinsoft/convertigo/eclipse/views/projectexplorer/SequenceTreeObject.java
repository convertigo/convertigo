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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.beans.core.Step;
import com.twinsoft.convertigo.beans.steps.BlockStep;
import com.twinsoft.convertigo.beans.steps.BranchStep;
import com.twinsoft.convertigo.beans.steps.ElseStep;
import com.twinsoft.convertigo.beans.steps.SequenceStep;
import com.twinsoft.convertigo.beans.steps.ThenStep;
import com.twinsoft.convertigo.beans.steps.XMLComplexStep;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLSequenceEditorInput;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class SequenceTreeObject extends DatabaseObjectTreeObject implements IEditableTreeObject {

	public SequenceTreeObject(Viewer viewer, Sequence object) {
		this(viewer, object, false);
	}
	
	public SequenceTreeObject(Viewer viewer, Sequence object, boolean inherited) {
		super(viewer, object, inherited);
		
		updateLoadedProjects();
	}

	@Override
	public Sequence getObject(){
		return (Sequence) super.getObject();
	}
	
	@Override
	protected void remove() {
		super.remove();
		getObject().loadedProjects.clear();
	}
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
			DatabaseObject databaseObject = (DatabaseObject)databaseObjectTreeObject.getObject();
			if (!databaseObject.equals(getObject())) {
				if ((databaseObject instanceof Project) || (!databaseObject.getProject().getName().equals(getObject().getProject().getName()))) {
					updateLoadedProjects();
				}
			}
		}
	}
	
	@Override
	public void treeObjectRemoved(TreeObjectEvent treeObjectEvent) {
		super.treeObjectRemoved(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (!getParents().contains(treeObject)) {
			if (treeObject instanceof ProjectTreeObject) {
				updateLoadedProjects();
			}
			else if (treeObject instanceof UnloadedProjectTreeObject) {
				updateLoadedProjects();
			}
		}
	}
	
	@Override
	public void treeObjectPropertyChanged(TreeObjectEvent treeObjectEvent) {
		super.treeObjectPropertyChanged(treeObjectEvent);

		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
			String propertyName = (String)treeObjectEvent.propertyName;
			propertyName = ((propertyName == null) ? "":propertyName);
			
			if (!databaseObject.equals(getObject())) {
				if ((databaseObject instanceof Project) || (databaseObject.getParent() != null)) {
					updateLoadedProjects();
				}
			}
			
			if ("name".equals(propertyName)) {
				stepNameChanged(treeObjectEvent);
			} 
			else if ("sourceSequence".equals(propertyName)) {
				if (databaseObject instanceof SequenceStep) {
					stepSourceNameChanged(databaseObject);
				}
			}
		}
	}
	
	private void stepNameChanged (TreeObjectEvent treeObjectEvent) {
		
		DatabaseObjectTreeObject treeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
		DatabaseObject databaseObject = (DatabaseObject)treeObject.getObject();
		
		Object oldValue = treeObjectEvent.oldValue;
		Object newValue = treeObjectEvent.newValue;
		
		if (databaseObject instanceof Sequence) {
			try {
				List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
				ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
				
				for (String projectName : projectNames) {
					Project project = null;
					TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
							.getContentProvider()).getProjectRootObject(projectName);
					if (projectTreeObject instanceof UnloadedProjectTreeObject) {
						project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
					} else {
						project = projectExplorerView.getProject(projectName);
					}
					
					List<Sequence> sequences = project.getSequencesList();
					for (Sequence sequence : sequences) {
						List<Step> steps = sequence.getAllSteps();
						for (Step step : steps) {
							nameChanged(step, projectExplorerView, oldValue, newValue);
						}
					}
				}
			} catch (EngineException e) {
						ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
			}
		}
	}
	
	private void nameChanged (Step step, ProjectExplorerView projectExplorerView, Object oldValue, Object newValue) {
		try {
			if (step instanceof SequenceStep) {
				SequenceStep sequenceStep = (SequenceStep) step;
				String sequenceStepName = sequenceStep.getName();
				Sequence seq = getObject();
				if (sequenceStepName.equals("Call_" + seq.getProject().getName() + "_" + oldValue)) {
					sequenceStep.setName("Call_" + seq.getProject().getName() + "_" + newValue);
					projectExplorerView.refreshTree();
				}
			} else if (isStepContainer(step)) {
				List<Step> steps = getStepList(step);
				for (Step s : steps) {
					nameChanged(s, projectExplorerView, oldValue, newValue);
				}
			}
		} catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}
	}
	
	private boolean isStepContainer(Step step) {
		return (step instanceof BlockStep || step instanceof BranchStep || step instanceof ThenStep || step instanceof ElseStep || step instanceof XMLComplexStep);
	}
	
	private List<Step> getStepList (Step step) {
		List<Step> steps = null;
		
		if (step instanceof BlockStep) {
			steps = ((BlockStep) step).getAllSteps();
		} else if (step instanceof BranchStep) {
			steps = ((BranchStep) step).getAllSteps();
		} else if (step instanceof ThenStep) {
			steps = ((ThenStep) step).getAllSteps();
		} else if (step instanceof ElseStep) {
			steps = ((ElseStep) step).getAllSteps();
		} else if (step instanceof XMLComplexStep) {
			steps = ((XMLComplexStep) step).getAllSteps();
		}
		
		return steps;
	}
	
	private void stepSourceNameChanged (DatabaseObject databaseObject) {
		
		SequenceStep sequenceStep = (SequenceStep) databaseObject;
		String sourceSequence = sequenceStep.getSequenceName();
		String sourceProject = sequenceStep.getProjectName();
		try {
			List<String> projectNames = Engine.theApp.databaseObjectsManager.getAllProjectNamesList();
			ProjectExplorerView projectExplorerView = ConvertigoPlugin.getDefault().getProjectExplorerView();
			
			for (String projectName : projectNames) {
				Project project = null;
				TreeObject projectTreeObject = ((ViewContentProvider) projectExplorerView.viewer
						.getContentProvider()).getProjectRootObject(projectName);
				if (projectTreeObject instanceof UnloadedProjectTreeObject) {
					project = Engine.theApp.databaseObjectsManager.getProjectByName(projectName);
				} else {
					project = projectExplorerView.getProject(projectName);
				}
		
				List<Sequence> sequences = project.getSequencesList();
				for (Sequence sequence : sequences) {
					
					if (sequenceStep.getName().equals("Call_" + sequence.getProject().getName() + "_" + sequence.getName())) {
						sequenceStep.setName("Call_" + sourceProject + "_" + sourceSequence);
						projectExplorerView.refreshTree();
					}
				}
			}
		}catch (EngineException e) {
			ConvertigoPlugin.logException(e, "Error while analyzing the projects hierarchy", true);
		}
	
	}

	private void updateLoadedProjects() {
		TreeParent invisibleRoot = ((ViewContentProvider)((TreeViewer)viewer).getContentProvider()).getTreeRoot();
		Sequence sequence = getObject();
		
		List<String> list = new ArrayList<String>();
		Enumeration<String> names = sequence.getLoadedProjectNames();
		while (names.hasMoreElements()) {
			list.add(names.nextElement());
		}
		
		for (TreeObject treeObject : invisibleRoot.getChildren()) {
			if (treeObject instanceof ProjectTreeObject) {
				Project project = ((ProjectTreeObject)treeObject).getObject();
				sequence.setLoadedProject(project);
			}
			else if (treeObject instanceof UnloadedProjectTreeObject) {
				sequence.removeLoaded(((UnloadedProjectTreeObject)treeObject).getName());
			}
			
			if (list.contains(treeObject.getName()))
				list.remove(treeObject.getName());
		}
		
		for (String projectName: list) {
			sequence.removeLoaded(projectName);
		}
	}
	
	public void launchEditor() {
		// Retrieve the project name
		String projectName = getObject().getProject().getName();
		try {
			// Refresh project resource
			ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);
			
			// Open editor
			openSequenceEditor();

		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}

	public void launchEditor(String editorType) {
		// Retrieve the project name
		String projectName = getObject().getProject().getName();
		try {
			// Refresh project resource
			IProject project = ConvertigoPlugin.getDefault().getProjectPluginResource(projectName);

			// Open editor
			if ((editorType == null) || (editorType.equals("XMLSequenceEditor")))
				openXMLSequenceEditor(project);

			
		} catch (CoreException e) {
			ConvertigoPlugin.logException(e, "Unable to open project named '" + projectName + "'!");
		}
	}
	
	public void openXMLSequenceEditor(IProject project) {
		Sequence sequence = getObject();
		
		IFile	file = project.getFile("_private/"+sequence.getName()+".xml");
		
		
		IWorkbenchPage activePage = PlatformUI
										.getWorkbench()
										.getActiveWorkbenchWindow()
										.getActivePage();
		if (activePage != null) {
			try {
				activePage.openEditor(new XMLSequenceEditorInput(file,sequence),
										"com.twinsoft.convertigo.eclipse.editors.xml.XMLSequenceEditor");
			}
			catch(PartInitException e) {
				ConvertigoPlugin.logException(e, "Error while loading the step editor '" + sequence.getName() + "'");
			} 
		}
	}
	
	public void openSequenceEditor() {
		Sequence sequence = getObject();
		synchronized (sequence) {
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (activePage != null) {
				IEditorPart editorPart = getSequenceEditor(activePage, sequence);
				
				if (editorPart != null) {
					activePage.activate(editorPart);
				}
				else {
					try {
						editorPart = activePage.openEditor(new SequenceEditorInput(sequence),
										"com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditor");
					} catch (PartInitException e) {
						ConvertigoPlugin.logException(e,
								"Error while loading the sequence editor '"
										+ sequence.getName() + "'");
					}
				}
			}
		}
	}
	
	private IEditorPart getSequenceEditor(IWorkbenchPage activePage, Sequence sequence) {
		IEditorPart editorPart = null;
		if (activePage != null) {
			if (sequence != null) {
				IEditorReference[] editorRefs = activePage.getEditorReferences();
				for (int i=0;i<editorRefs.length;i++) {
					IEditorReference editorRef = (IEditorReference)editorRefs[i];
					try {
						IEditorInput editorInput = editorRef.getEditorInput();
						if ((editorInput != null) && (editorInput instanceof SequenceEditorInput)) {
							if (((SequenceEditorInput)editorInput).sequence.equals(sequence)) {
								editorPart = editorRef.getEditor(false);
								break;
							}
						}
					} catch(PartInitException e) {
						//ConvertigoPlugin.logException(e, "Error while retrieving the sequence editor '" + editorRef.getName() + "'");
					}
				}
			}
		}
		return editorPart;
	}
}