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

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.dialogs.MultipleDeletionDialog;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObjectEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.UnloadedProjectTreeObject;
import com.twinsoft.convertigo.engine.Engine;;

public class UnloadedProjectDeleteAction extends MyAbstractAction {
	
	public UnloadedProjectDeleteAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
		MultipleDeletionDialog dialog;

        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
    			if (treeObjects.length == 1) {
    				dialog = new MultipleDeletionDialog(shell, "Project Deletion", false);
    			}
    			else {
    				dialog = new MultipleDeletionDialog(shell, "Project Deletion", true);
    			}
    			
    			if ((treeObjects != null)) {
					for (TreeObject treeObject :treeObjects) {
						if (treeObject instanceof UnloadedProjectTreeObject) {
							String projectName = ((UnloadedProjectTreeObject)treeObject).getName();
							if (dialog.shouldBeDeleted("Do you really want to delete the project \"" + projectName + "\" and all its sub-objects?")) {
			    				// Deleted project will be backup, car will be deleted to avoid its deployment at engine restart
			    				//Engine.theApp.databaseObjectsManager.deleteProject(projectName);
				        		Engine.theApp.databaseObjectsManager.deleteProjectAndCar(projectName);
			    				ConvertigoPlugin.getDefault().deleteProjectPluginResource(projectName);
			    				explorerView.removeProjectTreeObject(treeObject);
			    				explorerView.fireTreeObjectRemoved(new TreeObjectEvent(treeObject));
							}
						}
					}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to delete the project!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
