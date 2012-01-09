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
import com.twinsoft.convertigo.eclipse.views.projectexplorer.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeObject;

public class DatabaseObjectSaveAction extends MyAbstractAction {

	public DatabaseObjectSaveAction() {
		super();
	}

	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);

        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			TreeObject[] treeObjects = explorerView.getSelectedTreeObjects();
    			if (treeObjects != null){
    				for (int i =0; i < treeObjects.length; i++) {
    					DatabaseObjectTreeObject databaseObjectTreeObject = null;
    					TreeObject treeObject = treeObjects[i];

    					// normal case
            			if (treeObject instanceof DatabaseObjectTreeObject) {
            				databaseObjectTreeObject = (DatabaseObjectTreeObject)treeObject;
            			}
            			// case of selected object is not a bean (e.g PropertyTableRowTreeObject)
            			else {
            				while (!(treeObject instanceof DatabaseObjectTreeObject)) {
            					treeObject = treeObject.getParent();
            					if (treeObject == null) break;
            				}
            				databaseObjectTreeObject = (DatabaseObjectTreeObject)treeObject;
            			}
    					
            			if (databaseObjectTreeObject != null){
            				databaseObjectTreeObject.save(false);
        					explorerView.refreshTreeObject(databaseObjectTreeObject,true);
    					}
    				}
    			}
    			explorerView.refreshTree();
    			explorerView.setSelectedTreeObjects(treeObjects);
    		}
		}
		catch (Throwable e) {
			ConvertigoPlugin.logException(e, "Unable to save object");
		}
		finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
        
	}

}
