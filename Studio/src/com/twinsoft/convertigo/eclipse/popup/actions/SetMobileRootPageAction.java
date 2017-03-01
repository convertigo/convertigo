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
 * $URL: $
 * $Author: $
 * $Revision: $
 * $Date: $
 */

package com.twinsoft.convertigo.eclipse.popup.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.twinsoft.convertigo.beans.mobile.components.ApplicationComponent;
import com.twinsoft.convertigo.beans.mobile.components.PageComponent;
import com.twinsoft.convertigo.eclipse.ConvertigoPlugin;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.ProjectExplorerView;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.MobilePageComponentTreeObject;
import com.twinsoft.convertigo.eclipse.views.projectexplorer.model.TreeObject;

public class SetMobileRootPageAction extends MyAbstractAction {

	public SetMobileRootPageAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		super.selectionChanged(action, selection);
		IStructuredSelection structuredSelection = (IStructuredSelection) selection;
		TreeObject treeObject = (TreeObject) structuredSelection.getFirstElement();
		if (treeObject instanceof MobilePageComponentTreeObject) {
			PageComponent page = ((MobilePageComponentTreeObject)treeObject).getObject();
			action.setChecked(page.isRoot);
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
    			MobilePageComponentTreeObject pageTreeObject = (MobilePageComponentTreeObject)explorerView.getFirstSelectedTreeObject();
    			PageComponent page = (PageComponent)pageTreeObject.getObject();

    			ApplicationComponent application = (ApplicationComponent) page.getParent();
                
                MobilePageComponentTreeObject rootPageTreeObject = null;
                PageComponent rootPage = application.getRootPage();
                if (rootPage != null) {
                	rootPageTreeObject = (MobilePageComponentTreeObject)explorerView.findTreeObjectByUserObject(rootPage);
                }
                
                application.setRootPage(page);
    			page.getProject().getMobileBuilder().appChanged();

                if (rootPageTreeObject != null) {
                	rootPageTreeObject.isDefault = false;
                	rootPageTreeObject.hasBeenModified(true);
                }
                pageTreeObject.isDefault = true;
                pageTreeObject.hasBeenModified(true);
                
                // Updating the tree
    			explorerView.refreshTreeObject(pageTreeObject.getParentDatabaseObjectTreeObject());
    		}
        }
        catch (Throwable e) {
        	ConvertigoPlugin.logException(e, "Unable to set page to root one!");
        }
        finally {
			shell.setCursor(null);
			waitCursor.dispose();
        }
	}

}
