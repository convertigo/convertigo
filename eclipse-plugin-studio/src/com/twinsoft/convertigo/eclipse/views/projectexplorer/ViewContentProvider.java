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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ObjectsFolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.MigrationManager;

public class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
	private static final Object[] NO_CHILDREN = new Object[0];
	
	private ProjectExplorerView projectExplorerView;
	private TreeParent invisibleRoot;
	
	ViewContentProvider(ProjectExplorerView projectExplorerView) {
		this.projectExplorerView = projectExplorerView;
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}

	public void dispose() {
	}
	
	public Object[] getElements(Object parent) {
		if (parent.equals(projectExplorerView.getViewSite())) {
			getUnloadedProjects();
			if (invisibleRoot != null && "true".equals(ConvertigoPlugin.getProperty(ConvertigoPlugin.PREFERENCE_HIDE_LIB_PROJECTS))) {
				Collection<? extends TreeObject> c = invisibleRoot.getChildren();
				Iterator<? extends TreeObject> i = c.iterator();
				while (i.hasNext()) {
					if (i.next().getName().startsWith("lib_")) {
						i.remove();
					}
				}
				return c.toArray(new TreeObject[c.size()]);
			}
			return getChildren(invisibleRoot);
		}
		return getChildren(parent);
	}
	
	public Object getParent(Object child) {
		if (child instanceof TreeObject) {
			return ((TreeObject)child).getParent();
		}
		return null;
	}
	
	public Object[] getChildren(Object parent) {
		if (parent instanceof TreeParent) {
			Collection<? extends TreeObject> c = ((TreeParent) parent).getChildren();
			return c.toArray(new TreeObject[c.size()]);
		}

		if (parent instanceof IFolder) {
			return getResources((IFolder) parent);
		}

		return new Object[0];
	}
	
	public boolean hasChildren(Object parent) {
		if (parent instanceof TreeParent)
			return ((TreeParent)parent).hasChildren();
		return false;
	}

	void loadProjects() {
		getUnloadedProjects();
	}
	
	void loadProject(String projectName) {
		getUnloadedProject(projectName);
	}
	
	private void getUnloadedProject(String projectName) {
		try {
			if (!Engine.isStarted)
				return;
			
			synchronized (this) {
				if (invisibleRoot == null)
					invisibleRoot = new ObjectsFolderTreeObject(projectExplorerView.viewer, ObjectsFolderTreeObject.FOLDER_TYPE_INVISIBLE_ROOT);
				
				if (MigrationManager.isProjectMigrated(projectName)) {
					ConvertigoPlugin.logDebug("[ViewContentProvider] getUnloadedProject: do load project "+projectName);
					loadProjectRootObject(projectName);
				}
				else {
					ConvertigoPlugin.logDebug("[ViewContentProvider] getUnloadedProject: do not load migrating project "+projectName);
				}
			}
		}
		catch(Exception e) {
			String message = "Error while loading project";
			ConvertigoPlugin.logException(e, message);
		}
	}
	
	void refreshProjects() {
		getUnloadedProjects(true);
	}
	
	private void getUnloadedProjects() {
		getUnloadedProjects(false);
	}
	
	private void getUnloadedProjects(boolean bRefresh) {
		try {
			if (!Engine.isStarted)
				return;
			
			synchronized (this) {
				if (invisibleRoot == null)	{
					invisibleRoot = new ObjectsFolderTreeObject(projectExplorerView.viewer, ObjectsFolderTreeObject.FOLDER_TYPE_INVISIBLE_ROOT);
					bRefresh = true;
				}
				if (bRefresh) {
					for (String projectName: Engine.theApp.databaseObjectsManager.getAllProjectNamesList(false)) {
						if (MigrationManager.isProjectMigrated(projectName)) {
							ConvertigoPlugin.logDebug("[ViewContentProvider] getUnloadedProjects : do load project "+projectName);
							loadProjectRootObject(projectName);
						}
						else {
							ConvertigoPlugin.logDebug("[ViewContentProvider] getUnloadedProjects : do not load migrating project "+projectName);
						}
					}
				}
			}
		}
		catch(Exception e) {
			String message = "Error while initializing the project explorer (view content provider)";
			ConvertigoPlugin.logException(e, message);
		}
	}

	private void loadProjectRootObject(String projectName) throws Exception {
		if (projectName.startsWith("mobilebuilder_tpl_")) {
			ConvertigoPlugin.logDebug("Skip loading of the mobilebuilder template project '" + projectName +"'.");
		} else {
			TreeObject treeObject = getProjectRootObject(projectName);
			if (treeObject == null) {
				UnloadedProjectTreeObject unloadedProjectTreeObject = new UnloadedProjectTreeObject(projectExplorerView.viewer, projectName);
				invisibleRoot.addChild(unloadedProjectTreeObject);
				if (ConvertigoPlugin.getDefault().isProjectOpened(projectName)) {
					projectExplorerView.loadProject(unloadedProjectTreeObject);
				}
			}
		}
	}
	
	public TreeParent getTreeRoot() {
		return invisibleRoot;
	}
	
	void reloadProject(TreeObject projectTreeObject) {
		reloadProject(projectTreeObject, false, null);
	}
	
	private void reloadProject(TreeObject projectTreeObject, boolean isCopy, String originalName) {
		if (!Engine.isStarted)
			return;
		
		synchronized (this) {
			String projectName = projectTreeObject.toString();
	
			if (!MigrationManager.isProjectMigrated(projectName)) {
				String message = "Could not load the project \"" + projectName + "\" while it is still migrating."; 
				ConvertigoPlugin.logError(message, Boolean.TRUE);
			}
			else {
				try {
					if (projectTreeObject instanceof UnloadedProjectTreeObject) {
						ProjectLoadingJob job = new ProjectLoadingJob(projectExplorerView.viewer, (UnloadedProjectTreeObject) projectTreeObject);
						job.setUser(true);
						job.schedule();
					}
					else {
						UnloadedProjectTreeObject treeObject = projectExplorerView.unloadProjectTreeObject((ProjectTreeObject) projectTreeObject);
						if (treeObject != null) {
							ProjectLoadingJob job = new ProjectLoadingJob(projectExplorerView.viewer, treeObject, isCopy, originalName);
							job.setUser(true);
							job.schedule();
						}
					}
				}
				catch(Exception e) {
					String message = "Error while loading project " + projectName;
					ConvertigoPlugin.logException(e, message);
				}
			}
		}
	}

	Project getProject(String projectName) throws EngineException {
		for(TreeObject treeObject : invisibleRoot.getChildren())
			if(treeObject instanceof ProjectTreeObject) {
				Project project = ((ProjectTreeObject) treeObject).getObject();
				if(project.getName().equals(projectName))
					return project;
			}
		throw new EngineException("The project " + projectName + " is not loaded or does not exist.");
	}
	
	public TreeObject getProjectRootObject(String projectName) throws EngineException {
		for (TreeObject treeObject : invisibleRoot.getChildren()) {
			if (treeObject instanceof ProjectTreeObject) {
				Project project = ((ProjectTreeObject) treeObject).getObject();
				if (project.getName().equals(projectName))
					return treeObject;
			}
			if (treeObject instanceof UnloadedProjectTreeObject) {
				String name = treeObject.getName();
				if (name.equals(projectName))
					return treeObject;
			}
		}
		return null;
	}

	private Object[] getResources(IFolder folder) {
		try {
			IResource[] members = folder.members();
			return members;
		} catch(CoreException e) {
			return NO_CHILDREN;
		}
	}
	
	public Collection<ProjectTreeObject> getOpenedProjects() {
		List<? extends TreeObject> children = invisibleRoot.getChildren();
		Collection<ProjectTreeObject> opened = new ArrayList<ProjectTreeObject>(children.size());
		for (TreeObject treeObject : children) {
			if (treeObject instanceof ProjectTreeObject) {
				opened.add((ProjectTreeObject) treeObject);
			}
		}
		return opened;
	}
}
