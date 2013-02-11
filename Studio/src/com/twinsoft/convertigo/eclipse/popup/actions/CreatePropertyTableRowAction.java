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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.editors.CompositeEvent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.TreeParent;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.DatabaseObjectTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.FolderTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.IPropertyTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.PropertyTableTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class CreatePropertyTableRowAction extends MyAbstractAction {

	public CreatePropertyTableRowAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		if (treeObject instanceof PropertyTableTreeObject) {
			action.setText(((PropertyTableTreeObject)treeObject).getRowDefaultLabel());
		}
	}
	
	public void run() {
		Display display = Display.getDefault();
		Cursor waitCursor = new Cursor(display, SWT.CURSOR_WAIT);		
		
		Shell shell = getParentShell();
		shell.setCursor(waitCursor);
		
        try {
    		ProjectExplorerView explorerView = getProjectExplorerView();
    		if (explorerView != null) {
    			PropertyTableTreeObject propertyTableTreeObject = (PropertyTableTreeObject)explorerView.getFirstSelectedTreeObject();
    			TreeParent owner = propertyTableTreeObject.getTreeObjectOwner();
    			IPropertyTreeObject pto = propertyTableTreeObject.addNewRow();
    			if (owner instanceof DatabaseObjectTreeObject) {
    				DatabaseObjectTreeObject databaseObjectTreeObject = (DatabaseObjectTreeObject)owner;
    				if (databaseObjectTreeObject.hasChanged()) {
    					TreeParent treeParent = databaseObjectTreeObject.getParent();
    					if (treeParent instanceof FolderTreeObject)
    						treeParent = treeParent.getParent();
//    					explorerView.reloadTreeObject(treeParent);
    					explorerView.objectChanged(new CompositeEvent(treeParent.getObject(),pto.getPath()));
    				}
    			}
    			else
    				explorerView.refreshTreeObject(owner, true);
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to create a new row!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}
	
}
