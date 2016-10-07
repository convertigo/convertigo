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

import java.util.Set;

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
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.sequence.SequenceEditorInput;
import com.twinsoft.convertigo.eclipse.editors.xml.XMLSequenceEditorInput;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ViewContentProvider;

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
		synchronized (getObject().loadedProjects) {
			getObject().loadedProjects.clear();
		}
	}
	
	@Override
	public void treeObjectAdded(TreeObjectEvent treeObjectEvent) {
		super.treeObjectAdded(treeObjectEvent);
		
		TreeObject treeObject = (TreeObject)treeObjectEvent.getSource();
		if (treeObject instanceof DatabaseObjectTreeObject) {
			DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)treeObjectEvent.getSource();
			DatabaseObject databaseObject = (DatabaseObject)databaseObjectTreeObject.getObject();
			if (!databaseObject.equals(getObject())) {
				try {
					if ((databaseObject instanceof Project) || (!databaseObject.getProject().getName().equals(getObject().getProject().getName()))) {
						updateLoadedProjects();
					}
				} catch (Exception e) {}
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
		}
	}
	
	private void updateLoadedProjects() {
		TreeParent invisibleRoot = ((ViewContentProvider)((TreeViewer)viewer).getContentProvider()).getTreeRoot();
		Sequence sequence = getObject();
		
		Set<String> loadedProject = sequence.getLoadedProjectNames();
		
		for (TreeObject treeObject : invisibleRoot.getChildren()) {
			if (treeObject instanceof ProjectTreeObject) {
				Project project = ((ProjectTreeObject)treeObject).getObject();
				sequence.setLoadedProject(project);
			}
			else if (treeObject instanceof UnloadedProjectTreeObject) {
				sequence.removeLoaded(((UnloadedProjectTreeObject)treeObject).getName());
			}
			
			if (loadedProject.contains(treeObject.getName())) {
				loadedProject.remove(treeObject.getName());
			}
		}
		
		for (String projectName: loadedProject) {
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