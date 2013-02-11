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

import java.util.HashSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableColumnTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableRowTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class DeletePropertyTableColumnAction extends MyAbstractAction {

	public DeletePropertyTableColumnAction() {
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
    			if (treeObjects != null) {
    				HashSet<TreeParent> treeParentToReload = new HashSet<TreeParent>();
    				HashSet<TreeParent> treeParentToRefresh = new HashSet<TreeParent>();
    				
    				int len = treeObjects.length;
    				for (int i = 0 ; i < len ; i++) {
    					try {
	    	    			PropertyTableColumnTreeObject propertyTableColumnTreeObject = (PropertyTableColumnTreeObject)treeObjects[i];
	    	    			if (propertyTableColumnTreeObject != null) {
	    	    				MessageBox messageBox = new MessageBox(shell,SWT.YES | SWT.NO | SWT.CANCEL | SWT.ICON_QUESTION | SWT.APPLICATION_MODAL);
	    						String message = java.text.MessageFormat.format("Do you really want to delete the object \"{0}\" and all its sub-objects?", new Object[] {propertyTableColumnTreeObject.getName()});
	    			        	messageBox.setMessage(message);
	    			        	if (messageBox.open() == SWT.YES) {
	    		        			PropertyTableRowTreeObject propertyTableRowTreeObject = (PropertyTableRowTreeObject)propertyTableColumnTreeObject.getParent();
	    		        			TreeParent owner = propertyTableRowTreeObject.getParent().getParent();
	    		        			propertyTableRowTreeObject.removeColumn(propertyTableColumnTreeObject);
	    		        			if (owner instanceof DatabaseObjectTreeObject) {
	    		        				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)owner;
	    		        				if (databaseObjectTreeObject.hasChanged()) {
	    		        					TreeParent treeParent = databaseObjectTreeObject.getParent();
	    		        					if (treeParent instanceof FolderTreeObject)
	    		        						treeParent = treeParent.getParent();
	    			    					treeParentToReload.add(treeParent);
	    			    				}
	    			    			}
	    			    			else
	    			    				treeParentToRefresh.add(owner);
	    			        	}
	    	    			}
    					}
    					catch (ClassCastException e) {}
    				}
    				
    				for (TreeParent treeParent: treeParentToReload) {
    					explorerView.reloadTreeObject(treeParent);
    				}
    				for (TreeParent owner: treeParentToRefresh) {
    					explorerView.refreshTreeObject(owner, true);
    				}
    			}
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to delete column!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
}
