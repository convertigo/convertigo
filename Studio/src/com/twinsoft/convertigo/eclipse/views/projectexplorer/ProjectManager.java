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

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

import com.twinsoft.convertigo.beans.core.DatabaseObject;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;

public class ProjectManager {
    
	/**
     * The current project.
     */
    public ProjectTreeObject currentProjectTreeObject = null;
    public Project currentProject = null;
    
    public String currentProjectName = null;
    public String previousProjectName = null;

    /**
     * Indicates if the current project has been modified.
     */
    protected boolean bModified = false;
    
    public void setCurrentProject(ProjectTreeObject projectTreeObject) {
    	DatabaseObject databaseObject = (DatabaseObject)projectTreeObject.getObject();
    	if ((databaseObject != null) && (databaseObject instanceof Project)) {
    		if (currentProject != null)
    			previousProjectName = currentProjectName;
    		
    		currentProject = (Project)databaseObject;
    		currentProjectName = currentProject.getName();
    		currentProjectTreeObject = projectTreeObject;
    	}
    }

    private ProjectExplorerView projectExplorerView = null;
    
	public ProjectExplorerView getProjectExplorerView() {
		if (projectExplorerView == null) {
			try {
				IViewPart viewPart =  PlatformUI
											.getWorkbench()
											.getActiveWorkbenchWindow()
											.getActivePage()
											.findView("com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView");
				if (viewPart != null)
					projectExplorerView = (ProjectExplorerView)viewPart;
			}
			catch (Exception e) {;}
		}
		
		return projectExplorerView;
	}

	public void setProjectExplorerView(ProjectExplorerView projectExplorerView) {
		this.projectExplorerView = projectExplorerView;
	}
	    
	public int getNumberOfObjects(String projectName) {
		return 100;
	}
}
