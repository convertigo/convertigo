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

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;
import com.twinsoft.convertigo.engine.Engine;

public class DeclareGlobalSymbolsFromThisProjectAction extends MyAbstractAction {
	
	public DeclareGlobalSymbolsFromThisProjectAction() {
		super();
	}

	@Override
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			Object databaseObject = treeObject.getObject();
				
    			//For each database object from the current project we find the undefined global symbols
    			if ((databaseObject != null) && (databaseObject instanceof Project)) {
    				Project project = (Project)treeObject.getObject();
    				Engine.theApp.databaseObjectsManager.symbolsCreateUndefined(project.getName());
    				
    				//Reload the project
    				explorerView.reloadProject(treeObject);
    				
    				ConvertigoPlugin.infoMessageBox("The global symbols file has been successfully updated!"); 
    			}
    		}
        	
        }
        catch (Exception e) {
        	ConvertigoPlugin.logException(e, "Unable to declare global symbols from the selected project!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
