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

package com.twinsoft.convertigo.eclipse.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.ProjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class ProjectExplorerSaveAllAction extends MyAbstractAction implements IViewActionDelegate {

	public ProjectExplorerSaveAllAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
		// setting log levels to WARNING for studio and Engine
//    	int engineLogLevel = Engine.log.logLevel;
//		Engine.log.logLevel = Log.LOGLEVEL_EXCEPTION;
//		int studioLogLevel = ConvertigoPlugin.getLogLevel();
//		ConvertigoPlugin.setLogLevel(Log.LOGLEVEL_EXCEPTION);
				
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject treeObject = explorerView.getFirstSelectedTreeObject();
    			
        		while (treeObject != null && !(treeObject instanceof DatabaseObjectTreeObject)) {
        			treeObject = treeObject.getParent();
        		}
        		
        		DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject) treeObject;
    				
    			if (databaseObjectTreeObject != null) {
    				DatabaseObjectTreeObject parentObjectTreeObject = databaseObjectTreeObject.getParentDatabaseObjectTreeObject();
    				ProjectTreeObject projectTreeObject = databaseObjectTreeObject.getProjectTreeObject();
    				projectTreeObject.save(false);

    				explorerView.refreshTree();

    				explorerView.setFocus();

    				if (parentObjectTreeObject != null) {
    					explorerView.setSelectedTreeObject(parentObjectTreeObject);
    				}
    				explorerView.setSelectedTreeObject(databaseObjectTreeObject);
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to save the project!");
        }
        finally {
        	// setting back log levels
//        	Engine.log.logLevel = engineLogLevel;
//    		ConvertigoPlugin.setLogLevel(studioLogLevel);
    		
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
	public void init(IViewPart view) {
	}

}
